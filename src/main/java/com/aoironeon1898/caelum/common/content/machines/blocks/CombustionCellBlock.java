package com.aoironeon1898.caelum.common.content.machines.blocks;

import com.aoironeon1898.caelum.common.content.machines.tile.CombustionCellBlockEntity;
import com.aoironeon1898.caelum.common.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Combustion Cell — Phase 0 発電機ブロック。
 * - 右クリック（燃料持ち）：燃料投入
 * - 右クリック（空手）：状態をチャットに表示
 * - 上面からhopperで自動燃料挿入可
 */
public class CombustionCellBlock extends BaseEntityBlock {

    public CombustionCellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CombustionCellBlockEntity cell)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && cell.tryInsertFuel(held)) {
            return InteractionResult.CONSUME;
        }

        // ステータス表示
        String status = String.format("§7[Combustion Cell] §fEnergy: §e%d§7/§e%d §8| §f%s",
                cell.getEnergyStored(), cell.getEnergyCapacity(),
                cell.isBurning() ? "§a燃焼中" : "§c停止");
        player.displayClientMessage(Component.literal(status), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CombustionCellBlockEntity cell) cell.drops();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CombustionCellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.COMBUSTION_CELL_BE.get(),
                (l, p, s, be) -> CombustionCellBlockEntity.tick(l, p, s, be));
    }
}
