package com.enchantmentmastery.client;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.capability.MasteryDataHelper;
import com.enchantmentmastery.data.EffectiveLevelsComponent;
import com.enchantmentmastery.data.ModDataComponents;
import com.enchantmentmastery.util.DecodingUtil;
import com.enchantmentmastery.util.EnchantComponentUtil;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles custom tooltip rendering for enchantments.
 * Shows effective levels with Roman numerals and decoded names.
 */
@Mod.EventBusSubscriber(modid = EnchantmentMastery.MOD_ID, value = Dist.CLIENT)
public class TooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Check if item has enchantments
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            return;
        }

        // Get effective levels component if present
        EffectiveLevelsComponent effectiveLevels = stack.get(ModDataComponents.EFFECTIVE_LEVELS.get());

        // Get the current player for decoding data
        Player player = Minecraft.getInstance().player;

        // Find and replace enchantment lines in the tooltip
        List<Component> tooltip = event.getToolTip();
        List<Integer> indicesToRemove = new ArrayList<>();
        List<Component> newLines = new ArrayList<>();

        // Collect vanilla enchantment lines to replace
        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            String lineText = line.getString();

            // Check each enchantment to see if this line is for it
            for (var entry : enchants.entrySet()) {
                Holder<Enchantment> holder = entry.getKey();
                int vanillaLevel = entry.getIntValue();

                String enchantName = holder.value().description().getString();

                // Check if this tooltip line contains this enchantment name
                if (lineText.contains(enchantName)) {
                    // Get effective level
                    ResourceLocation enchantId = holder.unwrapKey()
                            .map(key -> key.location())
                            .orElse(null);

                    int displayLevel = vanillaLevel;
                    if (enchantId != null && effectiveLevels != null) {
                        int effective = effectiveLevels.getLevel(enchantId);
                        if (effective > 0) {
                            displayLevel = effective;
                        }
                    }

                    // Create custom line with decoded name and Roman numeral
                    Component customLine = createEnchantmentLine(holder, displayLevel, enchantId, player);

                    indicesToRemove.add(i);
                    newLines.add(customLine);
                    break;
                }
            }
        }

        // Replace lines (in reverse order to maintain indices)
        for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
            int index = indicesToRemove.get(i);
            tooltip.set(index, newLines.get(i));
        }
    }

    /**
     * Creates a custom enchantment tooltip line with decoded name and Roman numeral.
     */
    private static Component createEnchantmentLine(Holder<Enchantment> holder, int level,
                                                   ResourceLocation enchantId, Player player) {
        Enchantment enchant = holder.value();
        String enchantName = enchant.description().getString();

        // Get decoded name based on player's unlocked letters
        Component decodedName;
        if (player != null && enchantId != null) {
            int[] unlockedIndices = MasteryDataHelper.getUnlockedLetterIndices(player, enchantId);
            decodedName = DecodingUtil.createDecodedName(enchantName, unlockedIndices);
        } else {
            // If no player (shouldn't happen normally), show fully locked
            decodedName = DecodingUtil.createFullyLockedName(enchantName);
        }

        // Get Roman numeral for level (always readable)
        String romanLevel = RomanNumerals.toRoman(level);

        // Combine: "DecodedName LXXIII"
        MutableComponent result = Component.empty();
        result = result.append(decodedName);

        // Only add level if > 1 (vanilla behavior) or if it exceeds max (mastery behavior)
        if (level > 1 || level > enchant.getMaxLevel()) {
            result = result.append(Component.literal(" " + romanLevel));
        }

        // Apply curse coloring if applicable
        if (holder.is(EnchantmentTags.CURSE)) {
            result = result.withStyle(ChatFormatting.RED);
        } else {
            result = result.withStyle(ChatFormatting.GRAY);
        }

        return result;
    }
}
