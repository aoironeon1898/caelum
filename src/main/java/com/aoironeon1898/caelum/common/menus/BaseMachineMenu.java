package com.aoironeon1898.caelum.common.menus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

// ★抽象クラス (abstract) として定義
public abstract class BaseMachineMenu extends AbstractContainerMenu {

    protected BaseMachineMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    // プレイヤーのインベントリ（下の3列）を追加する共通メソッド
    protected void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    // プレイヤーのホットバー（一番下の1列）を追加する共通メソッド
    protected void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}