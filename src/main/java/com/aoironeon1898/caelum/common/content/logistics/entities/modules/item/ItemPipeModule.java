package com.aoironeon1898.caelum.common.content.logistics.entities.modules.item;

import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.content.logistics.entities.modules.IPipeModule;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.*;

public class ItemPipeModule implements IPipeModule {
    private final CompositePipeBlockEntity parent;

    // --- 内部データ ---
    // 現在のTier (デフォルトはTier 1)
    private PipeTier currentTier = PipeTier.TIER_1;

    // 内部バッファ (Tier 2の最大サイズで確保しておく)
    private final ItemStackHandler internalBuffer = new ItemStackHandler(PipeTier.TIER_2.getBufferSlots()) {
        @Override
        protected void onContentsChanged(int slot) {
            parent.setChanged(); // 中身が変わったら保存フラグを立てる
        }
    };
    private final LazyOptional<IItemHandler> bufferCap = LazyOptional.of(() -> internalBuffer);

    // --- ルール設定 (方向ごと) ---
    // 搬出用 (Output: パイプ -> 隣)
    public final Map<Direction, List<RoutingRule>> deliveryRules = new EnumMap<>(Direction.class);
    // 吸入用 (Input: 隣 -> パイプ)
    public final Map<Direction, List<RoutingRule>> extractionRules = new EnumMap<>(Direction.class);

    // ラウンドロビン用 (最後に送ったルールのインデックスを記憶)
    private final Map<Direction, Integer> roundRobinState = new EnumMap<>(Direction.class);

    public ItemPipeModule(CompositePipeBlockEntity parent) {
        this.parent = parent;
        for (Direction dir : Direction.values()) {
            deliveryRules.put(dir, new ArrayList<>());
            extractionRules.put(dir, new ArrayList<>());
            roundRobinState.put(dir, 0);
        }
    }

    // --- メインロジック (毎tick実行) ---
    @Override
    public void tick() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide) return;

        // 1. 各方向に対して処理を行う
        for (Direction dir : Direction.values()) {
            // A. 吸い出し処理 (Extraction / Input)
            processExtraction(dir);

            // B. 搬出処理 (Delivery / Output)
            processDelivery(dir);
        }
    }

    // --- A. 吸い出しロジック (隣 -> バッファ) ---
    private void processExtraction(Direction dir) {
        List<RoutingRule> rules = extractionRules.get(dir);
        if (rules == null || rules.isEmpty()) return;

        // 隣のインベントリを取得
        BlockEntity neighbor = parent.getLevel().getBlockEntity(parent.getBlockPos().relative(dir));
        if (neighbor == null) return;

        // パイプ自身からは吸わない (無限ループ防止)
        if (neighbor instanceof CompositePipeBlockEntity) return;

        neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(sourceHandler -> {

            for (RoutingRule rule : rules) {
                // 1. クールダウンチェック
                if (rule.currentCooldown > 0) {
                    rule.currentCooldown--;
                    continue;
                }

                // 2. 実行 (強制引き抜き)
                // 実際に移動させる最大数 (Tier上限とルール設定の小さい方)
                int amountLimit = Math.min(rule.limitAmount, currentTier.getMaxAmount());

                // ソースインベントリをスキャン
                for (int slot = 0; slot < sourceHandler.getSlots(); slot++) {
                    ItemStack sourceStack = sourceHandler.getStackInSlot(slot);
                    if (sourceStack.isEmpty()) continue;

                    // フィルタ判定
                    if (!isMatchFilter(sourceStack, rule)) continue;

                    // 引き抜きシミュレーション
                    ItemStack toExtract = sourceHandler.extractItem(slot, amountLimit, true);
                    if (toExtract.isEmpty()) continue;

                    // 内部バッファに入るか試す
                    ItemStack remaining = ItemHandlerHelper.insertItem(internalBuffer, toExtract, true);
                    int canAcceptCount = toExtract.getCount() - remaining.getCount();

                    if (canAcceptCount > 0) {
                        // 確定処理: 本当に移動させる
                        ItemStack extracted = sourceHandler.extractItem(slot, canAcceptCount, false);
                        ItemHandlerHelper.insertItem(internalBuffer, extracted, false);

                        // クールダウンをリセット
                        rule.currentCooldown = Math.max(rule.targetInterval, currentTier.getMinInterval());

                        // 1つのルールにつき1回処理したら、このtickは終了(負荷軽減)
                        return;
                    }
                }
            }
        });
    }

    // --- B. 搬出ロジック (バッファ -> 隣) ---
    private void processDelivery(Direction dir) {
        List<RoutingRule> rules = deliveryRules.get(dir);
        if (rules == null || rules.isEmpty()) return;

        // 隣のインベントリを取得
        BlockEntity neighbor = parent.getLevel().getBlockEntity(parent.getBlockPos().relative(dir));
        if (neighbor == null) return;

        neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(targetHandler -> {

            // バッファが空なら処理しない
            boolean isBufferEmpty = true;
            for(int i=0; i<internalBuffer.getSlots(); i++) {
                if(!internalBuffer.getStackInSlot(i).isEmpty()) {
                    isBufferEmpty = false;
                    break;
                }
            }
            if (isBufferEmpty) return;

            // ラウンドロビン: 前回の続きからスタート
            int startIndex = roundRobinState.getOrDefault(dir, 0);

            for (int i = 0; i < rules.size(); i++) {
                int currentIndex = (startIndex + i) % rules.size();
                RoutingRule rule = rules.get(currentIndex);

                // 1. クールダウンチェック
                if (rule.currentCooldown > 0) {
                    rule.currentCooldown--;
                    continue;
                }

                // 2. 実行 (強制ねじ込み)
                int amountLimit = Math.min(rule.limitAmount, currentTier.getMaxAmount());

                // バッファ内のアイテムを走査
                for (int slot = 0; slot < internalBuffer.getSlots(); slot++) {
                    ItemStack stackInBuffer = internalBuffer.getStackInSlot(slot);
                    if (stackInBuffer.isEmpty()) continue;

                    // フィルタ判定 (搬出側でもフィルタがあれば適用)
                    if (!isMatchFilter(stackInBuffer, rule)) continue;

                    // 移動させるアイテムのコピーを作成
                    ItemStack toSend = stackInBuffer.copy();
                    toSend.setCount(Math.min(toSend.getCount(), amountLimit));

                    ItemStack remaining;

                    // ターゲットスロットの指定がある場合
                    if (rule.targetSlotIndex >= 0) {
                        if (rule.targetSlotIndex < targetHandler.getSlots()) {
                            remaining = targetHandler.insertItem(rule.targetSlotIndex, toSend, false);
                        } else {
                            remaining = toSend; // スロット番号が無効
                        }
                    } else {
                        // 自動挿入
                        remaining = ItemHandlerHelper.insertItem(targetHandler, toSend, false);
                    }

                    int sentCount = toSend.getCount() - remaining.getCount();

                    if (sentCount > 0) {
                        // 成功: バッファから実際に減らす
                        internalBuffer.extractItem(slot, sentCount, false);

                        // クールダウンリセット
                        rule.currentCooldown = Math.max(rule.targetInterval, currentTier.getMinInterval());

                        // ラウンドロビンの位置更新
                        roundRobinState.put(dir, (currentIndex + 1) % rules.size());
                        return;
                    }
                }
            }
        });
    }

    // フィルタ一致判定ヘルパー
    private boolean isMatchFilter(ItemStack stack, RoutingRule rule) {
        if (rule.filterItem.isEmpty()) return true; // フィルタなしなら何でもOK

        if (rule.isTagMode && !rule.tagString.isEmpty()) {
            // タグ一致判定
            return stack.getTags().anyMatch(t -> t.location().toString().equals(rule.tagString));
        } else {
            // アイテム一致判定
            return stack.getItem() == rule.filterItem.getItem();
        }
    }

    // --- NBT保存・読み込み ---
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("Buffer", internalBuffer.serializeNBT());
        nbt.putString("Tier", currentTier.name());

        // ルールの保存
        saveRules(nbt, "DeliveryRules", deliveryRules);
        saveRules(nbt, "ExtractionRules", extractionRules);

        return nbt;
    }

    private void saveRules(CompoundTag root, String key, Map<Direction, List<RoutingRule>> ruleMap) {
        CompoundTag mapTag = new CompoundTag();
        for (Direction dir : ruleMap.keySet()) {
            ListTag list = new ListTag();
            for (RoutingRule rule : ruleMap.get(dir)) {
                list.add(rule.serializeNBT());
            }
            mapTag.put(dir.name(), list);
        }
        root.put(key, mapTag);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("Buffer")) {
            internalBuffer.deserializeNBT(nbt.getCompound("Buffer"));
        }
        if (nbt.contains("Tier")) {
            try {
                currentTier = PipeTier.valueOf(nbt.getString("Tier"));
            } catch (IllegalArgumentException e) {
                currentTier = PipeTier.TIER_1;
            }
        }

        loadRules(nbt, "DeliveryRules", deliveryRules);
        loadRules(nbt, "ExtractionRules", extractionRules);
    }

    private void loadRules(CompoundTag root, String key, Map<Direction, List<RoutingRule>> ruleMap) {
        if (!root.contains(key)) return;
        CompoundTag mapTag = root.getCompound(key);
        for (Direction dir : Direction.values()) {
            ruleMap.get(dir).clear();
            if (mapTag.contains(dir.name())) {
                ListTag list = mapTag.getList(dir.name(), Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    RoutingRule rule = new RoutingRule();
                    rule.deserializeNBT(list.getCompound(i));
                    ruleMap.get(dir).add(rule);
                }
            }
        }
    }

    // --- Capability ---
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        // パイプ同士の接続や、外部からの搬入を受け付けるためにCapabilityを公開する
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return bufferCap.cast();
        }
        return LazyOptional.empty();
    }

    // GUI用判定
    @Override
    public boolean hasSettings() {
        return true;
    }
}