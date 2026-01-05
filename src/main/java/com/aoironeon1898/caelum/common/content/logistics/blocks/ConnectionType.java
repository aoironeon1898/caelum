package com.aoironeon1898.caelum.common.content.logistics.blocks;

import net.minecraft.util.StringRepresentable;

public enum ConnectionType implements StringRepresentable {
    NONE("none"),     // 接続なし
    PIPE("pipe"),     // パイプ同士 (通常)
    INPUT("input"),   // インベントリから搬入 (吸い出し用モデル)
    OUTPUT("output"); // インベントリへ搬出 (押し込み用モデル)

    private final String name;

    ConnectionType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}