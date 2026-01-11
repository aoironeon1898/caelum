package com.aoironeon1898.caelum.common.network.packet;

import com.aoironeon1898.caelum.common.content.logistics.blocks.EnumPipeMode;
import com.aoironeon1898.caelum.common.content.logistics.data.SlotMappingRule;
import com.aoironeon1898.caelum.common.content.logistics.tile.CompositePipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketUpdatePipeConfig {
    private final BlockPos pos;
    private final Direction direction;
    private final EnumPipeMode mode;
    private final List<SlotMappingRule> rules;
    private final boolean isWhitelist;

    public PacketUpdatePipeConfig(BlockPos pos, Direction direction, EnumPipeMode mode, List<SlotMappingRule> rules, boolean isWhitelist) {
        this.pos = pos;
        this.direction = direction;
        this.mode = mode;
        this.rules = rules;
        this.isWhitelist = isWhitelist;
    }

    public PacketUpdatePipeConfig(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.direction = buffer.readEnum(Direction.class);
        this.mode = buffer.readEnum(EnumPipeMode.class);
        this.isWhitelist = buffer.readBoolean();

        int size = buffer.readInt();
        this.rules = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            CompoundTag tag = buffer.readNbt();
            this.rules.add(SlotMappingRule.fromNBT(tag));
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(direction);
        buffer.writeEnum(mode);
        buffer.writeBoolean(isWhitelist);

        buffer.writeInt(rules.size());
        for (SlotMappingRule rule : rules) {
            buffer.writeNbt(rule.serializeNBT());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // ブロックエンティティを取得して設定を更新
                if (player.level().getBlockEntity(pos) instanceof CompositePipeBlockEntity pipe) {
                    // setModeなどはCompositePipeBlockEntity側でsendBlockUpdated(見た目の更新通知)を行うように修正済
                    pipe.setMode(direction, mode);
                    pipe.setRules(direction, rules);
                    pipe.setWhitelist(direction, isWhitelist);
                    pipe.setChanged();
                }
            }
        });
        return true;
    }
}