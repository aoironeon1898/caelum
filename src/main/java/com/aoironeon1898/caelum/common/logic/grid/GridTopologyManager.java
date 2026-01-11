package com.aoironeon1898.caelum.common.logic.grid;

import com.aoironeon1898.caelum.common.content.logistics.blocks.CompositePipeBlock;
import com.aoironeon1898.caelum.common.content.logistics.blocks.EnumPipeMode;
import com.aoironeon1898.caelum.common.content.logistics.tile.CompositePipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GridTopologyManager {
    private static final Set<BlockPos> DIRTY_POSITIONS = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, GridGraph> GRAPH_CACHE = new HashMap<>();

    public static void markDirty(ServerLevel level, BlockPos pos) { DIRTY_POSITIONS.add(pos); }
    public static GridGraph getGraph(ServerLevel level, BlockPos pos) { return GRAPH_CACHE.get(pos); }

    public static void tick(ServerLevel level) {
        if (DIRTY_POSITIONS.isEmpty()) return;
        Set<BlockPos> processQueue = new HashSet<>(DIRTY_POSITIONS);
        DIRTY_POSITIONS.clear();
        for (BlockPos pos : processQueue) rebuildGraphAt(level, pos);
    }

    private static void rebuildGraphAt(ServerLevel level, BlockPos startNode) {
        if (!(level.getBlockEntity(startNode) instanceof CompositePipeBlockEntity)) return;

        GridGraph oldGraph = GRAPH_CACHE.get(startNode);
        if (oldGraph != null) oldGraph.invalidate();

        GridGraph newGraph = new GridGraph();
        Queue<BlockPos> openSet = new LinkedList<>();
        Set<BlockPos> closedSet = new HashSet<>();

        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            BlockPos current = openSet.poll();
            if (!closedSet.add(current)) continue;

            if (level.getBlockEntity(current) instanceof CompositePipeBlockEntity) {
                newGraph.addNode(current);
                GRAPH_CACHE.put(current, newGraph);

                BlockState currentState = level.getBlockState(current);

                for (Direction dir : Direction.values()) {
                    EnumPipeMode myMode = EnumPipeMode.NONE;
                    if (currentState.getBlock() instanceof CompositePipeBlock) {
                        EnumProperty<EnumPipeMode> modeProp = CompositePipeBlock.PROPERTY_BY_DIRECTION.get(dir);
                        if (currentState.hasProperty(modeProp)) myMode = currentState.getValue(modeProp);
                    }

                    if (myMode == EnumPipeMode.NONE || myMode == EnumPipeMode.DISABLED) continue;

                    BlockPos neighborPos = current.relative(dir);
                    BlockState neighborState = level.getBlockState(neighborPos);

                    if (neighborState.getBlock() instanceof CompositePipeBlock) {
                        EnumProperty<EnumPipeMode> neighborProp = CompositePipeBlock.PROPERTY_BY_DIRECTION.get(dir.getOpposite());
                        EnumPipeMode neighborMode = EnumPipeMode.NONE;
                        if (neighborState.hasProperty(neighborProp)) neighborMode = neighborState.getValue(neighborProp);

                        if (neighborMode != EnumPipeMode.NONE && neighborMode != EnumPipeMode.DISABLED) {
                            if (!closedSet.contains(neighborPos)) openSet.add(neighborPos);
                        }
                    } else {
                        if (myMode == EnumPipeMode.REQUEST) {
                            var be = level.getBlockEntity(neighborPos);
                            if (be != null) {
                                be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(handler -> newGraph.addEndpoint(handler));
                            }
                        }
                    }
                }
            }
        }
    }
}