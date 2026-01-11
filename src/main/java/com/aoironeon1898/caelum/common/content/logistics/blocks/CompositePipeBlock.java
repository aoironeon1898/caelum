package com.aoironeon1898.caelum.common.content.logistics.blocks;

import com.aoironeon1898.caelum.common.content.logistics.menu.PipeConfigMenu;
import com.aoironeon1898.caelum.common.content.logistics.tile.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.logic.grid.GridTopologyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings("deprecation")
public class CompositePipeBlock extends BaseEntityBlock {

    public static final EnumProperty<EnumPipeMode> NORTH = EnumProperty.create("mode_north", EnumPipeMode.class);
    public static final EnumProperty<EnumPipeMode> EAST = EnumProperty.create("mode_east", EnumPipeMode.class);
    public static final EnumProperty<EnumPipeMode> SOUTH = EnumProperty.create("mode_south", EnumPipeMode.class);
    public static final EnumProperty<EnumPipeMode> WEST = EnumProperty.create("mode_west", EnumPipeMode.class);
    public static final EnumProperty<EnumPipeMode> UP = EnumProperty.create("mode_up", EnumPipeMode.class);
    public static final EnumProperty<EnumPipeMode> DOWN = EnumProperty.create("mode_down", EnumPipeMode.class);

    public static final Map<Direction, EnumProperty<EnumPipeMode>> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE_SHAPE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> SIDE_SHAPES = new EnumMap<>(Direction.class);

    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST, EAST);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.WEST, WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP, UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN, DOWN);
        SIDE_SHAPES.put(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4));
        SIDE_SHAPES.put(Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16));
        SIDE_SHAPES.put(Direction.EAST, Block.box(12, 4, 4, 16, 12, 12));
        SIDE_SHAPES.put(Direction.WEST, Block.box(0, 4, 4, 4, 12, 12));
        SIDE_SHAPES.put(Direction.UP, Block.box(4, 12, 4, 12, 16, 12));
        SIDE_SHAPES.put(Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12));
    }

    public CompositePipeBlock(Properties props) {
        super(props);
        BlockState defaultState = this.stateDefinition.any();
        for (Direction dir : Direction.values()) {
            defaultState = defaultState.setValue(PROPERTY_BY_DIRECTION.get(dir), EnumPipeMode.NONE);
        }
        this.registerDefaultState(defaultState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CompositePipeBlockEntity pipe) {
                Vec3 hitVec = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
                Direction targetDir = getTargetDirection(hitVec, hit.getDirection());

                BlockPos neighborPos = pos.relative(targetDir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);

                if (level.getBlockState(neighborPos).getBlock() instanceof CompositePipeBlock) {
                    return InteractionResult.PASS;
                }

                if (neighborBe != null && neighborBe.getCapability(ForgeCapabilities.ITEM_HANDLER, targetDir.getOpposite()).isPresent()) {
                    NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider((id, inv, p) -> new PipeConfigMenu(id, inv, pipe), net.minecraft.network.chat.Component.literal("Pipe Config")), pos);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private Direction getTargetDirection(Vec3 hitVec, Direction face) {
        double x = hitVec.x, y = hitVec.y, z = hitVec.z;
        if (x < 0.25) return Direction.WEST;
        if (x > 0.75) return Direction.EAST;
        if (y < 0.25) return Direction.DOWN;
        if (y > 0.75) return Direction.UP;
        if (z < 0.25) return Direction.NORTH;
        if (z > 0.75) return Direction.SOUTH;
        return face;
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos currentPos, @NotNull BlockPos neighborPos) {
        BlockEntity be = level.getBlockEntity(currentPos);
        if (be instanceof CompositePipeBlockEntity pipe) {
            pipe.updateBlockState();
            return level.getBlockState(currentPos);
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState();
        for (Direction dir : Direction.values()) {
            if (canConnectTo(level, pos.relative(dir), dir)) {
                state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), EnumPipeMode.NORMAL);
            } else {
                state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), EnumPipeMode.NONE);
            }
        }
        return state;
    }

    private boolean canConnectTo(BlockGetter level, BlockPos neighborPos, Direction direction) {
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof CompositePipeBlock) return true;
        BlockEntity be = level.getBlockEntity(neighborPos);
        return be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).isPresent();
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dir)) != EnumPipeMode.NONE) {
                shape = Shapes.or(shape, SIDE_SHAPES.get(dir));
            }
        }
        return shape;
    }

    @Nullable @Override public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) { return new CompositePipeBlockEntity(pos, state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, com.aoironeon1898.caelum.common.registries.ModBlockEntities.COMPOSITE_PIPE.get(), CompositePipeBlockEntity::tick);
    }
    @Override public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock()) && level instanceof ServerLevel sl) GridTopologyManager.markDirty(sl, pos);
        super.onPlace(state, level, pos, oldState, isMoving);
    }
    @Override public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CompositePipeBlockEntity pipe) {
                pipe.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> { for(int i=0; i<h.getSlots(); i++) net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), h.getStackInSlot(i)); });
            }
            if (level instanceof ServerLevel sl) GridTopologyManager.markDirty(sl, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.MODEL; }
}