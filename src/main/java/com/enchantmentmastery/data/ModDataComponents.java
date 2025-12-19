package com.enchantmentmastery.data;

import com.enchantmentmastery.EnchantmentMastery;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom data components.
 */
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, EnchantmentMastery.MOD_ID);

    /**
     * Stores effective enchantment levels that exceed vanilla caps.
     * Applied to items to track true levels beyond vanilla maximum.
     */
    public static final RegistryObject<DataComponentType<EffectiveLevelsComponent>> EFFECTIVE_LEVELS =
            REGISTRY.register("effective_levels", () ->
                    DataComponentType.<EffectiveLevelsComponent>builder()
                            .persistent(EffectiveLevelsComponent.CODEC)
                            .networkSynchronized(EffectiveLevelsComponent.STREAM_CODEC)
                            .build()
            );
}
