package com.enchantmentmastery.util;

/**
 * Utility class for converting integers to Roman numerals.
 * Supports very large numbers by repeating 'M' as needed.
 */
public final class RomanNumerals {
    private RomanNumerals() {}

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    /**
     * Converts an integer to its Roman numeral representation.
     * For very large numbers (>3999), uses repeated M characters.
     *
     * @param number The integer to convert (must be > 0)
     * @return The Roman numeral string
     * @throws IllegalArgumentException if number <= 0
     */
    public static String toRoman(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Roman numerals must be positive: " + number);
        }

        StringBuilder result = new StringBuilder();
        int remaining = number;

        for (int i = 0; i < VALUES.length; i++) {
            while (remaining >= VALUES[i]) {
                result.append(SYMBOLS[i]);
                remaining -= VALUES[i];
            }
        }

        return result.toString();
    }

    /**
     * Converts a Roman numeral string back to an integer.
     *
     * @param roman The Roman numeral string
     * @return The integer value
     * @throws IllegalArgumentException if the string is invalid
     */
    public static int fromRoman(String roman) {
        if (roman == null || roman.isEmpty()) {
            throw new IllegalArgumentException("Roman numeral string cannot be null or empty");
        }

        String upper = roman.toUpperCase();
        int result = 0;
        int i = 0;

        while (i < upper.length()) {
            // Check for two-character symbols first
            if (i + 1 < upper.length()) {
                String twoChar = upper.substring(i, i + 2);
                int twoCharValue = getValueForSymbol(twoChar);
                if (twoCharValue > 0) {
                    result += twoCharValue;
                    i += 2;
                    continue;
                }
            }

            // Single character
            String oneChar = upper.substring(i, i + 1);
            int oneCharValue = getValueForSymbol(oneChar);
            if (oneCharValue > 0) {
                result += oneCharValue;
                i++;
            } else {
                throw new IllegalArgumentException("Invalid Roman numeral character: " + oneChar);
            }
        }

        return result;
    }

    private static int getValueForSymbol(String symbol) {
        for (int i = 0; i < SYMBOLS.length; i++) {
            if (SYMBOLS[i].equals(symbol)) {
                return VALUES[i];
            }
        }
        return 0;
    }

    /**
     * Gets a Roman numeral for display, with special handling for level 0.
     *
     * @param level The level to convert
     * @return The Roman numeral or "0" if level is 0
     */
    public static String toRomanOrZero(int level) {
        if (level <= 0) {
            return "0";
        }
        return toRoman(level);
    }
}
