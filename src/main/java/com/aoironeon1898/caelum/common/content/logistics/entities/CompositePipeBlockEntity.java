package com.aoironeon1898.caelum.common.content.logistics.entities;

import com.aoironeon1898.caelum.common.content.logistics.menus.CompositePipeMenu;
import com.aoironeon1898.caelum.common.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CompositePipeBlockEntity extends BlockEntity implements MenuProvider {

    public final Map<Direction, List<PipeRule>> rules = new EnumMap<>(Direction.class);
    public enum IOMode { INPUT, OUTPUT }

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> itemHandler);

    public CompositePipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPOSITE_PIPE.get(), pos, state);
        for (Direction dir : Direction.values()) rules.put(dir, new ArrayList<>());
    }

    // =================================================================
    //  Tick処理
    // =================================================================
    public static void tick(Level level, BlockPos pos, BlockState state, CompositePipeBlockEntity entity) {
        if (level.isClientSide) return;

        // 1. 吸い出し処理 (Extract)
        // パイプが空のとき、[OUTPUT] 設定のルールを使って吸い出す
        if (entity.itemHandler.getStackInSlot(0).isEmpty()) {
            for (Direction dir : Direction.values()) {
                List<PipeRule> list = entity.rules.get(dir);
                if (list == null || list.isEmpty()) continue;

                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null) continue;

                for (PipeRule rule : list) {
                    if (rule.cooldown > 0) {
                        rule.cooldown--;
                        continue;
                    }

                    // ★変更点: INPUT ではなく OUTPUT (橙) で吸い出す
                    if (rule.mode == IOMode.OUTPUT) {
                        if (attemptItemExtract(level, pos, dir, neighbor, rule, entity)) {
                            rule.cooldown = rule.tick;
                        }
                    }
                }
            }
        }
        // 2. 分配処理 (Distribute)
        // パイプがアイテムを持っていたら、[INPUT] 設定の場所を探して送る
        else {
            distributeToNetwork(level, pos, entity);
        }
    }

    // --- 吸い出しロジック ---
    private static boolean attemptItemExtract(Level level, BlockPos pos, Direction dir, BlockEntity neighbor, PipeRule rule, CompositePipeBlockEntity pipe) {
        LazyOptional<IItemHandler> cap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return false;
        IItemHandler handler = cap.orElseThrow(RuntimeException::new);

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack sim = handler.extractItem(i, rule.amount, true);
            if (sim.isEmpty()) continue;

            if (isMatch(sim, rule)) {
                ItemStack extracted = handler.extractItem(i, rule.amount, false);
                if (!extracted.isEmpty()) {
                    pipe.itemHandler.insertItem(0, extracted, false);
                    return true;
                }
            }
        }
        return false;
    }

    // --- ネットワーク探索 ---
    private static void distributeToNetwork(Level level, BlockPos startPos, CompositePipeBlockEntity sourcePipe) {
        ItemStack stackToSend = sourcePipe.itemHandler.getStackInSlot(0);
        if (stackToSend.isEmpty()) return;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockEntity be = level.getBlockEntity(currentPos);

            if (!(be instanceof CompositePipeBlockEntity currentPipe)) continue;

            // ターゲット探索
            for (Direction dir : Direction.values()) {
                List<PipeRule> rules = currentPipe.rules.get(dir);
                if (rules == null || rules.isEmpty()) continue;

                BlockEntity targetInv = level.getBlockEntity(currentPos.relative(dir));
                if (targetInv == null) continue;

                for (PipeRule rule : rules) {
                    // ★変更点: OUTPUT ではなく INPUT (青) に向かって吐き出す
                    // 「INPUT設定 = パイプからインベントリへ入れる」
                    if (rule.mode == IOMode.INPUT && rule.cooldown <= 0 && isMatch(stackToSend, rule)) {

                        LazyOptional<IItemHandler> cap = targetInv.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
                        if (cap.isPresent()) {
                            IItemHandler handler = cap.orElseThrow(RuntimeException::new);

                            ItemStack simStack = stackToSend.copy();
                            simStack.setCount(Math.min(stackToSend.getCount(), rule.amount));

                            ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, simStack, false);
                            int sentCount = simStack.getCount() - remaining.getCount();

                            if (sentCount > 0) {
                                // 送信成功
                                sourcePipe.itemHandler.getStackInSlot(0).shrink(sentCount);
                                rule.cooldown = rule.tick;

                                if (sourcePipe.itemHandler.getStackInSlot(0).isEmpty()) return;
                                stackToSend = sourcePipe.itemHandler.getStackInSlot(0);
                            }
                        }
                    }
                }
            }

            // 探索範囲の拡大
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(dir);
                if (!visited.contains(neighborPos)) {
                    BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                    if (neighborBe instanceof CompositePipeBlockEntity) {
                        visited.add(neighborPos);
                        queue.add(neighborPos);
                    }
                }
            }
        }
    }

    private static boolean isMatch(ItemStack stack, PipeRule rule) {
        // バリアブロックなら全許可
        if (rule.filterStack.getItem() == Items.BARRIER) return true;

        if (rule.tagName != null) {
            TagKey<net.minecraft.world.item.Item> key = ItemTags.create(new ResourceLocation(rule.tagName.replace("#", "")));
            return stack.is(key);
        }
        return ItemStack.isSameItemSameTags(stack, rule.filterStack);
    }

    // 省略部分はそのまま
    private static boolean attemptFluidExtract(Level level, BlockPos pos, Direction dir, BlockEntity neighbor, PipeRule rule) { return false; }
    @Override public Component getDisplayName() { return Component.translatable("block.caelum.composite_pipe"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) { return new CompositePipeMenu(id, playerInventory, this); }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerCap.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemHandlerCap.invalidate(); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag rulesTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            List<PipeRule> list = rules.get(dir);
            if (!list.isEmpty()) {
                ListTag dirListTag = new ListTag();
                for (PipeRule rule : list) dirListTag.add(rule.serializeNBT());
                rulesTag.put(dir.getName(), dirListTag);
            }
        }
        tag.put("Rules", rulesTag);
        tag.put("Inventory", itemHandler.serializeNBT());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        for (List<PipeRule> list : rules.values()) list.clear();
        if (tag.contains("Rules")) {
            CompoundTag rulesTag = tag.getCompound("Rules");
            for (Direction dir : Direction.values()) {
                if (rulesTag.contains(dir.getName(), Tag.TAG_LIST)) {
                    ListTag dirListTag = rulesTag.getList(dir.getName(), Tag.TAG_COMPOUND);
                    for (int i = 0; i < dirListTag.size(); i++) {
                        rules.get(dir).add(PipeRule.deserializeNBT(dirListTag.getCompound(i)));
                    }
                }
            }
        }
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(tag.getCompound("Inventory"));
    }
    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    public static class PipeRule {
        public int sortIndex; public int tick; public int amount; public boolean isFluid; public ItemStack filterStack; public String tagName; public IOMode mode; public int cooldown = 0;
        public PipeRule(int sortIndex, int tick, int amount, boolean isFluid, ItemStack filterStack, String tagName, IOMode mode) {
            this.sortIndex = sortIndex; this.tick = tick; this.amount = amount; this.isFluid = isFluid; this.filterStack = filterStack; this.tagName = tagName; this.mode = mode != null ? mode : IOMode.INPUT; this.cooldown = tick;
        }
        public CompoundTag serializeNBT() { CompoundTag tag = new CompoundTag(); tag.putInt("Index", sortIndex); tag.putInt("Tick", tick); tag.putInt("Amount", amount); tag.putBoolean("IsFluid", isFluid); CompoundTag itemTag = new CompoundTag(); filterStack.save(itemTag); tag.put("FilterItem", itemTag); if (tagName != null) tag.putString("TagName", tagName); tag.putInt("Mode", mode.ordinal()); return tag; }
        public static PipeRule deserializeNBT(CompoundTag tag) { int sortIndex = tag.getInt("Index"); int tick = tag.getInt("Tick"); int amount = tag.getInt("Amount"); boolean isFluid = tag.getBoolean("IsFluid"); ItemStack filterStack = ItemStack.of(tag.getCompound("FilterItem")); String tagName = null; if (tag.contains("TagName")) tagName = tag.getString("TagName"); IOMode mode = IOMode.values()[tag.getInt("Mode")]; return new PipeRule(sortIndex, tick, amount, isFluid, filterStack, tagName, mode); }
    }
}