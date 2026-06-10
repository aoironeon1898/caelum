package com.aoironeon1898.caelum.common.content.logistics.blocks;

import com.aoironeon1898.caelum.common.content.logistics.tile.EnergyConduitBlockEntity;
import com.aoironeon1898.caelum.common.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Energy Conduit — 沈黙する電力導線。
 * 装飾は最小限。当面は全方向に同じテクスチャの細い棒。
 * Phase 2でCompositePipe同様の方向別接続モデルに進化させる予定。
 */
public class EnergyConduitBlock extends BaseEntityBlock {

    public EnergyConduitBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyConduitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.ENERGY_CONDUIT_BE.get(),
                (l, p, s, be) -> EnergyConduitBlockEntity.tick(l, p, s, be));
    }
}
