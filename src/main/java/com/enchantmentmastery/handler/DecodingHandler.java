package com.enchantmentmastery.handler;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.capability.MasteryDataHelper;
import com.enchantmentmastery.network.ModNetworking;
import com.enchantmentmastery.util.DecodingUtil;
import com.enchantmentmastery.util.ProgressionMath;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;
import java.util.UUID;

/**
 * Handles the decoding of enchantment names as players spend XP.
 */
public class DecodingHandler {

    public static boolean tryUnlockLetter(ServerPlayer player, ResourceLocation enchantId, int levelsSpent) {
        int[] currentUnlocked = MasteryDataHelper.getUnlockedLetterIndices(player, enchantId);

        Registry<Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> holderOpt = registry.getHolder(
                ResourceKey.create(Registries.ENCHANTMENT, enchantId)
        );

        if (holderOpt.isEmpty()) {
            return false;
        }

        String enchantName = holderOpt.get().value().description().getString();
        int totalLetters = DecodingUtil.countLetters(enchantName);

        if (currentUnlocked.length >= totalLetters) {
            return false;
        }

        int unlockCost = ProgressionMath.decodeCostLevels(currentUnlocked.length);

        if (levelsSpent >= unlockCost) {
            long seed = generateSeed(player.getUUID(), enchantId);

            int nextIndex = DecodingUtil.selectNextLetterToUnlock(enchantName, currentUnlocked, seed);
            if (nextIndex >= 0) {
                MasteryDataHelper.addUnlockedLetterIndex(player, enchantId, nextIndex);

                char letter = getLetterAtIndex(enchantName, nextIndex);
                player.displayClientMessage(
                        Component.translatable("enchantmentmastery.decode.letter_unlocked",
                                String.valueOf(letter).toUpperCase()),
                        true
                );

                ModNetworking.syncMasteryData(player);

                EnchantmentMastery.LOGGER.debug("Player {} unlocked letter '{}' for {}",
                        player.getName().getString(), letter, enchantId);

                return true;
            }
        }

        return false;
    }

    public static void processLevelsSpent(ServerPlayer player, ResourceLocation enchantId, int levelsSpent) {
        int remaining = levelsSpent;
        int unlocked = 0;

        while (remaining > 0 && unlocked < 3) {
            int[] currentUnlocked = MasteryDataHelper.getUnlockedLetterIndices(player, enchantId);
            int unlockCost = ProgressionMath.decodeCostLevels(currentUnlocked.length);

            if (remaining >= unlockCost) {
                if (tryUnlockLetter(player, enchantId, remaining)) {
                    remaining -= unlockCost;
                    unlocked++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private static long generateSeed(UUID playerUuid, ResourceLocation enchantId) {
        return playerUuid.getMostSignificantBits() ^
                playerUuid.getLeastSignificantBits() ^
                enchantId.hashCode();
    }

    private static char getLetterAtIndex(String text, int letterIndex) {
        int currentIndex = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                if (currentIndex == letterIndex) {
                    return c;
                }
                currentIndex++;
            }
        }
        return '?';
    }
}
