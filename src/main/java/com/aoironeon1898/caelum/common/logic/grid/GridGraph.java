package com.aoironeon1898.caelum.common.logic.grid;

import net.minecraft.core.BlockPos;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GridGraph {
    private final Set<BlockPos> nodes = new HashSet<>();
    private final List<IItemHandler> endpoints = new ArrayList<>();
    private boolean valid = true;

    // パイプの座標を追加
    public void addNode(BlockPos pos) {
        nodes.add(pos);
    }

    // 搬入先インベントリを追加
    public void addEndpoint(IItemHandler handler) {
        endpoints.add(handler);
    }

    // ★追加: これが不足していたメソッドです
    public Set<BlockPos> getNodes() {
        return nodes;
    }

    // 搬入先リストの取得
    public List<IItemHandler> getValidEndpoints() {
        return endpoints;
    }

    // グラフの無効化（再構築用）
    public void invalidate() {
        this.valid = false;
        this.nodes.clear();
        this.endpoints.clear();
    }

    public boolean isValid() {
        return valid;
    }
}