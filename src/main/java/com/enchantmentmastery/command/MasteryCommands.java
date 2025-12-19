package com.enchantmentmastery.command;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.capability.MasteryDataHelper;
import com.enchantmentmastery.network.ModNetworking;
import com.enchantmentmastery.util.RomanNumerals;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Debug commands for testing the mastery system.
 */
@Mod.EventBusSubscriber(modid = EnchantmentMastery.MOD_ID)
public class MasteryCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("mastery")
                        .requires(source -> source.hasPermission(2)) // Op level 2

                        // /mastery list - Show all learned enchantments
                        .then(Commands.literal("list")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    Map<ResourceLocation, Integer> levels = MasteryDataHelper.getAllMasteryLevels(player);

                                    if (levels.isEmpty()) {
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("No enchantments learned yet."), false);
                                    } else {
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("Learned enchantments:"), false);
                                        for (var entry : levels.entrySet()) {
                                            String roman = RomanNumerals.toRoman(entry.getValue());
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("  " + entry.getKey() + " " + roman), false);
                                        }
                                    }
                                    return levels.size();
                                }))

                        // /mastery set <enchant_id> <level> - Set mastery level
                        .then(Commands.literal("set")
                                .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 1000))
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    ResourceLocation enchantId = ResourceLocationArgument.getId(context, "enchantment");
                                                    int level = IntegerArgumentType.getInteger(context, "level");

                                                    MasteryDataHelper.setMasteryLevel(player, enchantId, level);
                                                    ModNetworking.syncMasteryData(player);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Set " + enchantId + " mastery to " + level), true);
                                                    return 1;
                                                }))))

                        // /mastery reset - Reset all mastery data
                        .then(Commands.literal("reset")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    var data = MasteryDataHelper.getData(player);

                                    // Clear all data by setting levels to 0
                                    for (var id : data.getAllMasteryLevels().keySet()) {
                                        data.setMasteryLevel(id, 0);
                                        data.setMasteryXp(id, 0);
                                        data.setUnlockedLetterIndices(id, new int[0]);
                                    }

                                    ModNetworking.syncMasteryData(player);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Reset all mastery data."), true);
                                    return 1;
                                }))

                        // /mastery stats - Show stats
                        .then(Commands.literal("stats")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    var data = MasteryDataHelper.getData(player);

                                    int totalEnchants = data.getAllMasteryLevels().size();
                                    int totalLevelsSpent = data.getTotalLevelsSpent();
                                    int combinedMastery = data.getAllMasteryLevels().values().stream()
                                            .mapToInt(Integer::intValue).sum();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Mastery Stats:"), false);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("  Enchantments learned: " + totalEnchants), false);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("  Total levels spent: " + totalLevelsSpent), false);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("  Combined mastery: " + combinedMastery), false);
                                    return 1;
                                }))
        );
    }
}
