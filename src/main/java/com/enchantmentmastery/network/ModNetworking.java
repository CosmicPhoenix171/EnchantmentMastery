package com.enchantmentmastery.network;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.capability.MasteryCapability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Network packet registration and utility methods for Forge.
 */
public class ModNetworking {
    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(EnchantmentMastery.MOD_ID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(ApplyEnchantmentPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ApplyEnchantmentPacket::decode)
                .encoder(ApplyEnchantmentPacket::encode)
                .consumerMainThread(ApplyEnchantmentPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncMasteryDataPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncMasteryDataPacket::decode)
                .encoder(SyncMasteryDataPacket::encode)
                .consumerMainThread(SyncMasteryDataPacket::handle)
                .add();

        EnchantmentMastery.LOGGER.info("Network packets registered");
    }

    /**
     * Syncs a player's mastery data to their client.
     */
    public static void syncMasteryData(ServerPlayer player) {
        player.getCapability(MasteryCapability.PLAYER_MASTERY).ifPresent(data -> {
            SyncMasteryDataPacket packet = new SyncMasteryDataPacket(data);
            CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
        });
    }
}
