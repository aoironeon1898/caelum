package com.aoironeon1898.caelum.common.network;
import com.aoironeon1898.caelum.common.content.logistics.blocks.EnumPipeMode;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.Map;

public class CaelumModelProperties {
    // パイプの各方向のモードを保持するプロパティキー
    public static final ModelProperty<Map<Direction, EnumPipeMode>> PIPE_MODES = new ModelProperty<>();
}
