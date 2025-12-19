package com.enchantmentmastery.client;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.client.screen.MasteryEnchanterScreen;
import com.enchantmentmastery.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side event handlers for registering screens and renderers.
 */
@Mod.EventBusSubscriber(modid = EnchantmentMastery.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.MASTERY_ENCHANTER.get(), MasteryEnchanterScreen::new);
        });
    }
}
