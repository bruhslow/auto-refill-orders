package com.example.autofillorders.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ItemPickerScreen extends Screen {
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int ITEMS_PER_PAGE = COLS * ROWS; // 54 items

    private static final List<Item> ALL_ITEMS = new ArrayList<>();
    static {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                ALL_ITEMS.add(item);
            }
        }
        ALL_ITEMS.sort((a, b) -> {
            String nameA = a.getName(new ItemStack(a)).getString();
            String nameB = b.getName(new ItemStack(b)).getString();
            return nameA.compareToIgnoreCase(nameB);
        });
    }

    private final Screen parent;
    private final Consumer<Item> onSelect;
    private final List<Item> filteredItems = new ArrayList<>();

    private EditBox searchBox;
    private Button prevBtn;
    private Button nextBtn;
    private int currentPage = 0;

    public ItemPickerScreen(Screen parent, Consumer<Item> onSelect) {
        super(Component.literal("Select Item"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.filteredItems.addAll(ALL_ITEMS);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // Search box at top
        this.searchBox = new EditBox(this.font, centerX - 100, 38, 200, 20, Component.literal("Search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(this::updateFilter);
        this.addRenderableWidget(this.searchBox);

        // Page buttons below the 6-row grid (Y = 194)
        this.prevBtn = Button.builder(Component.literal("< Prev"), btn -> prevPage())
                .bounds(centerX - 90, 206, 55, 20).build();
        this.addRenderableWidget(this.prevBtn);

        this.nextBtn = Button.builder(Component.literal("Next >"), btn -> nextPage())
                .bounds(centerX + 35, 206, 55, 20).build();
        this.addRenderableWidget(this.nextBtn);

        // Cancel / Back button
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> onClose())
                .bounds(centerX - 60, this.height - 28, 120, 20).build());

        updatePageButtons();
    }

    private void updateFilter(String query) {
        this.filteredItems.clear();
        String clean = (query == null) ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty()) {
            this.filteredItems.addAll(ALL_ITEMS);
        } else {
            for (Item item : ALL_ITEMS) {
                String name = item.getName(new ItemStack(item)).getString().toLowerCase(Locale.ROOT);
                String id = ItemHelper.getItemId(item).toLowerCase(Locale.ROOT);
                if (name.contains(clean) || id.contains(clean)) {
                    this.filteredItems.add(item);
                }
            }
        }
        this.currentPage = 0;
        updatePageButtons();
    }

    private int getMaxPages() {
        if (this.filteredItems.isEmpty()) return 1;
        return (this.filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
    }

    private void prevPage() {
        if (this.currentPage > 0) {
            this.currentPage--;
            updatePageButtons();
        }
    }

    private void nextPage() {
        if (this.currentPage < getMaxPages() - 1) {
            this.currentPage++;
            updatePageButtons();
        }
    }

    private void updatePageButtons() {
        int max = getMaxPages();
        if (this.prevBtn != null) {
            this.prevBtn.active = (this.currentPage > 0);
        }
        if (this.nextBtn != null) {
            this.nextBtn.active = (this.currentPage < max - 1);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (super.mouseClicked(event, isDoubleClick)) {
            return true;
        }

        if (event.button() == 0) { // Left click
            Item clicked = getItemAt(event.x(), event.y());
            if (clicked != null) {
                selectItem(clicked);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            prevPage();
            return true;
        } else if (verticalAmount < 0) {
            nextPage();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private Item getItemAt(double mouseX, double mouseY) {
        int centerX = this.width / 2;
        int gridStartX = centerX - 90;
        int gridStartY = 64;

        if (mouseX < gridStartX || mouseX >= gridStartX + 180 || mouseY < gridStartY || mouseY >= gridStartY + 120) {
            return null;
        }

        int col = (int) ((mouseX - gridStartX) / 20);
        int row = (int) ((mouseY - gridStartY) / 20);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
            return null;
        }

        int index = this.currentPage * ITEMS_PER_PAGE + (row * COLS + col);
        if (index >= 0 && index < this.filteredItems.size()) {
            return this.filteredItems.get(index);
        }
        return null;
    }

    private void selectItem(Item item) {
        if (this.onSelect != null) {
            this.onSelect.accept(item);
        }
        if (this.minecraft != null) {
            if (this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            }
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int gridStartX = centerX - 90;
        int gridStartY = 64;

        // Title and header
        extractor.centeredText(this.font, Component.literal("§6§lSelect Minecraft Item"), centerX, 12, 0xFFFFFF);
        extractor.centeredText(this.font, Component.literal("§7Type to search and click an item to select"), centerX, 25, 0xAAAAAA);

        // Render current page items
        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        int count = Math.min(ITEMS_PER_PAGE, this.filteredItems.size() - startIndex);

        ItemStack hoveredStack = null;
        Item hoveredItem = getItemAt(mouseX, mouseY);

        for (int i = 0; i < count; i++) {
            Item item = this.filteredItems.get(startIndex + i);
            int col = i % COLS;
            int row = i / COLS;
            int x = gridStartX + col * 20;
            int y = gridStartY + row * 20;

            ItemStack stack = new ItemStack(item);
            extractor.item(stack, x + 2, y + 2);

            if (item == hoveredItem) {
                hoveredStack = stack;
            }
        }

        // Display hovered item name or page info
        if (hoveredItem != null && hoveredStack != null) {
            String name = hoveredItem.getName(hoveredStack).getString();
            String id = ItemHelper.getItemId(hoveredItem);
            extractor.centeredText(this.font, Component.literal("§e" + name + " §8(" + id + ")"), centerX, 190, 0xFFFFFF);
            extractor.setTooltipForNextFrame(this.font, hoveredStack, mouseX, mouseY);
        } else if (this.filteredItems.isEmpty()) {
            extractor.centeredText(this.font, Component.literal("§cNo items found matching search!"), centerX, 110, 0xFF5555);
        }

        // Page indicator
        int totalPages = getMaxPages();
        extractor.centeredText(this.font, Component.literal("§7" + (this.currentPage + 1) + " / " + totalPages), centerX, 212, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }
}

