package com.example.autofillorders.client;

import com.example.autofillorders.config.AutofillConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderAutoFiller {
    public enum State {
        IDLE,
        WAIT_FOR_ORDERS_GUI,
        HANDLING_ORDERS_LIST,
        WAIT_FOR_DELIVER_GUI,
        FILLING_DELIVERY_GUI,
        WAIT_AFTER_DELIVERY
    }

    private static State state = State.IDLE;
    private static int ticksWaited = 0;
    private static int timeoutTicks = 0;
    private static int totalDeliveredCycles = 0;
    private static int lastClosedContainerId = -1;

    public static State getState() {
        return state;
    }

    public static boolean isActive() {
        return state != State.IDLE;
    }

    /**
     * Toggles the autofiller on or off.
     */
    public static void toggle(Minecraft client) {
        if (isActive()) {
            stop(client, "§c[Autofill] Stopped by user.", true);
        } else {
            start(client);
        }
    }

    /**
     * Starts the auto-fill process.
     */
    public static void start(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            return;
        }

        AutofillConfig config = AutofillConfig.get();
        if (!config.enabled) {
            if (config.chatMessages) {
                client.player.sendSystemMessage(Component.literal("§c[Autofill] Mod is currently disabled in config. Type /autofillorders to enable."));
            }
            return;
        }

        Item targetItem = ItemHelper.getItem(config.targetItemId);
        int invCount = ItemHelper.countInInventory(client.player, targetItem);
        if (invCount == 0) {
            String name = ItemHelper.getItemDisplayName(targetItem).getString();
            if (config.chatMessages) {
                client.player.sendSystemMessage(Component.literal("§c[Autofill] No " + name + " found in your inventory!"));
            }
            if (config.soundAlert) {
                client.player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.8f);
            }
            return;
        }

        String itemName = ItemHelper.getItemDisplayName(targetItem).getString();
        if (config.chatMessages) {
            client.player.sendSystemMessage(Component.literal("§a[Autofill] Started filling orders for: §f" + itemName + " §7(x" + invCount + " in inventory)"));
        }
        if (config.soundAlert) {
            client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        totalDeliveredCycles = 0;
        ticksWaited = 0;
        timeoutTicks = 200;
        lastClosedContainerId = -1;

        // Check if an order or deliver GUI is ALREADY open
        Screen currentScreen = (client.gui != null) ? client.gui.screen() : null;
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            String title = currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);
            if (title.contains("deliver") || title.contains("deposit")) {
                state = State.FILLING_DELIVERY_GUI;
                return;
            } else if (title.contains("order")) {
                state = State.HANDLING_ORDERS_LIST;
                return;
            }
        }

        // Otherwise send the configured command
        sendCommand(client, config.command);
        state = State.WAIT_FOR_ORDERS_GUI;
    }

    /**
     * Stops the auto-filler.
     */
    public static void stop(Minecraft client, String message, boolean playSound) {
        state = State.IDLE;
        ticksWaited = 0;
        timeoutTicks = 0;
        lastClosedContainerId = -1;

        if (client.player != null) {
            if (AutofillConfig.get().chatMessages && message != null && !message.isEmpty()) {
                client.player.sendSystemMessage(Component.literal(message));
            }
            if (playSound && AutofillConfig.get().soundAlert) {
                client.player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.9f);
            }
        }
    }

    public static void onTick(Minecraft client) {
        if (state == State.IDLE) {
            return;
        }

        if (client.player == null) {
            state = State.IDLE;
            return;
        }

        timeoutTicks--;
        if (timeoutTicks <= 0) {
            stop(client, "§c[Autofill] Operation timed out.", true);
            return;
        }

        AutofillConfig config = AutofillConfig.get();
        Item targetItem = ItemHelper.getItem(config.targetItemId);

        Screen currentScreen = (client.gui != null) ? client.gui.screen() : null;

        switch (state) {
            case WAIT_FOR_ORDERS_GUI -> {
                if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
                    String title = currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);
                    if (title.contains("deliver") || title.contains("deposit")) {
                        state = State.FILLING_DELIVERY_GUI;
                        ticksWaited = 0;
                    } else if (title.contains("order")) {
                        state = State.HANDLING_ORDERS_LIST;
                        ticksWaited = 0;
                    }
                }
            }

            case HANDLING_ORDERS_LIST -> {
                if (!(currentScreen instanceof AbstractContainerScreen<?> containerScreen)) {
                    return;
                }
                String title = currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);
                if (title.contains("deliver") || title.contains("deposit")) {
                    state = State.FILLING_DELIVERY_GUI;
                    ticksWaited = 0;
                    return;
                }

                AbstractContainerMenu menu = containerScreen.getMenu();
                if (!hasContainerLoadedItems(menu, client.player)) {
                    return;
                }

                Slot orderSlot = findOrderSlot(menu, client.player, targetItem);
                if (orderSlot != null && client.gameMode != null) {
                    client.gameMode.handleContainerInput(menu.containerId, orderSlot.index, 0, ContainerInput.PICKUP, client.player);
                    state = State.WAIT_FOR_DELIVER_GUI;
                    ticksWaited = 0;
                    timeoutTicks = 160;
                }
            }

            case WAIT_FOR_DELIVER_GUI -> {
                if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
                    String title = currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);
                    if (title.contains("deliver") || title.contains("deposit")) {
                        state = State.FILLING_DELIVERY_GUI;
                        ticksWaited = 0;
                    }
                }
            }

            case FILLING_DELIVERY_GUI -> {
                if (!(currentScreen instanceof AbstractContainerScreen<?> containerScreen)) {
                    return;
                }

                AbstractContainerMenu menu = containerScreen.getMenu();
                int containerId = menu.containerId;

                // Let the container settle for 2 ticks after opening to ensure slot sync
                ticksWaited++;
                if (ticksWaited < 2) {
                    return;
                }

                // Find all player slots containing targetItem
                List<Slot> playerItemSlots = new ArrayList<>();
                for (Slot slot : menu.slots) {
                    if (slot.container == client.player.getInventory()) {
                        ItemStack stack = slot.getItem();
                        if (!stack.isEmpty() && stack.is(targetItem)) {
                            playerItemSlots.add(slot);
                        }
                    }
                }

                // If player has NO items of target type in inventory:
                if (playerItemSlots.isEmpty()) {
                    forceCloseCurrentScreen(client);
                    finishAll(client, targetItem);
                    return;
                }

                // Count available empty or stackable space in the top container
                int availableTopSlots = 0;
                for (Slot slot : menu.slots) {
                    if (slot.container != client.player.getInventory()) {
                        ItemStack stack = slot.getItem();
                        if (stack.isEmpty() || (stack.is(targetItem) && stack.getCount() < stack.getMaxStackSize())) {
                            availableTopSlots++;
                        }
                    }
                }

                // If top container is full:
                if (availableTopSlots <= 0) {
                    deliverAndClose(client, containerId);
                    return;
                }

                int toTransfer = Math.min(availableTopSlots, playerItemSlots.size());

                if (config.delayTicks <= 0) {
                    // Instant: transfer all available stacks in this single tick!
                    if (client.gameMode != null) {
                        for (int i = 0; i < toTransfer; i++) {
                            Slot slot = playerItemSlots.get(i);
                            client.gameMode.handleContainerInput(containerId, slot.index, 0, ContainerInput.QUICK_MOVE, client.player);
                        }
                    }
                    deliverAndClose(client, containerId);
                } else if (config.delayTicks == 1) {
                    // 4stack/per tick
                    int batch = Math.min(4, toTransfer);
                    if (client.gameMode != null) {
                        for (int i = 0; i < batch; i++) {
                            Slot slot = playerItemSlots.get(i);
                            client.gameMode.handleContainerInput(containerId, slot.index, 0, ContainerInput.QUICK_MOVE, client.player);
                        }
                    }
                    if (batch >= toTransfer) {
                        deliverAndClose(client, containerId);
                    }
                } else {
                    // 2 tick or 3 tick delay
                    ticksWaited++;
                    if (ticksWaited >= config.delayTicks) {
                        int batch = (config.delayTicks == 2) ? Math.min(2, toTransfer) : Math.min(1, toTransfer);
                        if (client.gameMode != null) {
                            for (int i = 0; i < batch; i++) {
                                Slot slot = playerItemSlots.get(i);
                                client.gameMode.handleContainerInput(containerId, slot.index, 0, ContainerInput.QUICK_MOVE, client.player);
                            }
                        }
                        ticksWaited = 0;
                        if (batch >= toTransfer) {
                            deliverAndClose(client, containerId);
                        }
                    }
                }
            }

            case WAIT_AFTER_DELIVERY -> {
                ticksWaited++;

                // Give the server at least 5 ticks to close the previous container
                if (ticksWaited < 5) {
                    return;
                }

                // Check if delivery or orders screen has re-opened
                if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
                    AbstractContainerMenu menu = containerScreen.getMenu();
                    // Ensure this is a newly opened container, not the old one closing
                    if (lastClosedContainerId == -1 || menu.containerId != lastClosedContainerId) {
                        String title = currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);
                        if (title.contains("deliver") || title.contains("deposit")) {
                            // Check if player still has items to deliver
                            if (ItemHelper.countInInventory(client.player, targetItem) == 0) {
                                forceCloseCurrentScreen(client);
                                finishAll(client, targetItem);
                                return;
                            }
                            state = State.FILLING_DELIVERY_GUI;
                            ticksWaited = 0;
                            timeoutTicks = 160;
                            return;
                        } else if (title.contains("order")) {
                            if (ItemHelper.countInInventory(client.player, targetItem) == 0) {
                                forceCloseCurrentScreen(client);
                                finishAll(client, targetItem);
                                return;
                            }
                            state = State.HANDLING_ORDERS_LIST;
                            ticksWaited = 0;
                            timeoutTicks = 160;
                            return;
                        }
                    }
                }

                // If after ~35 ticks (1.75s) the server hasn't auto-reopened the GUI:
                if (ticksWaited >= 35 && currentScreen == null) {
                    if (ItemHelper.countInInventory(client.player, targetItem) == 0) {
                        finishAll(client, targetItem);
                        return;
                    }
                    // Still have items: send the command again to continue the loop!
                    sendCommand(client, config.command);
                    state = State.WAIT_FOR_ORDERS_GUI;
                    ticksWaited = 0;
                    timeoutTicks = 160;
                }
            }
        }
    }

    private static void deliverAndClose(Minecraft client, int containerId) {
        totalDeliveredCycles++;
        lastClosedContainerId = containerId;
        forceCloseCurrentScreen(client);
        state = State.WAIT_AFTER_DELIVERY;
        ticksWaited = 0;
        timeoutTicks = 160;
    }

    private static void finishAll(Minecraft client, Item targetItem) {
        String itemName = ItemHelper.getItemDisplayName(targetItem).getString();
        stop(client, "§a§l[Autofill] Successfully delivered all " + itemName + "s! Finished " + totalDeliveredCycles + " delivery cycle(s).", false);
        if (AutofillConfig.get().soundAlert && client.player != null) {
            client.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.2f);
        }
    }

    public static void forceCloseCurrentScreen(Minecraft client) {
        if (client == null) return;
        Screen screen = (client.gui != null) ? client.gui.screen() : null;
        if (screen != null) {
            screen.onClose();
        }
        if (client.player != null) {
            client.player.closeContainer();
        }
        if (client.gui != null) {
            client.gui.setScreen(null);
        }
        if (client.mouseHandler != null && !client.mouseHandler.isMouseGrabbed()) {
            client.mouseHandler.grabMouse();
        }
    }

    private static void sendCommand(Minecraft client, String cmd) {
        if (client.getConnection() == null || cmd == null || cmd.isBlank()) return;
        String clean = cmd.trim();
        if (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        client.getConnection().sendCommand(clean);
    }

    private static boolean hasContainerLoadedItems(AbstractContainerMenu menu, Player player) {
        if (menu == null || menu.slots == null) return false;
        for (Slot slot : menu.slots) {
            if (slot.container != player.getInventory() && !slot.getItem().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static Slot findOrderSlot(AbstractContainerMenu menu, Player player, Item targetItem) {
        String targetItemName = targetItem.getName(new ItemStack(targetItem)).getString().toLowerCase(Locale.ROOT);
        Slot fallback = null;

        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            // Direct match by item type
            if (stack.is(targetItem)) {
                return slot;
            }

            // Match by hover name or lore containing target item name
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(targetItemName)) {
                fallback = slot;
            }
        }
        return fallback;
    }
}
