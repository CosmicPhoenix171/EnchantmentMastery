package com.enchantmentmastery.network;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.capability.MasteryCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet sent from server to client to sync player mastery data.
 */
public class SyncMasteryDataPacket {
    private final Map<ResourceLocation, Integer> masteryLevels;
    private final Map<ResourceLocation, Integer> masteryXp;
    private final Map<ResourceLocation, int[]> unlockedLetters;
    private final int totalLevelsSpent;

    public SyncMasteryDataPacket(Map<ResourceLocation, Integer> levels, Map<ResourceLocation, Integer> xp,
                                 Map<ResourceLocation, int[]> letters, int totalSpent) {
        this.masteryLevels = levels;
        this.masteryXp = xp;
        this.unlockedLetters = letters;
        this.totalLevelsSpent = totalSpent;
    }

    public SyncMasteryDataPacket(MasteryCapability.IPlayerMasteryData data) {
        this(
                data.getAllMasteryLevels(),
                data.getAllMasteryXp(),
                data.getAllUnlockedLetters(),
                data.getTotalLevelsSpent()
        );
    }

    public static void encode(SyncMasteryDataPacket packet, FriendlyByteBuf buf) {
        // Write mastery levels
        buf.writeVarInt(packet.masteryLevels.size());
        for (var entry : packet.masteryLevels.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        // Write mastery XP
        buf.writeVarInt(packet.masteryXp.size());
        for (var entry : packet.masteryXp.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        // Write unlocked letters
        buf.writeVarInt(packet.unlockedLetters.size());
        for (var entry : packet.unlockedLetters.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            int[] arr = entry.getValue();
            buf.writeVarInt(arr.length);
            for (int val : arr) {
                buf.writeVarInt(val);
            }
        }

        // Write total levels spent
        buf.writeVarInt(packet.totalLevelsSpent);
    }

    public static SyncMasteryDataPacket decode(FriendlyByteBuf buf) {
        // Read mastery levels
        int levelsSize = buf.readVarInt();
        Map<ResourceLocation, Integer> levels = new HashMap<>();
        for (int i = 0; i < levelsSize; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int level = buf.readVarInt();
            levels.put(id, level);
        }

        // Read mastery XP
        int xpSize = buf.readVarInt();
        Map<ResourceLocation, Integer> xp = new HashMap<>();
        for (int i = 0; i < xpSize; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int xpVal = buf.readVarInt();
            xp.put(id, xpVal);
        }

        // Read unlocked letters
        int lettersSize = buf.readVarInt();
        Map<ResourceLocation, int[]> letters = new HashMap<>();
        for (int i = 0; i < lettersSize; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int arrLen = buf.readVarInt();
            int[] arr = new int[arrLen];
            for (int j = 0; j < arrLen; j++) {
                arr[j] = buf.readVarInt();
            }
            letters.put(id, arr);
        }

        // Read total levels spent
        int totalSpent = buf.readVarInt();

        return new SyncMasteryDataPacket(levels, xp, letters, totalSpent);
    }

    public static void handle(SyncMasteryDataPacket packet, CustomPayloadEvent.Context ctx) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(MasteryCapability.PLAYER_MASTERY).ifPresent(data -> {
                // Copy received data
                for (var entry : packet.masteryLevels.entrySet()) {
                    data.setMasteryLevel(entry.getKey(), entry.getValue());
                }
                for (var entry : packet.masteryXp.entrySet()) {
                    data.setMasteryXp(entry.getKey(), entry.getValue());
                }
                for (var entry : packet.unlockedLetters.entrySet()) {
                    data.setUnlockedLetterIndices(entry.getKey(), entry.getValue());
                }

                EnchantmentMastery.LOGGER.debug("Synced mastery data from server: {} enchants learned",
                        packet.masteryLevels.size());
            });
        }
        ctx.setPacketHandled(true);
    }
}
