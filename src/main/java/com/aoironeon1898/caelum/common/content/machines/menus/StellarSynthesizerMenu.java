package com.aoironeon1898.caelum.common.content.machines.menus;

import com.aoironeon1898.caelum.common.content.machines.entities.StellarSynthesizerBlockEntity;
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

// ★変更1: BaseMachineMenu を継承
public class StellarSynthesizerMenu extends BaseMachineMenu {
    public final StellarSynthesizerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // クライアント側
    public StellarSynthesizerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // サーバー側
    public StellarSynthesizerMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.STELLAR_SYNTHESIZER_MENU.get(), pContainerId);
        checkContainerSize(inv, 3);
        this.blockEntity = ((StellarSynthesizerBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;

        // ★変更2: プレイヤーのインベントリを先に登録（恒星炉と合わせるため）
        // これによりスロット番号 0~35 がプレイヤー、36~38 が機械になります
        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // 機械のスロット登録 (index 36, 37, 38)
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            this.addSlot(new SlotItemHandler(handler, 0, 51, 13)); // Input 1
            this.addSlot(new SlotItemHandler(handler, 1, 51, 53)); // Input 2

            // Output Slot (配置不可設定)
            this.addSlot(new SlotItemHandler(handler, 2, 129, 33) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        });

        addDataSlots(data);
    }

    // --- データ取得用メソッド ---
    // ※あなたのコードに合わせてインデックスを設定しています
    // (0:Energy, 1:MaxEnergy, 2:Progress, 3:MaxProgress)

    public boolean isCrafting() {
        return data.get(2) > 0;
    }

    public int getEnergy() {
        return this.data.get(0);
    }

    public int getMaxEnergy() {
        return this.data.get(1);
    }

    public int getScaledProgress() {
        int progress = this.data.get(2);
        int maxProgress = this.data.get(3);
        int progressArrowSize = 30; // 矢印のピクセルサイズ
        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    // データ取得用のヘルパー（Screenから呼び出すために追加しておくと便利）
    public int getProgress() { return this.data.get(2); }
    public int getMaxProgress() { return this.data.get(3); }


    // --- Shiftクリック動作 (quickMoveStack) ---
    // ★変更3: プレイヤーインベントリを先頭にしたので、ロジックを恒星炉と同じ形に統一
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // 0-35 はプレイヤー領域、36-38 は機械領域
        if (index < 36) {
            // プレイヤーインベントリ -> 機械のスロットへ移動
            // 機械のスロットは 36 から 39 (36, 37, 38) まで
            if (!moveItemStackTo(sourceStack, 36, 39, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 39) {
            // 機械のスロット -> プレイヤーインベントリへ移動
            if (!moveItemStackTo(sourceStack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.STELLAR_SYNTHESIZER.get());
    }
}