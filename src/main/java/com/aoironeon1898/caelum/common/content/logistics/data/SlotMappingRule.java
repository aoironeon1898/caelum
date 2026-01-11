package com.aoironeon1898.caelum.common.content.logistics.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class SlotMappingRule {
    private ItemStack filterItem = ItemStack.EMPTY;
    private int targetSlotIndex = -1; // -1 = Auto
    private String tagFilter = "";    // 空ならアイテム指定、入っていればタグ指定

    public SlotMappingRule() {}

    // アイテム指定用コンストラクタ
    public SlotMappingRule(ItemStack item, int slot) {
        this.filterItem = item.copy();
        this.targetSlotIndex = slot;
        this.tagFilter = "";
    }

    // タグ指定用コンストラクタ
    public SlotMappingRule(String tag, int slot) {
        this.filterItem = ItemStack.EMPTY;
        this.targetSlotIndex = slot;
        this.tagFilter = tag;
    }

    public ItemStack getFilterItem() { return filterItem; }
    public int getTargetSlotIndex() { return targetSlotIndex; }
    public String getTagFilter() { return tagFilter; }

    public boolean isTagMode() {
        return !tagFilter.isEmpty();
    }

    // ★判定ロジック: このアイテムはルールに適合するか？
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (isTagMode()) {
            // タグ判定
            try {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(tagFilter));
                return stack.is(tagKey);
            } catch (Exception e) {
                return false; // 無効なタグ文字列
            }
        } else {
            // アイテム判定 (空の場合は常にtrueとして扱うか、呼び出し元で制御)
            return !filterItem.isEmpty() && ItemHandlerHelper.canItemStacksStack(filterItem, stack);
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (!filterItem.isEmpty()) {
            tag.put("Item", filterItem.save(new CompoundTag()));
        }
        tag.putInt("Slot", targetSlotIndex);
        if (!tagFilter.isEmpty()) {
            tag.putString("Tag", tagFilter);
        }
        return tag;
    }

    public static SlotMappingRule fromNBT(CompoundTag tag) {
        SlotMappingRule rule = new SlotMappingRule();
        if (tag.contains("Item")) {
            rule.filterItem = ItemStack.of(tag.getCompound("Item"));
        }
        rule.targetSlotIndex = tag.getInt("Slot");
        if (tag.contains("Tag")) {
            rule.tagFilter = tag.getString("Tag");
        }
        return rule;
    }
}