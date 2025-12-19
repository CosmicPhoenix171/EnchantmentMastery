package com.enchantmentmastery.handler;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.capability.MasteryDataHelper;
import com.enchantmentmastery.network.ModNetworking;
import com.enchantmentmastery.util.EnchantComponentUtil;
import com.enchantmentmastery.util.EnchantRegistryUtil;
import com.enchantmentmastery.util.ProgressionMath;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

/**
 * Handles the absorption of enchanted books into player mastery data.
 */
@Mod.EventBusSubscriber(modid = EnchantmentMastery.MOD_ID)
public class AbsorbHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();

        // Only process on server side and when sneaking
        if (player.level().isClientSide() || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack heldStack = event.getItemStack();

        // Must be holding an enchanted book
        if (!heldStack.is(Items.ENCHANTED_BOOK)) {
            return;
        }

        // Try to absorb
        AbsorbResult result = tryAbsorb((ServerPlayer) player, heldStack, event.getHand());

        // Handle result
        if (result.success()) {
            event.setCanceled(true);
        } else if (result.message() != null) {
            player.displayClientMessage(result.message(), true);
            event.setCanceled(true);
        }
    }

    public static AbsorbResult tryAbsorb(ServerPlayer player, ItemStack bookStack, InteractionHand hand) {
        // Get stored enchantment from book
        Optional<Pair<Holder<Enchantment>, Integer>> singleEnchant =
                EnchantComponentUtil.getSingleStoredEnchantment(bookStack);

        if (singleEnchant.isEmpty()) {
            var allEnchants = EnchantComponentUtil.getStoredEnchantments(bookStack);
            if (allEnchants.isEmpty()) {
                return new AbsorbResult(false,
                        Component.translatable("enchantmentmastery.absorb.no_enchantment"));
            } else {
                return new AbsorbResult(false,
                        Component.translatable("enchantmentmastery.absorb.multiple_enchantments"));
            }
        }

        Holder<Enchantment> enchantHolder = singleEnchant.get().getLeft();
        int bookLevel = singleEnchant.get().getRight();

        Optional<ResourceLocation> enchantIdOpt = EnchantRegistryUtil.getEnchantmentId(enchantHolder);
        if (enchantIdOpt.isEmpty()) {
            return new AbsorbResult(false,
                    Component.translatable("enchantmentmastery.absorb.unknown_enchantment"));
        }

        ResourceLocation enchantId = enchantIdOpt.get();
        int currentMastery = MasteryDataHelper.getMasteryLevel(player, enchantId);

        // Check progression requirement
        if (bookLevel > currentMastery + 1) {
            return new AbsorbResult(false,
                    Component.translatable("enchantmentmastery.absorb.level_too_high",
                            bookLevel, currentMastery + 1));
        }

        if (bookLevel <= currentMastery) {
            return new AbsorbResult(false,
                    Component.translatable("enchantmentmastery.absorb.already_learned", bookLevel));
        }

        int xpCost = ProgressionMath.absorbCostLevels(bookLevel);

        if (player.experienceLevel < xpCost) {
            return new AbsorbResult(false,
                    Component.translatable("enchantmentmastery.absorb.not_enough_xp",
                            xpCost, player.experienceLevel));
        }

        // Perform absorption
        player.giveExperienceLevels(-xpCost);
        MasteryDataHelper.setMasteryLevel(player, enchantId, bookLevel);
        MasteryDataHelper.addLevelsSpent(player, xpCost);

        // Process decoding
        DecodingHandler.processLevelsSpent(player, enchantId, xpCost);

        // Consume the book
        if (bookStack.getCount() > 1) {
            bookStack.shrink(1);
        } else {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }

        // Play sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Sync data
        ModNetworking.syncMasteryData(player);

        String enchantName = enchantHolder.value().description().getString();
        player.displayClientMessage(
                Component.translatable("enchantmentmastery.absorb.success", enchantName, bookLevel),
                true
        );

        EnchantmentMastery.LOGGER.debug("Player {} absorbed {} level {} for {} XP levels",
                player.getName().getString(), enchantId, bookLevel, xpCost);

        return new AbsorbResult(true, null);
    }

    public record AbsorbResult(boolean success, Component message) {}
}
