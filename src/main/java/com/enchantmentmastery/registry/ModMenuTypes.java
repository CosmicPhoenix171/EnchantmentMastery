package com.enchantmentmastery.registry;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.menu.MasteryEnchanterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom menu types.
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> REGISTRY =
            DeferredRegister.create(Registries.MENU, EnchantmentMastery.MOD_ID);

    public static final RegistryObject<MenuType<MasteryEnchanterMenu>> MASTERY_ENCHANTER =
            REGISTRY.register("mastery_enchanter", () ->
                    new MenuType<>(MasteryEnchanterMenu::new, FeatureFlags.DEFAULT_FLAGS)
            );
}
