package com.enchantmentmastery;

import com.enchantmentmastery.capability.MasteryCapability;
import com.enchantmentmastery.data.ModDataComponents;
import com.enchantmentmastery.network.ModNetworking;
import com.enchantmentmastery.registry.ModMenuTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(EnchantmentMastery.MOD_ID)
public class EnchantmentMastery {
    public static final String MOD_ID = "enchantmentmastery";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public EnchantmentMastery(FMLJavaModLoadingContext context) {
        LOGGER.info("Initializing Enchantment Mastery");

        IEventBus modEventBus = context.getModEventBus();

        // Register deferred registers
        ModDataComponents.REGISTRY.register(modEventBus);
        ModMenuTypes.REGISTRY.register(modEventBus);
        MasteryCapability.register(modEventBus);

        // Setup events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // Register ourselves for game events
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworking.register();
            LOGGER.info("Enchantment Mastery common setup complete");
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Enchantment Mastery client setup complete");
        });
    }
}
