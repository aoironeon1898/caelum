package com.aoironeon1898.caelum.common.content.logistics.blocks;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum ConnectionMode implements StringRepresentable {
    NONE("none"),       // ★追加: 切断（モデルなし・接続なし）
    NORMAL("normal"),   // 通常（筒・搬入）
    EXTRACT("extract"), // 搬出（オレンジ）
    INSERT("insert");   // 搬入（青・枠あり）

    private final String name;

    ConnectionMode(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}