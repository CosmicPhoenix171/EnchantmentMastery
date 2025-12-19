package com.enchantmentmastery.menu;

import com.enchantmentmastery.capability.MasteryDataHelper;
import com.enchantmentmastery.handler.DecodingHandler;
import com.enchantmentmastery.network.ModNetworking;
import com.enchantmentmastery.registry.ModMenuTypes;
import com.enchantmentmastery.util.EnchantComponentUtil;
import com.enchantmentmastery.util.EnchantRegistryUtil;
import com.enchantmentmastery.util.ProgressionMath;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

/**
 * Custom menu for the Mastery Enchanter.
 * Allows players to apply learned enchantments to items.
 */
public class MasteryEnchanterMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Player player;
    private final Container inputSlot;

    // Cached data for client display
    private List<EnchantmentEntry> availableEnchantments = new ArrayList<>();
    private ResourceLocation selectedEnchantment = null;
    private int selectedLevel = 0;

    public MasteryEnchanterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public MasteryEnchanterMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.MASTERY_ENCHANTER.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;
        this.inputSlot = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                MasteryEnchanterMenu.this.slotsChanged(this);
            }
        };

        // Input slot for item to enchant (center-top area)
        this.addSlot(new Slot(inputSlot, 0, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Don't allow enchanted books in input
                return !stack.is(Items.ENCHANTED_BOOK);
            }
        });

        // Player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // Update available enchantments
        updateAvailableEnchantments();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateAvailableEnchantments();
    }

    /**
     * Updates the list of available enchantments based on player mastery
     * and the current item in the input slot.
     */
    public void updateAvailableEnchantments() {
        availableEnchantments.clear();
        selectedEnchantment = null;
        selectedLevel = 0;

        ItemStack inputStack = inputSlot.getItem(0);

        // Get player's mastery data
        Map<ResourceLocation, Integer> masteryLevels = MasteryDataHelper.getAllMasteryLevels(player);

        if (inputStack.isEmpty()) {
            // Show all learned enchantments (greyed out / preview)
            for (var entry : masteryLevels.entrySet()) {
                availableEnchantments.add(new EnchantmentEntry(
                        entry.getKey(),
                        entry.getValue(),
                        false, // Not applicable (no item)
                        false  // No conflict
                ));
            }
        } else {
            // Filter to applicable enchantments
            Registry<Enchantment> registry = player.level().registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT);

            for (var entry : masteryLevels.entrySet()) {
                ResourceLocation enchantId = entry.getKey();
                int masteryLevel = entry.getValue();

                Optional<Holder.Reference<Enchantment>> holderOpt =
                        EnchantRegistryUtil.getEnchantment(player.level().registryAccess(), enchantId);

                if (holderOpt.isPresent()) {
                    Holder<Enchantment> holder = holderOpt.get();
                    boolean canApply = EnchantRegistryUtil.canEnchantItem(inputStack, holder);
                    boolean hasConflict = EnchantRegistryUtil.hasConflict(inputStack, holder);

                    availableEnchantments.add(new EnchantmentEntry(
                            enchantId,
                            masteryLevel,
                            canApply,
                            hasConflict
                    ));
                }
            }
        }

        // Sort: applicable first, then alphabetically
        availableEnchantments.sort((a, b) -> {
            if (a.applicable() && !b.applicable()) return -1;
            if (!a.applicable() && b.applicable()) return 1;
            return a.enchantId().compareTo(b.enchantId());
        });
    }

    /**
     * Selects an enchantment and level for application.
     * Called from client via packet.
     */
    public void selectEnchantment(ResourceLocation enchantId, int level) {
        this.selectedEnchantment = enchantId;
        this.selectedLevel = level;
    }

    /**
     * Attempts to apply the selected enchantment.
     * Server-side only.
     *
     * @return true if successful
     */
    public boolean tryApplyEnchantment() {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (selectedEnchantment == null || selectedLevel <= 0) {
            return false;
        }

        ItemStack inputStack = inputSlot.getItem(0);
        if (inputStack.isEmpty()) {
            return false;
        }

        // Verify mastery level
        int masteryLevel = MasteryDataHelper.getMasteryLevel(player, selectedEnchantment);
        if (selectedLevel > masteryLevel) {
            serverPlayer.displayClientMessage(
                    Component.translatable("enchantmentmastery.apply.mastery_too_low"), true);
            return false;
        }

        // Get enchantment holder
        Optional<Holder.Reference<Enchantment>> holderOpt =
                EnchantRegistryUtil.getEnchantment(player.level().registryAccess(), selectedEnchantment);

        if (holderOpt.isEmpty()) {
            return false;
        }

        Holder<Enchantment> holder = holderOpt.get();

        // Validate compatibility
        var validation = EnchantRegistryUtil.validateEnchantment(inputStack, holder);
        if (!validation.isValid()) {
            serverPlayer.displayClientMessage(
                    Component.translatable(validation.getErrorKey()), true);
            return false;
        }

        // Calculate cost
        int xpCost = ProgressionMath.applyCostLevels(selectedLevel);

        // Check XP
        if (serverPlayer.experienceLevel < xpCost) {
            serverPlayer.displayClientMessage(
                    Component.translatable("enchantmentmastery.apply.not_enough_xp",
                            xpCost, serverPlayer.experienceLevel), true);
            return false;
        }

        // All checks passed - apply the enchantment

        // Deduct XP
        serverPlayer.giveExperienceLevels(-xpCost);

        // Apply enchantment with effective level tracking
        EnchantComponentUtil.applyEnchantmentWithEffectiveLevel(inputStack, holder, selectedLevel);

        // Track levels spent
        MasteryDataHelper.addLevelsSpent(serverPlayer, xpCost);

        // Add mastery XP
        int xpGain = ProgressionMath.masteryXpGainFromApplyCost(xpCost);
        processXpGainAndLevelUp(serverPlayer, selectedEnchantment, xpGain);

        // Process decoding (unlock letters based on levels spent)
        DecodingHandler.processLevelsSpent(serverPlayer, selectedEnchantment, xpCost);

        // Play sound
        serverPlayer.level().playSound(null,
                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Sync data
        ModNetworking.syncMasteryData(serverPlayer);

        // Update available enchantments
        updateAvailableEnchantments();

        // Clear selection
        selectedEnchantment = null;
        selectedLevel = 0;

        return true;
    }

    /**
     * Processes XP gain and handles level-ups for mastery.
     */
    private void processXpGainAndLevelUp(ServerPlayer player, ResourceLocation enchantId, int xpGain) {
        var data = MasteryDataHelper.getData(player);
        int currentLevel = data.getMasteryLevel(enchantId);
        int currentXp = data.getMasteryXp(enchantId);

        int[] result = new int[2];
        ProgressionMath.processXpGain(currentLevel, currentXp, xpGain, result);

        int newLevel = result[0];
        int newXp = result[1];

        if (newLevel > currentLevel) {
            data.setMasteryLevel(enchantId, newLevel);
            player.displayClientMessage(
                    Component.translatable("enchantmentmastery.mastery_level_up",
                            enchantId.toString(), newLevel), false);
        }
        data.setMasteryXp(enchantId, newXp);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index == 0) {
                // Moving from input slot to player inventory
                if (!this.moveItemStackTo(slotStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to input slot
                if (this.slots.get(0).mayPlace(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 28) {
                    // Move from main inventory to hotbar
                    if (!this.moveItemStackTo(slotStack, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Move from hotbar to main inventory
                    if (!this.moveItemStackTo(slotStack, 1, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                        level.getBlockState(pos).is(Blocks.ENCHANTING_TABLE) &&
                                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Return items in input slot to player
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputSlot));
    }

    // --- Getters for Screen ---

    public List<EnchantmentEntry> getAvailableEnchantments() {
        return availableEnchantments;
    }

    public ResourceLocation getSelectedEnchantment() {
        return selectedEnchantment;
    }

    public int getSelectedLevel() {
        return selectedLevel;
    }

    public ItemStack getInputItem() {
        return inputSlot.getItem(0);
    }

    /**
     * Entry representing an available enchantment in the menu.
     */
    public record EnchantmentEntry(
            ResourceLocation enchantId,
            int masteryLevel,
            boolean applicable,
            boolean hasConflict
    ) {}
}
