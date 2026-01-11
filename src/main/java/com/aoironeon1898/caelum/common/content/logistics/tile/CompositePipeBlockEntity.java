package com.aoironeon1898.caelum.common.content.logistics.tile;

import com.aoironeon1898.caelum.common.base.CaelumBlockEntity;
import com.aoironeon1898.caelum.common.base.MachineTier;
import com.aoironeon1898.caelum.common.content.logistics.blocks.CompositePipeBlock;
import com.aoironeon1898.caelum.common.content.logistics.blocks.EnumPipeMode;
import com.aoironeon1898.caelum.common.content.logistics.data.SlotMappingRule;
import com.aoironeon1898.caelum.common.kernel.BatchController;
import com.aoironeon1898.caelum.common.logic.grid.GridTopologyManager;
import com.aoironeon1898.caelum.common.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CompositePipeBlockEntity extends CaelumBlockEntity {

    private final ItemStackHandler internalBuffer = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final LazyOptional<IItemHandler> itemHandlerOptional = LazyOptional.of(() -> internalBuffer);

    private final Map<Direction, List<SlotMappingRule>> mappingRules = new EnumMap<>(Direction.class);
    private final Map<Direction, EnumPipeMode> ioModes = new EnumMap<>(Direction.class);
    private final Map<Direction, Boolean> filterModes = new EnumMap<>(Direction.class);
    private int transferCooldown = 0;

    public CompositePipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPOSITE_PIPE.get(), pos, state);
        for (Direction dir : Direction.values()) {
            // ★重要: 初期値はすべて「NORMAL(接続したい)」にする
            ioModes.put(dir, EnumPipeMode.NORMAL);
            mappingRules.put(dir, new ArrayList<>());
            filterModes.put(dir, true);
        }
    }

    // --- 見た目更新ロジック ---
    public void updateBlockState() {
        if (level == null) return;
        BlockState currentState = level.getBlockState(worldPosition);
        if (!(currentState.getBlock() instanceof CompositePipeBlock)) return;

        BlockState newState = currentState;

        for (Direction dir : Direction.values()) {
            EnumPipeMode logicMode = ioModes.get(dir); // 内部の設定
            EnumPipeMode visualMode = logicMode;       // 実際の見た目

            // ★ロジック: NORMALなら、隣がある時だけつなぐ。DISABLEDなら絶対つながない。
            if (logicMode == EnumPipeMode.NORMAL) {
                if (canConnect(dir)) {
                    visualMode = EnumPipeMode.NORMAL;
                } else {
                    visualMode = EnumPipeMode.NONE;
                }
            } else if (logicMode == EnumPipeMode.DISABLED) {
                visualMode = EnumPipeMode.NONE;
            }

            newState = newState.setValue(CompositePipeBlock.PROPERTY_BY_DIRECTION.get(dir), visualMode);
        }

        if (!newState.equals(currentState)) {
            level.setBlock(worldPosition, newState, 3);
        }
    }

    // 接続判定ヘルパー
    private boolean canConnect(Direction dir) {
        if (level == null) return false;
        BlockPos neighborPos = worldPosition.relative(dir);
        // パイプなら接続
        if (level.getBlockState(neighborPos).getBlock() instanceof CompositePipeBlock) return true;
        // インベントリなら接続
        BlockEntity be = level.getBlockEntity(neighborPos);
        return be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).isPresent();
    }

    // --- Tick ---
    public static void tick(Level level, BlockPos pos, BlockState state, CompositePipeBlockEntity entity) {
        if (entity.transferCooldown > 0) entity.transferCooldown--;
    }

    public boolean isOnCooldown() { return transferCooldown > 0; }
    public void setCooldown(int ticks) { this.transferCooldown = ticks; }

    // --- Getter / Setter ---
    public void updateMode(Direction dir, EnumPipeMode mode) {
        setMode(dir, mode);
    }

    public void setMode(Direction dir, EnumPipeMode mode) {
        this.ioModes.put(dir, mode);
        setChanged();
        if (level != null && !level.isClientSide) {
            updateBlockState(); // 見た目を即座に更新
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public EnumPipeMode getMode(Direction dir) { return ioModes.get(dir); }
    public List<SlotMappingRule> getRules(Direction dir) { return mappingRules.get(dir); }
    public void setRules(Direction dir, List<SlotMappingRule> rules) {
        this.mappingRules.put(dir, rules);
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public boolean isWhitelist(Direction dir) { return filterModes.getOrDefault(dir, true); }
    public void setWhitelist(Direction dir, boolean isWhitelist) {
        this.filterModes.put(dir, isWhitelist);
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public MachineTier getMachineTier() { return MachineTier.TIER_1_ATOMIC; }
    @Override
    protected boolean runLogic(float efficiencyMultiplier) { return false; }

    // --- Save / Load ---
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeSyncData(tag);
        tag.put("Inventory", internalBuffer.serializeNBT());
        tag.putInt("Cooldown", transferCooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readSyncData(tag);
        if (tag.contains("Inventory")) internalBuffer.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("Cooldown")) transferCooldown = tag.getInt("Cooldown");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeSyncData(tag);
        return tag;
    }
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            readSyncData(tag);
            if (level != null && level.isClientSide) updateBlockState();
        }
    }

    private void writeSyncData(CompoundTag tag) {
        CompoundTag modesTag = new CompoundTag();
        CompoundTag filterTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            modesTag.putString(dir.getName(), ioModes.get(dir).getSerializedName());
            filterTag.putBoolean(dir.getName(), filterModes.get(dir));
        }
        tag.put("IOModes", modesTag);
        tag.put("FilterModes", filterTag);

        // Rules save logic omitted for brevity (same as before)
        CompoundTag rulesTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            List<SlotMappingRule> rules = mappingRules.get(dir);
            if (!rules.isEmpty()) {
                ListTag list = new ListTag();
                for (SlotMappingRule rule : rules) list.add(rule.serializeNBT());
                rulesTag.put(dir.getName(), list);
            }
        }
        tag.put("MappingRules", rulesTag);
    }

    private void readSyncData(CompoundTag tag) {
        if (tag.contains("IOModes")) {
            CompoundTag modesTag = tag.getCompound("IOModes");
            for (Direction dir : Direction.values()) {
                if (modesTag.contains(dir.getName())) {
                    String modeName = modesTag.getString(dir.getName());
                    EnumPipeMode mode = EnumPipeMode.NORMAL; // デフォルトNORMAL
                    for (EnumPipeMode m : EnumPipeMode.values()) {
                        if (m.getSerializedName().equals(modeName)) { mode = m; break; }
                    }
                    ioModes.put(dir, mode);
                }
            }
        }
        if (tag.contains("FilterModes")) {
            CompoundTag filterTag = tag.getCompound("FilterModes");
            for (Direction dir : Direction.values()) {
                if (filterTag.contains(dir.getName())) filterModes.put(dir, filterTag.getBoolean(dir.getName()));
            }
        }
        // Rules load logic (same as before)
        if (tag.contains("MappingRules")) {
            CompoundTag rulesTag = tag.getCompound("MappingRules");
            for (Direction dir : Direction.values()) {
                if (rulesTag.contains(dir.getName(), Tag.TAG_LIST)) {
                    ListTag list = rulesTag.getList(dir.getName(), Tag.TAG_COMPOUND);
                    List<SlotMappingRule> rules = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) rules.add(SlotMappingRule.fromNBT(list.getCompound(i)));
                    mappingRules.put(dir, rules);
                } else mappingRules.get(dir).clear();
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            GridTopologyManager.markDirty(serverLevel, worldPosition);
            BatchController.register(this, level, worldPosition);
        }
        if (level != null) updateBlockState();
    }
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            GridTopologyManager.markDirty((ServerLevel) level, worldPosition);
            BatchController.unregister(level, worldPosition);
        }
        super.setRemoved();
        itemHandlerOptional.invalidate();
    }
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerOptional.cast();
        return super.getCapability(cap, side);
    }
}