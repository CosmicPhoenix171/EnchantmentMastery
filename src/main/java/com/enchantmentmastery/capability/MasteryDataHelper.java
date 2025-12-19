package com.enchantmentmastery.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for accessing player mastery data via capabilities.
 */
public class MasteryDataHelper {

    public static MasteryCapability.IPlayerMasteryData getData(Player player) {
        return player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .orElseThrow(() -> new IllegalStateException("Player missing mastery capability"));
    }

    public static int getMasteryLevel(Player player, ResourceLocation enchantId) {
        return player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .map(data -> data.getMasteryLevel(enchantId))
                .orElse(0);
    }

    public static void setMasteryLevel(Player player, ResourceLocation enchantId, int level) {
        player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .ifPresent(data -> data.setMasteryLevel(enchantId, level));
    }

    public static int getMasteryXp(Player player, ResourceLocation enchantId) {
        return player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .map(data -> data.getMasteryXp(enchantId))
                .orElse(0);
    }

    public static void addMasteryXp(Player player, ResourceLocation enchantId, int xp) {
        player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .ifPresent(data -> data.addMasteryXp(enchantId, xp));
    }

    public static boolean hasEnchantmentUnlocked(Player player, ResourceLocation enchantId) {
        return player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .map(data -> data.hasEnchantmentUnlocked(enchantId))
                .orElse(false);
    }

    public static Map<ResourceLocation, Integer> getAllMasteryLevels(Player player) {
        return player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .map(MasteryCapability.IPlayerMasteryData::getAllMasteryLevels)
                .orElse(new HashMap<>());
    }

    public static int getTotalLevelsSpent(Player player) {
        return player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .map(MasteryCapability.IPlayerMasteryData::getTotalLevelsSpent)
                .orElse(0);
    }

    public static void addLevelsSpent(Player player, int levels) {
        player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .ifPresent(data -> data.addLevelsSpent(levels));
    }

    public static int[] getUnlockedLetterIndices(Player player, ResourceLocation enchantId) {
        return player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .map(data -> data.getUnlockedLetterIndices(enchantId))
                .orElse(new int[0]);
    }

    public static void addUnlockedLetterIndex(Player player, ResourceLocation enchantId, int index) {
        player.getCapability(MasteryCapability.PLAYER_MASTERY)
                .ifPresent(data -> data.addUnlockedLetterIndex(enchantId, index));
    }
}
