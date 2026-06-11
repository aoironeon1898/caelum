package com.aoironeon1898.caelum.common.content.machines.menus;

import com.aoironeon1898.caelum.common.content.machines.tile.CombustionCellBlockEntity;
import com.aoironeon1898.caelum.common.registries.ModBlocks;
import com.aoironeon1898.caelum.common.registries.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class CombustionCellMenu extends BaseMachineMenu {
    public final CombustionCellBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // クライアント側
    public CombustionCellMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // サーバー側
    public CombustionCellMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.COMBUSTION_CELL_MENU.get(), id);
        this.blockEntity = (CombustionCellBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // 燃料スロット（中央やや上）
        this.addSlot(new SlotItemHandler(this.blockEntity.getFuelInventory(), 0, 80, 35));

        addDataSlots(data);
    }

    public boolean isBurning() { return data.get(2) > 0; }

    @Override public int getEnergy() { return data.get(0); }
    @Override public int getMaxEnergy() { return data.get(1); }
    @Override public int getProgress() { return data.get(2); }      // 残り燃焼tick（炎ゲージ）
    @Override public int getMaxProgress() { return data.get(3); }   // 燃料の総燃焼tick

    // プレイヤー36スロット(0-35) + 燃料1スロット(36)
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < 36) {
            // プレイヤー→燃料スロット
            if (!moveItemStackTo(sourceStack, 36, 37, false)) return ItemStack.EMPTY;
        } else if (index < 37) {
            // 燃料スロット→プレイヤー
            if (!moveItemStackTo(sourceStack, 0, 36, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();
        sourceSlot.onTake(player, sourceStack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.COMBUSTION_CELL.get());
    }
}
