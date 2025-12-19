package com.enchantmentmastery.util;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.data.EffectiveLevelsComponent;
import com.enchantmentmastery.data.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utility for reading and writing enchantment data components on items.
 * Handles both vanilla ENCHANTMENTS and STORED_ENCHANTMENTS (for books).
 */
public final class EnchantComponentUtil {
    private EnchantComponentUtil() {}

    /**
     * Gets the stored enchantments from an enchanted book.
     *
     * @param stack The item stack (should be an enchanted book)
     * @return Map of enchantment holder to level, empty if none
     */
    public static Map<Holder<Enchantment>, Integer> getStoredEnchantments(ItemStack stack) {
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) {
            return Map.of();
        }

        Map<Holder<Enchantment>, Integer> result = new HashMap<>();
        stored.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getIntValue()));
        return result;
    }

    /**
     * Gets the single stored enchantment from a book if it has exactly one.
     *
     * @param stack The enchanted book
     * @return Optional containing the enchantment holder and level, or empty
     */
    public static Optional<Pair<Holder<Enchantment>, Integer>> getSingleStoredEnchantment(ItemStack stack) {
        Map<Holder<Enchantment>, Integer> stored = getStoredEnchantments(stack);
        if (stored.size() != 1) {
            return Optional.empty();
        }
        var entry = stored.entrySet().iterator().next();
        return Optional.of(Pair.of(entry.getKey(), entry.getValue()));
    }

    /**
     * Gets the enchantments on a regular item (not stored enchantments).
     *
     * @param stack The item stack
     * @return Map of enchantment holder to level
     */
    public static Map<Holder<Enchantment>, Integer> getEnchantments(ItemStack stack) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            return Map.of();
        }

        Map<Holder<Enchantment>, Integer> result = new HashMap<>();
        enchants.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getIntValue()));
        return result;
    }

    /**
     * Adds or updates an enchantment on an item.
     * Uses vanilla capped levels for the actual enchantment but stores
     * effective level in our custom component.
     *
     * @param stack The item to enchant
     * @param enchantHolder The enchantment holder
     * @param effectiveLevel The effective level (can exceed vanilla max)
     */
    public static void applyEnchantmentWithEffectiveLevel(ItemStack stack, Holder<Enchantment> enchantHolder, int effectiveLevel) {
        Enchantment enchant = enchantHolder.value();

        // Cap the vanilla enchantment level at the max
        int vanillaLevel = Math.min(effectiveLevel, enchant.getMaxLevel());

        // Apply the vanilla enchantment
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
        );
        mutable.set(enchantHolder, vanillaLevel);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        // Store the effective level in our custom component
        if (effectiveLevel > vanillaLevel) {
            setEffectiveLevel(stack, enchantHolder, effectiveLevel);
        }
    }

    /**
     * Sets the effective level for an enchantment in our custom component.
     */
    public static void setEffectiveLevel(ItemStack stack, Holder<Enchantment> enchantHolder, int effectiveLevel) {
        ResourceLocation enchantId = enchantHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);

        if (enchantId == null) {
            EnchantmentMastery.LOGGER.warn("Cannot get ResourceLocation for enchantment holder");
            return;
        }

        EffectiveLevelsComponent current = stack.getOrDefault(
                ModDataComponents.EFFECTIVE_LEVELS.get(),
                EffectiveLevelsComponent.EMPTY
        );

        EffectiveLevelsComponent updated = current.withLevel(enchantId, effectiveLevel);
        stack.set(ModDataComponents.EFFECTIVE_LEVELS.get(), updated);
    }

    /**
     * Gets the effective level for an enchantment, checking custom component first.
     */
    public static int getEffectiveLevel(ItemStack stack, Holder<Enchantment> enchantHolder) {
        ResourceLocation enchantId = enchantHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);

        if (enchantId == null) {
            // Fallback to vanilla level
            return getVanillaLevel(stack, enchantHolder);
        }

        EffectiveLevelsComponent component = stack.get(ModDataComponents.EFFECTIVE_LEVELS.get());
        if (component != null) {
            int effectiveLevel = component.getLevel(enchantId);
            if (effectiveLevel > 0) {
                return effectiveLevel;
            }
        }

        // Fallback to vanilla level
        return getVanillaLevel(stack, enchantHolder);
    }

    /**
     * Gets the vanilla enchantment level on an item.
     */
    public static int getVanillaLevel(ItemStack stack, Holder<Enchantment> enchantHolder) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) {
            return 0;
        }
        return enchants.getLevel(enchantHolder);
    }

    /**
     * Checks if an item has any enchantments.
     */
    public static boolean hasEnchantments(ItemStack stack) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        return enchants != null && !enchants.isEmpty();
    }

    /**
     * Checks if an item is an enchanted book.
     */
    public static boolean isEnchantedBook(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK);
    }

    /**
     * Gets all effective levels for display purposes.
     * Returns a map of enchantment ID to effective level.
     */
    public static Map<ResourceLocation, Integer> getAllEffectiveLevels(ItemStack stack) {
        Map<ResourceLocation, Integer> result = new HashMap<>();

        // Start with vanilla enchantments
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants != null) {
            enchants.entrySet().forEach(entry -> {
                ResourceLocation id = entry.getKey().unwrapKey()
                        .map(key -> key.location())
                        .orElse(null);
                if (id != null) {
                    result.put(id, entry.getIntValue());
                }
            });
        }

        // Override with effective levels from our component
        EffectiveLevelsComponent component = stack.get(ModDataComponents.EFFECTIVE_LEVELS.get());
        if (component != null) {
            component.levels().forEach((id, level) -> {
                if (level > 0) {
                    result.put(id, level);
                }
            });
        }

        return result;
    }
}
