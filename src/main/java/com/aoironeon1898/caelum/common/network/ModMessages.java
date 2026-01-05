package com.aoironeon1898.caelum.common.network;

import com.aoironeon1898.caelum.common.network.packets.PacketUpdatePipeRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection; // ★追加
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel INSTANCE;

    // パケットID（0から順番に増やす）
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        // ★修正: 変数名を 'net' から 'channel' に変更して衝突を回避
        SimpleChannel channel = NetworkRegistry.ChannelBuilder
                // ResourceLocationの警告回避: ("modid", "path") ではなく ("modid:path") と書くのが今の流儀です
                .named(new ResourceLocation("caelum:messages"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = channel;

        // パケットの登録
        channel.messageBuilder(PacketUpdatePipeRules.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketUpdatePipeRules::new)
                .encoder(PacketUpdatePipeRules::toBytes)
                .consumerMainThread(PacketUpdatePipeRules::handle)
                .add();
    }
}