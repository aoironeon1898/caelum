package com.aoironeon1898.caelum.common.network;

import com.aoironeon1898.caelum.common.network.packets.PacketSyncDuct;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("caelum", "main"), // MODIDに合わせてください
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.registerMessage(id(), PacketSyncDuct.class,
                PacketSyncDuct::encode,
                PacketSyncDuct::decode,
                PacketSyncDuct::handle);
    }
}