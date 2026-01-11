package com.aoironeon1898.caelum.common.network;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.network.packet.PacketUpdatePipeConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Caelum.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // パケットの登録
        net.messageBuilder(PacketUpdatePipeConfig.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketUpdatePipeConfig::new)
                .encoder(PacketUpdatePipeConfig::encode)
                .consumerMainThread(PacketUpdatePipeConfig::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), message);
    }

    public static <MSG> void sendToPlayer(MSG message, net.minecraft.server.level.ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}