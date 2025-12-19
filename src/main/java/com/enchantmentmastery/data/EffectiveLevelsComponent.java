package com.enchantmentmastery.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Data component storing effective enchantment levels beyond vanilla caps.
 * This is attached to items to track true levels for enchantments that exceed
 * vanilla maximum levels.
 *
 * Format: Map of enchantment ResourceLocation -> effective level (int)
 */
public record EffectiveLevelsComponent(Map<ResourceLocation, Integer> levels) {

    public static final EffectiveLevelsComponent EMPTY = new EffectiveLevelsComponent(Map.of());

    public static final Codec<EffectiveLevelsComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                            .fieldOf("levels")
                            .forGetter(EffectiveLevelsComponent::levels)
            ).apply(instance, EffectiveLevelsComponent::new)
    );

    public static final StreamCodec<ByteBuf, EffectiveLevelsComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(
                    HashMap::new,
                    ResourceLocation.STREAM_CODEC,
                    ByteBufCodecs.VAR_INT
            ),
            EffectiveLevelsComponent::levels,
            EffectiveLevelsComponent::new
    );

    /**
     * Gets the effective level for an enchantment.
     *
     * @param enchantId The enchantment ResourceLocation
     * @return The effective level, or 0 if not present
     */
    public int getLevel(ResourceLocation enchantId) {
        return levels.getOrDefault(enchantId, 0);
    }

    /**
     * Checks if this component has a level stored for the given enchantment.
     */
    public boolean hasLevel(ResourceLocation enchantId) {
        return levels.containsKey(enchantId) && levels.get(enchantId) > 0;
    }

    /**
     * Creates a new component with an updated level for the given enchantment.
     *
     * @param enchantId The enchantment to update
     * @param level The new level
     * @return A new EffectiveLevelsComponent with the updated level
     */
    public EffectiveLevelsComponent withLevel(ResourceLocation enchantId, int level) {
        Map<ResourceLocation, Integer> newLevels = new HashMap<>(levels);
        if (level <= 0) {
            newLevels.remove(enchantId);
        } else {
            newLevels.put(enchantId, level);
        }
        return new EffectiveLevelsComponent(Map.copyOf(newLevels));
    }

    /**
     * Creates a new component with a level removed.
     */
    public EffectiveLevelsComponent withoutLevel(ResourceLocation enchantId) {
        if (!levels.containsKey(enchantId)) {
            return this;
        }
        Map<ResourceLocation, Integer> newLevels = new HashMap<>(levels);
        newLevels.remove(enchantId);
        return new EffectiveLevelsComponent(Map.copyOf(newLevels));
    }

    /**
     * Checks if this component is empty (no levels stored).
     */
    public boolean isEmpty() {
        return levels.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EffectiveLevelsComponent other)) return false;
        return levels.equals(other.levels);
    }

    @Override
    public int hashCode() {
        return levels.hashCode();
    }
}
