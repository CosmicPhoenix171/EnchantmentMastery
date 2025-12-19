package com.enchantmentmastery.network;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.menu.MasteryEnchanterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Packet sent from client to server to request applying an enchantment.
 */
public class ApplyEnchantmentPacket {
    private final ResourceLocation enchantmentId;
    private final int targetLevel;

    public ApplyEnchantmentPacket(ResourceLocation enchantmentId, int targetLevel) {
        this.enchantmentId = enchantmentId;
        this.targetLevel = targetLevel;
    }

    public static void encode(ApplyEnchantmentPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.enchantmentId);
        buf.writeVarInt(packet.targetLevel);
    }

    public static ApplyEnchantmentPacket decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        int level = buf.readVarInt();
        return new ApplyEnchantmentPacket(id, level);
    }

    public static void handle(ApplyEnchantmentPacket packet, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            if (player.containerMenu instanceof MasteryEnchanterMenu menu) {
                menu.selectEnchantment(packet.enchantmentId, packet.targetLevel);
                menu.tryApplyEnchantment();
            } else {
                EnchantmentMastery.LOGGER.warn(
                        "Player {} sent ApplyEnchantmentPacket without MasteryEnchanterMenu open",
                        player.getName().getString()
                );
            }
        }
        ctx.setPacketHandled(true);
    }

    public ResourceLocation getEnchantmentId() {
        return enchantmentId;
    }

    public int getTargetLevel() {
        return targetLevel;
    }
}
