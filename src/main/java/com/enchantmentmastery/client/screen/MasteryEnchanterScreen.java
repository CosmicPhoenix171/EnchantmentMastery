package com.enchantmentmastery.client.screen;

import com.enchantmentmastery.EnchantmentMastery;
import com.enchantmentmastery.menu.MasteryEnchanterMenu;
import com.enchantmentmastery.network.ApplyEnchantmentPacket;
import com.enchantmentmastery.network.ModNetworking;
import com.enchantmentmastery.util.ProgressionMath;
import com.enchantmentmastery.util.RomanNumerals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Client screen for the Mastery Enchanter.
 * Displays available learned enchantments and allows selection and application.
 */
public class MasteryEnchanterScreen extends AbstractContainerScreen<MasteryEnchanterMenu> {
    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnchantmentMastery.MOD_ID, "textures/gui/mastery_enchanter.png");

    // Scrollable enchantment list
    private List<EnchantmentButton> enchantmentButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int VISIBLE_ENTRIES = 4;
    private static final int ENTRY_HEIGHT = 20;

    // Level selection
    private int selectedLevel = 1;
    private ResourceLocation selectedEnchantId = null;
    private Button applyButton;
    private Button levelUpButton;
    private Button levelDownButton;

    public MasteryEnchanterScreen(MasteryEnchanterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Apply button
        applyButton = Button.builder(
                        Component.translatable("enchantmentmastery.gui.apply"),
                        btn -> onApplyClicked())
                .bounds(leftPos + 120, topPos + 55, 50, 20)
                .build();
        addRenderableWidget(applyButton);

        // Level adjustment buttons
        levelDownButton = Button.builder(
                        Component.literal("-"),
                        btn -> adjustLevel(-1))
                .bounds(leftPos + 120, topPos + 35, 20, 15)
                .build();
        addRenderableWidget(levelDownButton);

        levelUpButton = Button.builder(
                        Component.literal("+"),
                        btn -> adjustLevel(1))
                .bounds(leftPos + 150, topPos + 35, 20, 15)
                .build();
        addRenderableWidget(levelUpButton);

        rebuildEnchantmentList();
    }

    private void rebuildEnchantmentList() {
        // Remove old buttons
        for (EnchantmentButton btn : enchantmentButtons) {
            removeWidget(btn);
        }
        enchantmentButtons.clear();

        // Get available enchantments
        List<MasteryEnchanterMenu.EnchantmentEntry> entries = menu.getAvailableEnchantments();

        // Create buttons for visible entries
        int y = topPos + 17;
        for (int i = 0; i < Math.min(VISIBLE_ENTRIES, entries.size() - scrollOffset); i++) {
            int index = scrollOffset + i;
            if (index < entries.size()) {
                MasteryEnchanterMenu.EnchantmentEntry entry = entries.get(index);
                EnchantmentButton btn = new EnchantmentButton(
                        leftPos + 8, y + i * ENTRY_HEIGHT,
                        108, ENTRY_HEIGHT - 2,
                        entry,
                        this::onEnchantmentSelected
                );
                enchantmentButtons.add(btn);
                addRenderableWidget(btn);
            }
        }

        updateButtonStates();
    }

    private void onEnchantmentSelected(MasteryEnchanterMenu.EnchantmentEntry entry) {
        this.selectedEnchantId = entry.enchantId();
        this.selectedLevel = 1; // Reset to level 1 when selecting new enchant

        // Highlight selected
        for (EnchantmentButton btn : enchantmentButtons) {
            btn.setSelected(btn.getEntry().enchantId().equals(selectedEnchantId));
        }

        updateButtonStates();
    }

    private void adjustLevel(int delta) {
        if (selectedEnchantId == null) return;

        // Find mastery level for selected enchant
        int maxLevel = menu.getAvailableEnchantments().stream()
                .filter(e -> e.enchantId().equals(selectedEnchantId))
                .mapToInt(MasteryEnchanterMenu.EnchantmentEntry::masteryLevel)
                .findFirst()
                .orElse(1);

        selectedLevel = Math.max(1, Math.min(maxLevel, selectedLevel + delta));
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean canApply = selectedEnchantId != null && selectedLevel > 0 &&
                !menu.getInputItem().isEmpty();

        // Check if selected enchant is applicable
        if (selectedEnchantId != null) {
            boolean applicable = menu.getAvailableEnchantments().stream()
                    .filter(e -> e.enchantId().equals(selectedEnchantId))
                    .anyMatch(e -> e.applicable() && !e.hasConflict());
            canApply = canApply && applicable;
        }

        applyButton.active = canApply;
        levelUpButton.active = selectedEnchantId != null;
        levelDownButton.active = selectedEnchantId != null && selectedLevel > 1;
    }

    private void onApplyClicked() {
        if (selectedEnchantId == null || selectedLevel <= 0) return;

        // Send packet to server
        ModNetworking.CHANNEL.send(new ApplyEnchantmentPacket(selectedEnchantId, selectedLevel), PacketDistributor.SERVER.noArg());

        // Reset selection
        selectedEnchantId = null;
        selectedLevel = 1;

        // Rebuild list after short delay (will update when container changes)
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Handle scrolling in enchantment list area
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 116 &&
                mouseY >= topPos + 17 && mouseY < topPos + 17 + VISIBLE_ENTRIES * ENTRY_HEIGHT) {

            int maxScroll = Math.max(0, menu.getAvailableEnchantments().size() - VISIBLE_ENTRIES);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) scrollY));
            rebuildEnchantmentList();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Refresh list when container changes
        rebuildEnchantmentList();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Draw background
        guiGraphics.blit(BACKGROUND_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Draw selected level and cost
        if (selectedEnchantId != null) {
            String levelText = "Level: " + RomanNumerals.toRoman(selectedLevel);
            int cost = ProgressionMath.applyCostLevels(selectedLevel);
            String costText = "Cost: " + cost + " levels";

            int xpLevels = Minecraft.getInstance().player != null ?
                    Minecraft.getInstance().player.experienceLevel : 0;
            int costColor = xpLevels >= cost ? 0x00FF00 : 0xFF0000;

            guiGraphics.drawString(font, levelText, leftPos + 120, topPos + 22, 0xFFFFFF);
            guiGraphics.drawString(font, costText, leftPos + 8, topPos + 75, costColor);
        }

        // Scroll indicator
        int totalEntries = menu.getAvailableEnchantments().size();
        if (totalEntries > VISIBLE_ENTRIES) {
            String scrollText = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + VISIBLE_ENTRIES, totalEntries) +
                    "/" + totalEntries;
            guiGraphics.drawString(font, scrollText, leftPos + 8, topPos + 8, 0x808080);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't draw default title/inventory labels
    }

    /**
     * Button representing an enchantment in the list.
     */
    private static class EnchantmentButton extends Button {
        private final MasteryEnchanterMenu.EnchantmentEntry entry;
        private boolean selected = false;

        public EnchantmentButton(int x, int y, int width, int height,
                                 MasteryEnchanterMenu.EnchantmentEntry entry,
                                 java.util.function.Consumer<MasteryEnchanterMenu.EnchantmentEntry> onPress) {
            super(x, y, width, height,
                    Component.literal(formatEnchantName(entry)),
                    btn -> onPress.accept(entry),
                    DEFAULT_NARRATION);
            this.entry = entry;
            this.active = entry.applicable() && !entry.hasConflict();
        }

        private static String formatEnchantName(MasteryEnchanterMenu.EnchantmentEntry entry) {
            // Simple format: enchant name + max level
            String name = entry.enchantId().getPath();
            // Convert snake_case to Title Case
            name = name.replace("_", " ");
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            return name + " (" + RomanNumerals.toRoman(entry.masteryLevel()) + ")";
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public MasteryEnchanterMenu.EnchantmentEntry getEntry() {
            return entry;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Custom rendering with selection highlight
            int bgColor = selected ? 0xFF4080FF : (isHovered ? 0xFF606060 : 0xFF404040);
            if (!active) bgColor = 0xFF303030;

            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            int textColor = active ? (entry.hasConflict() ? 0xFFFF00 : 0xFFFFFF) : 0x808080;
            guiGraphics.drawString(Minecraft.getInstance().font,
                    getMessage(), getX() + 4, getY() + 5, textColor);
        }
    }
}
