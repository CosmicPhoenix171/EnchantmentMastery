package com.enchantmentmastery.util;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;
import java.util.Optional;

/**
 * Utility for registry lookups and enchantment compatibility checks.
 */
public final class EnchantRegistryUtil {
    private EnchantRegistryUtil() {}

    /**
     * Gets the enchantment registry from a registry access.
     */
    public static Registry<Enchantment> getRegistry(RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(Registries.ENCHANTMENT);
    }

    /**
     * Looks up an enchantment by ResourceLocation.
     */
    public static Optional<Holder.Reference<Enchantment>> getEnchantment(RegistryAccess registryAccess, ResourceLocation id) {
        Registry<Enchantment> registry = getRegistry(registryAccess);
        return registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, id));
    }

    /**
     * Gets the ResourceLocation for an enchantment holder.
     */
    public static Optional<ResourceLocation> getEnchantmentId(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(ResourceKey::location);
    }

    /**
     * Checks if an enchantment can be applied to an item.
     *
     * @param stack The item to enchant
     * @param enchantHolder The enchantment to apply
     * @return true if the enchantment is valid for this item type
     */
    public static boolean canEnchantItem(ItemStack stack, Holder<Enchantment> enchantHolder) {
        Enchantment enchant = enchantHolder.value();
        // Check if the enchantment supports this item via its primary items or supported items
        return enchant.canEnchant(stack);
    }

    /**
     * Checks if a new enchantment conflicts with any existing enchantments on an item.
     *
     * @param stack The item with existing enchantments
     * @param newEnchantHolder The enchantment to add
     * @return true if there's a conflict, false if compatible
     */
    public static boolean hasConflict(ItemStack stack, Holder<Enchantment> newEnchantHolder) {
        Map<Holder<Enchantment>, Integer> existing = EnchantComponentUtil.getEnchantments(stack);

        for (Holder<Enchantment> existingHolder : existing.keySet()) {
            if (areIncompatible(existingHolder, newEnchantHolder)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if two enchantments are incompatible (conflict with each other).
     */
    public static boolean areIncompatible(Holder<Enchantment> enchant1, Holder<Enchantment> enchant2) {
        // Same enchantment is not a "conflict" - it would be an upgrade
        if (enchant1.equals(enchant2)) {
            return false;
        }

        Enchantment e1 = enchant1.value();
        Enchantment e2 = enchant2.value();

        // Check both directions for compatibility
        return !Enchantment.areCompatible(enchant1, enchant2);
    }

    /**
     * Gets the maximum vanilla level for an enchantment.
     */
    public static int getMaxVanillaLevel(Holder<Enchantment> holder) {
        return holder.value().getMaxLevel();
    }

    /**
     * Gets the minimum level for an enchantment.
     */
    public static int getMinLevel(Holder<Enchantment> holder) {
        return holder.value().getMinLevel();
    }

    /**
     * Gets the display name key for an enchantment.
     */
    public static String getDescriptionId(Holder<Enchantment> holder) {
        return holder.value().description().getString();
    }

    /**
     * Validates if an enchantment can be applied to an item considering both
     * item compatibility and conflicts.
     *
     * @param stack The item to enchant
     * @param enchantHolder The enchantment to apply
     * @return ValidationResult with success/failure and reason
     */
    public static ValidationResult validateEnchantment(ItemStack stack, Holder<Enchantment> enchantHolder) {
        // Check item compatibility
        if (!canEnchantItem(stack, enchantHolder)) {
            return new ValidationResult(false, "enchantment.incompatible.item");
        }

        // Check for conflicts
        if (hasConflict(stack, enchantHolder)) {
            return new ValidationResult(false, "enchantment.incompatible.conflict");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Result of enchantment validation.
     */
    public record ValidationResult(boolean valid, String errorKey) {
        public boolean isValid() {
            return valid;
        }

        public String getErrorKey() {
            return errorKey;
        }
    }
}
