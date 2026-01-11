package com.aoironeon1898.caelum.common.content.logistics.blocks;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum EnumPipeMode implements StringRepresentable {
    NONE("none"),       // 視覚用: 接続なし（隣がない、または切断中）
    NORMAL("normal"),   // 通常接続（自動接続モード）
    PROVIDE("provide"), // 搬出 (吸い込み)
    REQUEST("request"), // 搬入 (吐き出し)
    DISABLED("disabled"); // 論理用: 手動で切断された状態

    private final String name;

    EnumPipeMode(String name) { this.name = name; }

    @Override
    public @NotNull String getSerializedName() { return this.name; }
}