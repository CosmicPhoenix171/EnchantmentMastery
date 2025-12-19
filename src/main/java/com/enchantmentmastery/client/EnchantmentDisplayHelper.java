package com.enchantmentmastery.client;

import com.enchantmentmastery.capability.MasteryDataHelper;
import com.enchantmentmastery.data.EffectiveLevelsComponent;
import com.enchantmentmastery.data.ModDataComponents;
import com.enchantmentmastery.util.DecodingUtil;
import com.enchantmentmastery.util.RomanNumerals;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.EnchantmentTags;

/**
 * Helper class for creating enchantment display components.
 */
public final class EnchantmentDisplayHelper {
    private EnchantmentDisplayHelper() {}

    /**
     * Creates a display component for an enchantment with decoded name and Roman numeral level.
     *
     * @param holder The enchantment holder
     * @param effectiveLevel The effective level to display
     * @param player The player viewing the item (for decoding)
     * @return A formatted component
     */
    public static Component createEnchantmentDisplay(Holder<Enchantment> holder, int effectiveLevel, Player player) {
        Enchantment enchant = holder.value();
        String enchantName = enchant.description().getString();

        ResourceLocation enchantId = holder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);

        // Get decoded name
        Component decodedName;
        if (player != null && enchantId != null) {
            int[] unlockedIndices = MasteryDataHelper.getUnlockedLetterIndices(player, enchantId);
            decodedName = DecodingUtil.createDecodedName(enchantName, unlockedIndices);
        } else {
            decodedName = DecodingUtil.createFullyLockedName(enchantName);
        }

        // Build result
        MutableComponent result = Component.empty().append(decodedName);

        // Add level in Roman numerals
        if (effectiveLevel > 1 || effectiveLevel > enchant.getMaxLevel()) {
            String romanLevel = RomanNumerals.toRoman(effectiveLevel);
            result = result.append(Component.literal(" " + romanLevel));
        }

        // Apply styling
        if (holder.is(EnchantmentTags.CURSE)) {
            result = result.withStyle(ChatFormatting.RED);
        } else {
            result = result.withStyle(ChatFormatting.GRAY);
        }

        return result;
    }

    /**
     * Gets the effective level of an enchantment on an item.
     */
    public static int getEffectiveLevel(ItemStack stack, Holder<Enchantment> holder) {
        ResourceLocation enchantId = holder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);

        if (enchantId == null) {
            return getVanillaLevel(stack, holder);
        }

        EffectiveLevelsComponent component = stack.get(ModDataComponents.EFFECTIVE_LEVELS.get());
        if (component != null && component.hasLevel(enchantId)) {
            return component.getLevel(enchantId);
        }

        return getVanillaLevel(stack, holder);
    }

    /**
     * Gets the vanilla enchantment level on an item.
     */
    public static int getVanillaLevel(ItemStack stack, Holder<Enchantment> holder) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) {
            return 0;
        }
        return enchants.getLevel(holder);
    }

    /**
     * Creates a simple enchantment component without decoding (for non-player contexts).
     */
    public static Component createSimpleEnchantmentDisplay(Holder<Enchantment> holder, int level) {
        Enchantment enchant = holder.value();
        String enchantName = enchant.description().getString();

        MutableComponent result = Component.literal(enchantName);

        if (level > 1 || level > enchant.getMaxLevel()) {
            String romanLevel = RomanNumerals.toRoman(level);
            result = result.append(Component.literal(" " + romanLevel));
        }

        if (holder.is(EnchantmentTags.CURSE)) {
            result = result.withStyle(ChatFormatting.RED);
        } else {
            result = result.withStyle(ChatFormatting.GRAY);
        }

        return result;
    }
}
