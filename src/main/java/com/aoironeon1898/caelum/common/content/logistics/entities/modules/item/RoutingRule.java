package com.aoironeon1898.caelum.common.content.logistics.entities.modules.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class RoutingRule {
    // --- 保存される設定データ ---
    public int targetSlotIndex = -1;       // -1:自動, 0以上:指定スロット
    public ItemStack filterItem = ItemStack.EMPTY;
    public boolean isTagMode = false;
    public String tagString = "";

    public int limitAmount = 16;           // ユーザー設定: 1回の輸送量
    public int targetInterval = 20;        // ユーザー設定: 実行間隔(tick)

    // --- 実行時の一時データ (保存不要) ---
    public int currentCooldown = 0;        // 0になるまで待機

    public RoutingRule() {
        // デフォルトコンストラクタ
    }

    // NBTへの書き込み
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("TargetSlot", targetSlotIndex);
        if (!filterItem.isEmpty()) {
            tag.put("Filter", filterItem.save(new CompoundTag()));
        }
        tag.putBoolean("IsTagMode", isTagMode);
        tag.putString("TagString", tagString);
        tag.putInt("Limit", limitAmount);
        tag.putInt("Interval", targetInterval);
        return tag;
    }

    // NBTからの読み込み
    public void deserializeNBT(CompoundTag tag) {
        this.targetSlotIndex = tag.getInt("TargetSlot");
        if (tag.contains("Filter")) {
            this.filterItem = ItemStack.of(tag.getCompound("Filter"));
        } else {
            this.filterItem = ItemStack.EMPTY;
        }
        this.isTagMode = tag.getBoolean("IsTagMode");
        this.tagString = tag.getString("TagString");

        // 設定値がない場合(古いデータ等)のデフォルト値
        this.limitAmount = tag.contains("Limit") ? tag.getInt("Limit") : 16;
        this.targetInterval = tag.contains("Interval") ? tag.getInt("Interval") : 20;
    }
}