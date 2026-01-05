package com.aoironeon1898.caelum.common.content.machines.menus;

import com.aoironeon1898.caelum.common.content.machines.entities.StellarCrusherBlockEntity;
import com.aoironeon1898.caelum.common.registries.ModBlocks;
import com.aoironeon1898.caelum.common.registries.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class StellarCrusherMenu extends BaseMachineMenu {
    public final StellarCrusherBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // クライアント側
    public StellarCrusherMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // サーバー側
    public StellarCrusherMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.STELLAR_CRUSHER_MENU.get(), pContainerId);
        checkContainerSize(inv, 2);
        this.blockEntity = ((StellarCrusherBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // 入力スロット
            this.addSlot(new SlotItemHandler(handler, 0, 55, 33));
            // 出力スロット
            this.addSlot(new SlotItemHandler(handler, 1, 113, 33) {
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
            });
        });

        addDataSlots(data);
    }

    // ★追加: これがないとエラーになります！
    public boolean isCrafting() {
        // dataの2番目が「現在の進行度(progress)」です。
        // これが 0 より大きければ「稼働中」と判断します。
        return data.get(2) > 0;
    }

    // --- BaseMachineMenu の実装 ---
    @Override public int getEnergy() { return this.data.get(0); }
    @Override public int getMaxEnergy() { return this.data.get(1); }
    @Override public int getProgress() { return this.data.get(2); }
    @Override public int getMaxProgress() { return this.data.get(3); }

    // --- Shiftクリック動作 ---
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < 36) {
            if (!moveItemStackTo(sourceStack, 36, 37, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 38) {
            if (!moveItemStackTo(sourceStack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();

        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.STELLAR_CRUSHER.get());
    }
}