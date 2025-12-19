package com.enchantmentmastery.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility for decoding enchantment names using Standard Galactic Alphabet.
 * Locked letters display in minecraft:alt font, unlocked letters in normal font.
 */
public final class DecodingUtil {
    private DecodingUtil() {}

    // The Standard Galactic Alphabet font in Minecraft
    public static final ResourceLocation ALT_FONT = ResourceLocation.withDefaultNamespace("alt");

    // Style for locked (galactic) letters
    private static final Style LOCKED_STYLE = Style.EMPTY.withFont(ALT_FONT);
    // Style for unlocked (normal) letters
    private static final Style UNLOCKED_STYLE = Style.EMPTY;

    /**
     * Creates a mixed-font component for an enchantment name based on unlocked letters.
     *
     * @param enchantmentName The full enchantment name (e.g., "Sharpness")
     * @param unlockedIndices Array of character indices that are unlocked (use normal font)
     * @return A Component with mixed styling
     */
    public static Component createDecodedName(String enchantmentName, int[] unlockedIndices) {
        if (enchantmentName == null || enchantmentName.isEmpty()) {
            return Component.empty();
        }

        // Convert indices to a set for O(1) lookup
        Set<Integer> unlocked = new HashSet<>();
        if (unlockedIndices != null) {
            for (int idx : unlockedIndices) {
                unlocked.add(idx);
            }
        }

        MutableComponent result = Component.empty();

        // Track letter index (only counting A-Z letters)
        int letterIndex = 0;

        for (int i = 0; i < enchantmentName.length(); i++) {
            char c = enchantmentName.charAt(i);

            // Check if this is a letter (A-Z, a-z)
            boolean isLetter = Character.isLetter(c);

            if (isLetter) {
                // Determine if this letter is unlocked
                boolean isUnlocked = unlocked.contains(letterIndex);
                Style style = isUnlocked ? UNLOCKED_STYLE : LOCKED_STYLE;

                result = result.append(Component.literal(String.valueOf(c)).withStyle(style));
                letterIndex++;
            } else {
                // Non-letter characters (spaces, punctuation) always use normal style
                result = result.append(Component.literal(String.valueOf(c)).withStyle(UNLOCKED_STYLE));
            }
        }

        return result;
    }

    /**
     * Creates a fully locked (all galactic) name.
     */
    public static Component createFullyLockedName(String enchantmentName) {
        return createDecodedName(enchantmentName, new int[0]);
    }

    /**
     * Creates a fully unlocked (all normal) name.
     */
    public static Component createFullyUnlockedName(String enchantmentName) {
        // Create array of all letter indices
        int letterCount = countLetters(enchantmentName);
        int[] allIndices = new int[letterCount];
        for (int i = 0; i < letterCount; i++) {
            allIndices[i] = i;
        }
        return createDecodedName(enchantmentName, allIndices);
    }

    /**
     * Counts the number of letters (A-Z, a-z) in a string.
     */
    public static int countLetters(String text) {
        if (text == null) return 0;
        int count = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Selects a random unselected letter index to unlock next.
     * Uses a deterministic seed based on player UUID and enchantment ID for consistency.
     *
     * @param enchantmentName The enchantment name
     * @param currentUnlocked Currently unlocked indices
     * @param seed Seed for deterministic selection
     * @return The next index to unlock, or -1 if all unlocked
     */
    public static int selectNextLetterToUnlock(String enchantmentName, int[] currentUnlocked, long seed) {
        int letterCount = countLetters(enchantmentName);
        if (letterCount == 0) return -1;

        Set<Integer> unlocked = new HashSet<>();
        if (currentUnlocked != null) {
            for (int idx : currentUnlocked) {
                unlocked.add(idx);
            }
        }

        // Find all locked indices
        int[] locked = new int[letterCount - unlocked.size()];
        int lockedIdx = 0;
        for (int i = 0; i < letterCount; i++) {
            if (!unlocked.contains(i)) {
                locked[lockedIdx++] = i;
            }
        }

        if (locked.length == 0) {
            return -1; // All unlocked
        }

        // Use seed to deterministically select
        java.util.Random random = new java.util.Random(seed + unlocked.size());
        return locked[random.nextInt(locked.length)];
    }

    /**
     * Calculates the percentage of letters unlocked.
     */
    public static float getUnlockProgress(String enchantmentName, int[] unlockedIndices) {
        int total = countLetters(enchantmentName);
        if (total == 0) return 1.0f;
        int unlocked = unlockedIndices != null ? unlockedIndices.length : 0;
        return (float) unlocked / total;
    }

    /**
     * Checks if all letters are unlocked.
     */
    public static boolean isFullyUnlocked(String enchantmentName, int[] unlockedIndices) {
        int total = countLetters(enchantmentName);
        int unlocked = unlockedIndices != null ? unlockedIndices.length : 0;
        return unlocked >= total;
    }
}
