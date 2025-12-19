package com.enchantmentmastery.handler;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.menu.MasteryEnchanterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Handles opening the Mastery Enchanter menu.
 */
@Mod.EventBusSubscriber(modid = EnchantmentMastery.MOD_ID)
public class MasteryEnchanterHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();

        if (player.level().isClientSide() || !player.isShiftKeyDown()) {
            return;
        }

        if (!player.level().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) {
            return;
        }

        var heldStack = event.getItemStack();
        if (heldStack.is(Items.ENCHANTED_BOOK)) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.enchantmentmastery.mastery_enchanter");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new MasteryEnchanterMenu(containerId, playerInventory,
                            ContainerLevelAccess.create(player.level(), event.getPos()));
                }
            });

            event.setCanceled(true);
        }
    }
}
