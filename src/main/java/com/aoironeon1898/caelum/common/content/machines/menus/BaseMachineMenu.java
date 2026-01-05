package com.aoironeon1898.caelum.common.content.machines.menus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMachineMenu extends AbstractContainerMenu {

    protected BaseMachineMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    public abstract int getEnergy();
    public abstract int getMaxEnergy();
    public abstract int getProgress();
    public abstract int getMaxProgress();

    // ↓これは共通処理なのでそのまま
    protected void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    protected void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}