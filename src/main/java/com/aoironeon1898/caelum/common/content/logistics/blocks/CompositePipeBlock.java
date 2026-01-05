package com.aoironeon1898.caelum.common.content.logistics.blocks;

import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.registries.ModBlockEntities; // ★追加
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

// ConnectionTypeの定義が必要ですが、元のコードにあると仮定します
// もしなければ、クラス内か別ファイルで定義してください
import com.aoironeon1898.caelum.common.content.logistics.blocks.ConnectionType;

public class CompositePipeBlock extends Block implements EntityBlock {

    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.create("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.create("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.create("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.create("west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> UP = EnumProperty.create("up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> DOWN = EnumProperty.create("down", ConnectionType.class);

    public CompositePipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, ConnectionType.NONE)
                .setValue(EAST, ConnectionType.NONE)
                .setValue(SOUTH, ConnectionType.NONE)
                .setValue(WEST, ConnectionType.NONE)
                .setValue(UP, ConnectionType.NONE)
                .setValue(DOWN, ConnectionType.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompositePipeBlockEntity(pos, state);
    }

    // =================================================================
    //  ★最重要: これがないと tick が動きません！
    // =================================================================
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // クライアント側では動かさない
        if (level.isClientSide) return null;

        // 正しい BlockEntity タイプかチェックして、tick メソッドを登録
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof CompositePipeBlockEntity pipe) {
                CompositePipeBlockEntity.tick(lvl, pos, st, pipe);
            }
        };
    }

    // --- 右クリック動作 ---
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CompositePipeBlockEntity pipeEntity) {
                // GUIを開く
                NetworkHooks.openScreen((ServerPlayer) player, pipeEntity, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }
    }

    // --- 形状更新ロジック (既存のまま) ---
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return makeConnections(context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return makeConnections(level, currentPos);
    }

    private BlockState makeConnections(BlockGetter level, BlockPos pos) {
        return this.defaultBlockState()
                .setValue(DOWN, getConnectionType(level, pos, Direction.DOWN))
                .setValue(UP, getConnectionType(level, pos, Direction.UP))
                .setValue(NORTH, getConnectionType(level, pos, Direction.NORTH))
                .setValue(EAST, getConnectionType(level, pos, Direction.EAST))
                .setValue(SOUTH, getConnectionType(level, pos, Direction.SOUTH))
                .setValue(WEST, getConnectionType(level, pos, Direction.WEST));
    }

    private ConnectionType getConnectionType(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        BlockEntity neighborBE = level.getBlockEntity(neighborPos);

        if (neighborState.getBlock() instanceof CompositePipeBlock) {
            return ConnectionType.PIPE;
        }
        if (neighborBE != null) {
            boolean hasInventory = neighborBE.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).isPresent();
            if (hasInventory) {
                return ConnectionType.INPUT;
            }
        }
        return ConnectionType.NONE;
    }
}