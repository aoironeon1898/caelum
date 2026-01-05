package com.aoironeon1898.caelum.common.content.logistics.entities.modules.item;

public enum PipeTier {
    // コンストラクタ引数: (MaxBufferSlots, MinInterval, MaxAmount, FluidCapacity)
    TIER_1(16, 10, 16, 16000),
    TIER_2(64, 8, 64, 128000);

    private final int bufferSlots;      // 内部バッファのスロット数
    private final int minInterval;      // 最速実行間隔 (tick)
    private final int maxAmount;        // 1回の最大輸送数
    private final int fluidCapacity;    // (将来用) 液体容量 mB

    PipeTier(int bufferSlots, int minInterval, int maxAmount, int fluidCapacity) {
        this.bufferSlots = bufferSlots;
        this.minInterval = minInterval;
        this.maxAmount = maxAmount;
        this.fluidCapacity = fluidCapacity;
    }

    public int getBufferSlots() { return bufferSlots; }
    public int getMinInterval() { return minInterval; }
    public int getMaxAmount() { return maxAmount; }
    public int getFluidCapacity() { return fluidCapacity; }
}