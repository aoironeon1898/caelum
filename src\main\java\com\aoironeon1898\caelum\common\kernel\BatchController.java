package com.aoironeon1898.caelum.common.kernel;

import com.aoironeon1898.caelum.common.content.logistics.blocks.EnumPipeMode;
import com.aoironeon1898.caelum.common.content.logistics.data.SlotMappingRule;
import com.aoironeon1898.caelum.common.content.logistics.tile.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.logic.grid.GridGraph;
import com.aoironeon1898.caelum.common.logic.grid.GridTopologyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BatchController {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> DIMENSIONAL_PIPES = new ConcurrentHashMap<>();

    // 転送レート（Tier制への布石）。4は遅すぎたため16に引き上げ（64個スタックを4tick=0.2秒で移送）
    private static final int TRANSFER_RATE = 16;
    // 処理が空振りしたときのクールダウン時間 (tick)。短くして搬入直後の反応遅延を抑える
    private static final int SLEEP_TICKS = 10; // 0.5秒

    public static void register(BlockEntity pipe, Level level, BlockPos pos) {
        if (level.isClientSide) return;
        DIMENSIONAL_PIPES.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos);
    }

    public static void unregister(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        Set<BlockPos> pipes = DIMENSIONAL_PIPES.get(level.dimension());
        if (pipes != null) pipes.remove(pos);
    }

    public static void tickLevel(ServerLevel level) {
        Set<BlockPos> pipes = DIMENSIONAL_PIPES.get(level.dimension());
        if (pipes == null || pipes.isEmpty()) return;

        for (BlockPos pos : pipes) {
            if (!level.isLoaded(pos)) continue;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CompositePipeBlockEntity pipe) {
                // ★改善: クールダウン中は処理をスキップ（負荷対策）
                if (pipe.isOnCooldown()) continue;

                boolean didWork = processPipeExtract(level, pipe);

                // 何も仕事をしなかった（吸い出し対象がない、送るアイテムがない）場合、スリープさせる
                if (!didWork) {
                    pipe.setCooldown(SLEEP_TICKS);
                }
            }
        }
    }

    /**
     * @return アイテムの移動が発生したら true
     */
    private static boolean processPipeExtract(ServerLevel level, CompositePipeBlockEntity sourcePipe) {
        boolean transferredAny = false;

        // ネットワークグラフはスロット毎に取得する必要はないので一度だけ取得する
        GridGraph graph = GridTopologyManager.getGraph(level, sourcePipe.getBlockPos());
        if (graph == null) return false;

        // 配送先候補を先に絞り込んでおく（REQUESTモードを持つパイプのみ）
        List<BlockPos> candidateTargets = new ArrayList<>();
        for (BlockPos targetPos : graph.getNodes()) {
            if (targetPos.equals(sourcePipe.getBlockPos())) continue;
            if (!level.isLoaded(targetPos)) continue;
            BlockEntity targetBe = level.getBlockEntity(targetPos);
            if (targetBe instanceof CompositePipeBlockEntity destPipe) {
                boolean hasRequest = false;
                for (Direction d : Direction.values()) {
                    if (destPipe.getMode(d) == EnumPipeMode.REQUEST) { hasRequest = true; break; }
                }
                if (hasRequest) candidateTargets.add(targetPos);
            }
        }

        if (candidateTargets.isEmpty()) return false;

        for (Direction sourceDir : Direction.values()) {
            if (sourcePipe.getMode(sourceDir) != EnumPipeMode.PROVIDE) continue;

            BlockPos sourceInvPos = sourcePipe.getBlockPos().relative(sourceDir);
            BlockEntity sourceBe = level.getBlockEntity(sourceInvPos);
            if (sourceBe == null) continue;

            LazyOptional<IItemHandler> cap = sourceBe.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
            if (!cap.isPresent()) cap = sourceBe.getCapability(ForgeCapabilities.ITEM_HANDLER, sourceDir.getOpposite());
            if (!cap.isPresent()) continue;

            IItemHandler sourceHandler = cap.orElse(null);
            if (sourceHandler == null) continue;

            List<SlotMappingRule> sourceRules = sourcePipe.getRules(sourceDir);
            boolean sourceIsWhitelist = sourcePipe.isWhitelist(sourceDir);

            // インベントリスキャン
            for (int slot = 0; slot < sourceHandler.getSlots(); slot++) {
                ItemStack simStack = sourceHandler.extractItem(slot, TRANSFER_RATE, true);
                if (simStack.isEmpty()) continue;

                if (!isAllowed(simStack, sourceRules, sourceIsWhitelist)) continue;

                // 候補先に順に試す
                for (BlockPos targetPos : candidateTargets) {
                    BlockEntity targetBe = level.getBlockEntity(targetPos);
                    if (!(targetBe instanceof CompositePipeBlockEntity destPipe)) continue;
                    if (tryInsertToDestination(level, destPipe, simStack, sourceHandler, slot)) {
                        transferredAny = true;
                        // 1スタック送れたらこの面の処理は一旦終了（ラウンドロビンの布石）
                        break;
                    }
                }
            }
        }
        return transferredAny;
    }

    private static boolean tryInsertToDestination(ServerLevel level, CompositePipeBlockEntity destPipe, ItemStack stackToSend, IItemHandler sourceHandler, int sourceSlot) {

        for (Direction destDir : Direction.values()) {
            if (destPipe.getMode(destDir) == EnumPipeMode.REQUEST) {

                List<SlotMappingRule> destRules = destPipe.getRules(destDir);
                boolean destIsWhitelist = destPipe.isWhitelist(destDir);

                if (!isAllowed(stackToSend, destRules, destIsWhitelist)) {
                    continue;
                }

                BlockPos destInvPos = destPipe.getBlockPos().relative(destDir);
                BlockEntity destInvBe = level.getBlockEntity(destInvPos);
                if (destInvBe == null) continue;

                // ★改善: 安全なCapability取得（フォールバック実装）
                LazyOptional<IItemHandler> cap = destInvBe.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
                if (!cap.isPresent()) {
                    cap = destInvBe.getCapability(ForgeCapabilities.ITEM_HANDLER, destDir.getOpposite());
                }

                if (cap.isPresent()) {
                    IItemHandler destHandler = cap.orElse(null);

                    ItemStack remaining = ItemHandlerHelper.insertItem(destHandler, stackToSend, true);
                    int amountTransferred = stackToSend.getCount() - remaining.getCount();

                    if (amountTransferred > 0) {
                        ItemStack extracted = sourceHandler.extractItem(sourceSlot, amountTransferred, false);
                        ItemHandlerHelper.insertItem(destHandler, extracted, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isAllowed(ItemStack stack, List<SlotMappingRule> rules, boolean isWhitelist) {
        boolean matchFound = false;

        for (SlotMappingRule rule : rules) {
            if (rule.matches(stack)) {
                matchFound = true;
                break;
            }
        }

        if (isWhitelist) {
            return matchFound;
        } else {
            return !matchFound;
        }
    }
}
