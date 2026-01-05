package com.aoironeon1898.caelum.common.network.packets;

import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity.IOMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketUpdatePipeRules {
    private final BlockPos pos;
    private final Direction side;
    private final List<CompositePipeBlockEntity.PipeRule> rules;

    public PacketUpdatePipeRules(BlockPos pos, Direction side, List<CompositePipeBlockEntity.PipeRule> rules) {
        this.pos = pos;
        this.side = side;
        this.rules = rules;
    }

    public PacketUpdatePipeRules(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.side = Direction.from3DDataValue(buf.readInt());

        int size = buf.readInt();
        this.rules = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int index = buf.readInt();
            int tick = buf.readInt();
            int amount = buf.readInt();
            boolean isFluid = buf.readBoolean();
            ItemStack stack = buf.readItem();
            String tagName = buf.readBoolean() ? buf.readUtf() : null;

            // IOModeの読み込み
            IOMode mode = buf.readEnum(IOMode.class);

            this.rules.add(new CompositePipeBlockEntity.PipeRule(index, tick, amount, isFluid, stack, tagName, mode));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(side.get3DDataValue());

        buf.writeInt(rules.size());
        for (CompositePipeBlockEntity.PipeRule rule : rules) {
            buf.writeInt(rule.sortIndex);
            buf.writeInt(rule.tick);
            buf.writeInt(rule.amount);
            buf.writeBoolean(rule.isFluid);
            buf.writeItem(rule.filterStack);

            if (rule.tagName != null) {
                buf.writeBoolean(true);
                buf.writeUtf(rule.tagName);
            } else {
                buf.writeBoolean(false);
            }

            // IOModeの書き込み
            buf.writeEnum(rule.mode);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // ブロックエンティティの取得
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof CompositePipeBlockEntity pipe) {
                    pipe.rules.put(side, rules);
                    pipe.setChanged();
                    player.level().sendBlockUpdated(pos, pipe.getBlockState(), pipe.getBlockState(), 3);
                }
            }
        });
        return true;
    }
}