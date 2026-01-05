package com.aoironeon1898.caelum.common.content.logistics.menus;

import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.registries.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CompositePipeMenu extends AbstractContainerMenu {
    public final CompositePipeBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    // ★追加: フィルター設定用アイテムを置くためのコンテナ (1スロット)
    public final SimpleContainer filterContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    // クライアント側コンストラクタ
    public CompositePipeMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // サーバー側コンストラクタ
    public CompositePipeMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.COMPOSITE_PIPE_MENU.get(), id);
        this.blockEntity = (CompositePipeBlockEntity) entity;
        this.levelAccess = ContainerLevelAccess.create(playerInv.player.level(), blockEntity.getBlockPos());

        // 1. プレイヤーインベントリの配置 (Slot 0 - 35)
        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);

        // 2. ★追加: フィルタースロットの配置 (Slot 36)
        // 座標 (x=8, y=10) は Screen 側のレイアウト定数と合わせます
        this.addSlot(new Slot(this.filterContainer, 0, 31, 24) {
            @Override
            public int getMaxStackSize() {
                return 1; // 1個しか置けないように制限
            }
        });
    }

    // プレイヤーインベントリ (3行)
    // 座標は前回調整した中央寄せの位置を使用
    private void addPlayerInventory(Inventory playerInv) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInv, l + i * 9 + 9, 8 + l * 18, 204 + i * 18));
            }
        }
    }

    // ホットバー (1行)
    private void addPlayerHotbar(Inventory playerInv) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInv, i, 8 + i * 18, 262));
        }
    }

    // ★追加: GUIを閉じた時にアイテムをプレイヤーに返す処理
    // これがないと、アイテムを置いたまま閉じると消滅してしまいます
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.filterContainer);
    }

    // Shiftクリック移動の実装 (フィルター対応版)
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // インデックス 36 は フィルタースロット
            if (index == 36) {
                // フィルター -> プレイヤーインベントリ (0-36) へ移動
                if (!this.moveItemStackTo(itemstack1, 0, 36, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // インデックス 0-35 は プレイヤーインベントリ
            else if (index < 36) {
                // プレイヤー -> フィルタースロット (36) へ移動
                if (!this.moveItemStackTo(itemstack1, 36, 37, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, blockEntity.getBlockState().getBlock());
    }
}