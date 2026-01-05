package com.aoironeon1898.caelum.common.content.logistics.entities.modules;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;

public interface IPipeModule {
    // 毎tick実行される処理
    void tick();

    // データの読み書き
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);

    // 外部から「機能持ってる？」と聞かれた時の対応
    <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side);

    boolean hasSettings();

}