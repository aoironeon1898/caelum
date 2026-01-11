package com.aoironeon1898.caelum.common.content.logistics.menu;

import com.aoironeon1898.caelum.common.content.logistics.tile.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.registries.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PipeConfigMenu extends AbstractContainerMenu {

    // --- インベントリ配置設定 (ここを変更して位置を調整してください) ---
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 205; // インベントリのY座標

    private static final int HOTBAR_X = 8;
    private static final int HOTBAR_Y = 262;       // ホットバーのY座標
    // ---------------------------------------------------------

    public final CompositePipeBlockEntity pipe;
    private final Level level;
    private final ContainerLevelAccess access;

    // クライアント側コンストラクタ
    public PipeConfigMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // サーバー側コンストラクタ
    public PipeConfigMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.PIPE_CONFIG_MENU.get(), id);
        this.pipe = (CompositePipeBlockEntity) entity;
        this.level = inv.player.level();
        this.access = ContainerLevelAccess.create(level, pipe.getBlockPos());

        // プレイヤーインベントリの追加
        addPlayerInventory(inv);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // メインインベントリ (3行)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9,
                        PLAYER_INVENTORY_X + j * 18,
                        PLAYER_INVENTORY_Y + i * 18));
            }
        }

        // ホットバー (1行)
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k,
                    HOTBAR_X + k * 18,
                    HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, pipe.getBlockState().getBlock());
    }
}