package com.aoironeon1898.caelum.common.base;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public enum MachineTier {
    // パイプコードが要求する定数名に修正
    TIER_1_ATOMIC("Type I", "Planetary", "Atomic", 1.0f, 60_000, ChatFormatting.AQUA),
    TIER_2_GENETIC("Type II", "Stellar", "Genetic", 4.0f, 1_000_000, ChatFormatting.GOLD),
    TIER_3_NUCLEON("Type III", "Galactic", "Nucleon", 64.0f, 100_000_000, ChatFormatting.LIGHT_PURPLE);

    private final String tierName;
    private final String civilization;
    private final String decomposition;
    private final float speedMultiplier; // 変数名変更
    private final int energyCapacity;    // 変数名変更
    private final ChatFormatting color;

    MachineTier(String tierName, String civilization, String decomposition, float speed, int capacity, ChatFormatting color) {
        this.tierName = tierName;
        this.civilization = civilization;
        this.decomposition = decomposition;
        this.speedMultiplier = speed;
        this.energyCapacity = capacity;
        this.color = color;
    }

    // パイプコードが要求するメソッド名に修正
    public float getSpeedMultiplier() { return speedMultiplier; }
    public int getEnergyCapacity() { return energyCapacity; }

    // (旧互換用: 必要なら残すが、基本は上を使う)
    public int getCapacity() { return energyCapacity; }

    public String getDecomposition() { return decomposition; }

    public Component getFullDisplayName() {
        return Component.literal(tierName + " - " + civilization)
                .withStyle(Style.EMPTY.withColor(color))
                .append(Component.literal(" [" + decomposition + "]").withStyle(ChatFormatting.GRAY));
    }
}