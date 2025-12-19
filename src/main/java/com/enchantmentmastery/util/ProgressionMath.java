package com.enchantmentmastery.util;

/**
 * Tunable progression math module for the mastery system.
 * All formulas use enchanting-style quadratic scaling.
 */
public final class ProgressionMath {
    private ProgressionMath() {}

    // --- Configuration constants (can be made configurable via a config file) ---

    /** Base cost multiplier for absorbing books */
    private static final double ABSORB_BASE_COST = 3.0;
    /** Quadratic factor for absorb cost */
    private static final double ABSORB_QUADRATIC = 1.5;

    /** Base cost multiplier for applying enchants */
    private static final double APPLY_BASE_COST = 2.0;
    /** Quadratic factor for apply cost */
    private static final double APPLY_QUADRATIC = 1.2;

    /** Base XP required for first mastery level up */
    private static final double MASTERY_XP_BASE = 10.0;
    /** Quadratic factor for mastery XP scaling */
    private static final double MASTERY_XP_QUADRATIC = 1.5;

    /** Percentage of apply cost converted to mastery XP (as integer) */
    private static final double XP_GAIN_MULTIPLIER = 5.0;

    /** Base cost for unlocking letters */
    private static final double DECODE_BASE_COST = 1.0;
    /** Cost multiplier per letter already unlocked */
    private static final double DECODE_SCALING = 0.5;

    // --- Absorb Cost ---

    /**
     * Calculate the XP level cost to absorb an enchanted book of the given level.
     * Cost increases quadratically with book level.
     *
     * @param bookLevel The enchantment level on the book (1, 2, 3, etc.)
     * @return The XP levels required to absorb
     */
    public static int absorbCostLevels(int bookLevel) {
        if (bookLevel <= 0) return 0;
        // Formula: base * level + quadratic * level^2
        double cost = ABSORB_BASE_COST * bookLevel + ABSORB_QUADRATIC * bookLevel * bookLevel;
        return Math.max(1, (int) Math.ceil(cost));
    }

    // --- Apply Cost ---

    /**
     * Calculate the XP level cost to apply an enchantment at the given target level.
     * Cost increases quadratically with target level.
     *
     * @param targetLevel The level to apply (1, 2, 3, etc.)
     * @return The XP levels required to apply
     */
    public static int applyCostLevels(int targetLevel) {
        if (targetLevel <= 0) return 0;
        // Formula: base * level + quadratic * level^2
        double cost = APPLY_BASE_COST * targetLevel + APPLY_QUADRATIC * targetLevel * targetLevel;
        return Math.max(1, (int) Math.ceil(cost));
    }

    // --- Mastery XP ---

    /**
     * Calculate the mastery XP required to advance from the current mastery level
     * to the next level. This scales like enchanting costs.
     *
     * @param currentMasteryLevel The current mastery level (0, 1, 2, etc.)
     * @return The XP points required to reach the next level
     */
    public static int masteryXpToNext(int currentMasteryLevel) {
        if (currentMasteryLevel < 0) currentMasteryLevel = 0;
        // Formula similar to vanilla enchanting: base + level * quadratic + level^2
        double xpNeeded = MASTERY_XP_BASE +
                currentMasteryLevel * MASTERY_XP_QUADRATIC * 2 +
                currentMasteryLevel * currentMasteryLevel * MASTERY_XP_QUADRATIC;
        return Math.max(1, (int) Math.ceil(xpNeeded));
    }

    /**
     * Calculate mastery XP gained from applying an enchantment.
     * Based on the XP levels spent on the apply.
     *
     * @param applyCostLevels The XP levels spent on the apply action
     * @return The mastery XP points gained
     */
    public static int masteryXpGainFromApplyCost(int applyCostLevels) {
        if (applyCostLevels <= 0) return 0;
        // Gain is a multiplied portion of the cost spent
        return Math.max(1, (int) Math.ceil(applyCostLevels * XP_GAIN_MULTIPLIER));
    }

    // --- Decoding Cost ---

    /**
     * Calculate the XP level cost to unlock the next letter in an enchantment name.
     *
     * @param lettersAlreadyUnlocked How many letters are already unlocked for this enchant
     * @return The XP levels required to unlock the next letter
     */
    public static int decodeCostLevels(int lettersAlreadyUnlocked) {
        if (lettersAlreadyUnlocked < 0) lettersAlreadyUnlocked = 0;
        // Cost increases with more letters unlocked
        double cost = DECODE_BASE_COST + DECODE_SCALING * lettersAlreadyUnlocked;
        return Math.max(1, (int) Math.ceil(cost));
    }

    // --- Level Processing ---

    /**
     * Process mastery XP gain and handle level-ups.
     * Returns the new mastery level after processing all level-ups.
     *
     * @param currentLevel Current mastery level
     * @param currentXp Current mastery XP (before adding new XP)
     * @param xpToAdd XP to add
     * @param result Output array: [0] = new level, [1] = remaining XP
     */
    public static void processXpGain(int currentLevel, int currentXp, int xpToAdd, int[] result) {
        int level = currentLevel;
        int xp = currentXp + xpToAdd;

        // Process level-ups
        int xpNeeded = masteryXpToNext(level);
        while (xp >= xpNeeded) {
            xp -= xpNeeded;
            level++;
            xpNeeded = masteryXpToNext(level);
        }

        result[0] = level;
        result[1] = xp;
    }

    // --- Convenience method for total cost preview ---

    /**
     * Calculate the total XP levels needed to absorb up to a target level.
     *
     * @param fromLevel Current mastery level (exclusive)
     * @param toLevel Target level (inclusive)
     * @return Total XP levels needed
     */
    public static int totalAbsorbCost(int fromLevel, int toLevel) {
        int total = 0;
        for (int i = fromLevel + 1; i <= toLevel; i++) {
            total += absorbCostLevels(i);
        }
        return total;
    }
}
