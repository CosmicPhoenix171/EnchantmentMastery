package com.enchantmentmastery.capability;

import com.enchantmentmastery.EnchantmentMastery;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Forge Capability for player mastery data.
 */
public class MasteryCapability {
    public static final Capability<IPlayerMasteryData> PLAYER_MASTERY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation CAPABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(EnchantmentMastery.MOD_ID, "player_mastery");

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MasteryCapability::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerMasteryData.class);
    }

    public static LazyOptional<IPlayerMasteryData> get(Player player) {
        return player.getCapability(PLAYER_MASTERY);
    }

    /**
     * Interface for player mastery data.
     */
    public interface IPlayerMasteryData {
        int getMasteryLevel(ResourceLocation enchantId);
        void setMasteryLevel(ResourceLocation enchantId, int level);
        boolean hasEnchantmentUnlocked(ResourceLocation enchantId);
        Map<ResourceLocation, Integer> getAllMasteryLevels();

        int getMasteryXp(ResourceLocation enchantId);
        void setMasteryXp(ResourceLocation enchantId, int xp);
        void addMasteryXp(ResourceLocation enchantId, int xpToAdd);
        Map<ResourceLocation, Integer> getAllMasteryXp();

        int[] getUnlockedLetterIndices(ResourceLocation enchantId);
        void setUnlockedLetterIndices(ResourceLocation enchantId, int[] indices);
        void addUnlockedLetterIndex(ResourceLocation enchantId, int index);
        Map<ResourceLocation, int[]> getAllUnlockedLetters();

        int getTotalLevelsSpent();
        void addLevelsSpent(int levels);

        void copyFrom(IPlayerMasteryData other);
        CompoundTag serializeNBT();
        void deserializeNBT(CompoundTag nbt);
    }

    /**
     * Default implementation of player mastery data.
     */
    public static class PlayerMasteryData implements IPlayerMasteryData {
        private final Map<ResourceLocation, Integer> masteryLevels = new HashMap<>();
        private final Map<ResourceLocation, Integer> masteryXp = new HashMap<>();
        private final Map<ResourceLocation, int[]> unlockedLetterIndices = new HashMap<>();
        private int totalLevelsSpent = 0;

        @Override
        public int getMasteryLevel(ResourceLocation enchantId) {
            return masteryLevels.getOrDefault(enchantId, 0);
        }

        @Override
        public void setMasteryLevel(ResourceLocation enchantId, int level) {
            if (level <= 0) {
                masteryLevels.remove(enchantId);
            } else {
                masteryLevels.put(enchantId, level);
            }
        }

        @Override
        public boolean hasEnchantmentUnlocked(ResourceLocation enchantId) {
            return masteryLevels.containsKey(enchantId) && masteryLevels.get(enchantId) > 0;
        }

        @Override
        public Map<ResourceLocation, Integer> getAllMasteryLevels() {
            return new HashMap<>(masteryLevels);
        }

        @Override
        public int getMasteryXp(ResourceLocation enchantId) {
            return masteryXp.getOrDefault(enchantId, 0);
        }

        @Override
        public void setMasteryXp(ResourceLocation enchantId, int xp) {
            if (xp <= 0) {
                masteryXp.remove(enchantId);
            } else {
                masteryXp.put(enchantId, xp);
            }
        }

        @Override
        public void addMasteryXp(ResourceLocation enchantId, int xpToAdd) {
            int current = getMasteryXp(enchantId);
            setMasteryXp(enchantId, current + xpToAdd);
        }

        @Override
        public Map<ResourceLocation, Integer> getAllMasteryXp() {
            return new HashMap<>(masteryXp);
        }

        @Override
        public int[] getUnlockedLetterIndices(ResourceLocation enchantId) {
            return unlockedLetterIndices.getOrDefault(enchantId, new int[0]);
        }

        @Override
        public void setUnlockedLetterIndices(ResourceLocation enchantId, int[] indices) {
            if (indices == null || indices.length == 0) {
                unlockedLetterIndices.remove(enchantId);
            } else {
                unlockedLetterIndices.put(enchantId, indices.clone());
            }
        }

        @Override
        public void addUnlockedLetterIndex(ResourceLocation enchantId, int index) {
            int[] current = getUnlockedLetterIndices(enchantId);
            for (int i : current) {
                if (i == index) return;
            }
            int[] newArr = new int[current.length + 1];
            System.arraycopy(current, 0, newArr, 0, current.length);
            newArr[current.length] = index;
            unlockedLetterIndices.put(enchantId, newArr);
        }

        @Override
        public Map<ResourceLocation, int[]> getAllUnlockedLetters() {
            Map<ResourceLocation, int[]> copy = new HashMap<>();
            for (var entry : unlockedLetterIndices.entrySet()) {
                copy.put(entry.getKey(), entry.getValue().clone());
            }
            return copy;
        }

        @Override
        public int getTotalLevelsSpent() {
            return totalLevelsSpent;
        }

        @Override
        public void addLevelsSpent(int levels) {
            this.totalLevelsSpent += levels;
        }

        @Override
        public void copyFrom(IPlayerMasteryData other) {
            this.masteryLevels.clear();
            this.masteryLevels.putAll(other.getAllMasteryLevels());
            this.masteryXp.clear();
            this.masteryXp.putAll(other.getAllMasteryXp());
            this.unlockedLetterIndices.clear();
            for (var entry : other.getAllUnlockedLetters().entrySet()) {
                this.unlockedLetterIndices.put(entry.getKey(), entry.getValue().clone());
            }
            this.totalLevelsSpent = other.getTotalLevelsSpent();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();

            // Serialize mastery levels
            ListTag levelsTag = new ListTag();
            for (var entry : masteryLevels.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("id", entry.getKey().toString());
                entryTag.putInt("level", entry.getValue());
                levelsTag.add(entryTag);
            }
            tag.put("mastery_levels", levelsTag);

            // Serialize mastery XP
            ListTag xpTag = new ListTag();
            for (var entry : masteryXp.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("id", entry.getKey().toString());
                entryTag.putInt("xp", entry.getValue());
                xpTag.add(entryTag);
            }
            tag.put("mastery_xp", xpTag);

            // Serialize unlocked letters
            ListTag lettersTag = new ListTag();
            for (var entry : unlockedLetterIndices.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("id", entry.getKey().toString());
                entryTag.putIntArray("indices", entry.getValue());
                lettersTag.add(entryTag);
            }
            tag.put("unlocked_letters", lettersTag);

            tag.putInt("total_levels_spent", totalLevelsSpent);

            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            masteryLevels.clear();
            masteryXp.clear();
            unlockedLetterIndices.clear();

            // Deserialize mastery levels
            ListTag levelsTag = tag.getList("mastery_levels", Tag.TAG_COMPOUND);
            for (int i = 0; i < levelsTag.size(); i++) {
                CompoundTag entryTag = levelsTag.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(entryTag.getString("id"));
                if (id != null) {
                    masteryLevels.put(id, entryTag.getInt("level"));
                }
            }

            // Deserialize mastery XP
            ListTag xpTag = tag.getList("mastery_xp", Tag.TAG_COMPOUND);
            for (int i = 0; i < xpTag.size(); i++) {
                CompoundTag entryTag = xpTag.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(entryTag.getString("id"));
                if (id != null) {
                    masteryXp.put(id, entryTag.getInt("xp"));
                }
            }

            // Deserialize unlocked letters
            ListTag lettersTag = tag.getList("unlocked_letters", Tag.TAG_COMPOUND);
            for (int i = 0; i < lettersTag.size(); i++) {
                CompoundTag entryTag = lettersTag.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(entryTag.getString("id"));
                if (id != null) {
                    unlockedLetterIndices.put(id, entryTag.getIntArray("indices"));
                }
            }

            totalLevelsSpent = tag.getInt("total_levels_spent");
        }
    }

    /**
     * Capability provider for players.
     */
    public static class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final PlayerMasteryData data = new PlayerMasteryData();
        private final LazyOptional<IPlayerMasteryData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == PLAYER_MASTERY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.deserializeNBT(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }

    /**
     * Event handlers for capability attachment and cloning.
     */
    @Mod.EventBusSubscriber(modid = EnchantmentMastery.MOD_ID)
    public static class EventHandler {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(CAPABILITY_ID, new Provider());
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            // Copy capability data on death/respawn
            if (event.isWasDeath()) {
                event.getOriginal().reviveCaps();
                event.getOriginal().getCapability(PLAYER_MASTERY).ifPresent(oldData -> {
                    event.getEntity().getCapability(PLAYER_MASTERY).ifPresent(newData -> {
                        newData.copyFrom(oldData);
                    });
                });
                event.getOriginal().invalidateCaps();
            }
        }
    }
}
