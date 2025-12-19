package com.enchantmentmastery.handler;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles syncing mastery data when players log in or respawn.
 */
@Mod.EventBusSubscriber(modid = EnchantmentMastery.MOD_ID)
public class PlayerSyncHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModNetworking.syncMasteryData(serverPlayer);
            EnchantmentMastery.LOGGER.debug("Synced mastery data to {} on login",
                    serverPlayer.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModNetworking.syncMasteryData(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModNetworking.syncMasteryData(serverPlayer);
        }
    }
}
