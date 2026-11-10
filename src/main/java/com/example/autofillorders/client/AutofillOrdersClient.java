package com.example.autofillorders.client;

import com.example.autofillorders.config.AutofillConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public class AutofillOrdersClient implements ClientModInitializer {
    private static boolean wasKeyPressed = false;

    @Override
    public void onInitializeClient() {
        AutofillConfig.load();

        // Register client command: /autofillorders
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var root = ClientCommands.literal("autofillorders")
                .executes(context -> {
                    openGui(context.getSource().getClient());
                    return 1;
                })
                .then(ClientCommands.literal("gui")
                    .executes(context -> {
                        openGui(context.getSource().getClient());
                        return 1;
                    }))
                .then(ClientCommands.literal("config")
                    .executes(context -> {
                        openGui(context.getSource().getClient());
                        return 1;
                    }))
                .then(ClientCommands.literal("toggle")
                    .executes(context -> {
                        OrderAutoFiller.toggle(context.getSource().getClient());
                        return 1;
                    }))
                .then(ClientCommands.literal("status")
                    .executes(context -> {
                        printStatus(context.getSource().getClient());
                        return 1;
                    }))
                .then(ClientCommands.literal("command")
                    .then(ClientCommands.argument("cmd", StringArgumentType.greedyString())
                        .executes(context -> {
                            String cmd = StringArgumentType.getString(context, "cmd").trim();
                            AutofillConfig cfg = AutofillConfig.get();
                            cfg.command = cmd;
                            cfg.save();
                            context.getSource().sendFeedback(Component.literal("§a[Autofill] Order command set to: §e" + cmd));
                            return 1;
                        })))
                .then(ClientCommands.literal("item")
                    .then(ClientCommands.argument("itemId", StringArgumentType.string())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "itemId").trim();
                            Item item = ItemHelper.getItem(id);
                            String resolvedId = ItemHelper.getItemId(item);
                            AutofillConfig cfg = AutofillConfig.get();
                            cfg.targetItemId = resolvedId;
                            cfg.save();
                            context.getSource().sendFeedback(Component.literal("§a[Autofill] Target item set to: §e" + ItemHelper.getItemDisplayName(item).getString() + " §7(" + resolvedId + ")"));
                            return 1;
                        })))
                .then(ClientCommands.literal("delay")
                    .then(ClientCommands.argument("ticks", IntegerArgumentType.integer(0, 20))
                        .executes(context -> {
                            int ticks = IntegerArgumentType.getInteger(context, "ticks");
                            AutofillConfig cfg = AutofillConfig.get();
                            cfg.delayTicks = ticks;
                            cfg.save();
                            context.getSource().sendFeedback(Component.literal("§a[Autofill] Click delay set to: §e" + ticks + " ticks (" + (ticks * 50) + "ms)"));
                            return 1;
                        })))
                .then(ClientCommands.literal("key")
                    .executes(context -> {
                        openGui(context.getSource().getClient());
                        return 1;
                    })
                    .then(ClientCommands.argument("combo", StringArgumentType.greedyString())
                        .executes(context -> {
                            String raw = StringArgumentType.getString(context, "combo");
                            String formatted = KeyCombo.format(raw);
                            AutofillConfig cfg = AutofillConfig.get();
                            cfg.keyCombo = formatted;
                            cfg.save();
                            context.getSource().sendFeedback(Component.literal("§a[Autofill] Shortcut set to: §e" + formatted));
                            return 1;
                        })));

            dispatcher.register(root);
        });

        // Register tick event for key detection and state machine
        ClientTickEvents.END_CLIENT_TICK.register(AutofillOrdersClient::onTick);
    }

    private static void openGui(Minecraft client) {
        client.schedule(() -> {
            if (client.gui != null) {
                var current = client.gui.screen();
                var parent = (current instanceof ChatScreen) ? null : current;
                client.gui.setScreen(new AutofillConfigScreen(parent));
            }
        });
    }

    private static void onTick(Minecraft client) {
        if (client.player == null) return;

        AutofillConfig config = AutofillConfig.get();

        // Determine if shortcut key was pressed
        var currentScreen = (client.gui != null) ? client.gui.screen() : null;
        boolean inTypingScreen = (currentScreen instanceof ChatScreen || currentScreen instanceof AutofillConfigScreen);

        if (!inTypingScreen) {
            boolean isPressed = config.enabled && KeyCombo.isComboPressed(config.keyCombo);
            if (isPressed && !wasKeyPressed) {
                OrderAutoFiller.toggle(client);
            }
            wasKeyPressed = isPressed;
        } else {
            wasKeyPressed = false;
        }

        // Advance auto-filler state machine
        OrderAutoFiller.onTick(client);
    }

    private static void printStatus(Minecraft client) {
        if (client.player == null) return;
        AutofillConfig cfg = AutofillConfig.get();
        Item item = ItemHelper.getItem(cfg.targetItemId);

        client.player.sendSystemMessage(Component.literal("§8§m----------------------------------------"));
        client.player.sendSystemMessage(Component.literal("§6§lAutofill Orders §7- Settings"));
        client.player.sendSystemMessage(Component.literal("§7Status: " + (cfg.enabled ? "§aEnabled" : "§cDisabled")));
        client.player.sendSystemMessage(Component.literal("§7Running State: " + (OrderAutoFiller.isActive() ? "§aACTIVE (" + OrderAutoFiller.getState() + ")" : "§7IDLE")));
        client.player.sendSystemMessage(Component.literal("§7Command: §e/" + cfg.command));
        client.player.sendSystemMessage(Component.literal("§7Target Item: §b" + ItemHelper.getItemDisplayName(item).getString() + " §8(" + cfg.targetItemId + ")"));
        client.player.sendSystemMessage(Component.literal("§7Shortcut: §e" + cfg.keyCombo));
        client.player.sendSystemMessage(Component.literal("§7Click Delay: §e" + cfg.delayTicks + " ticks (" + (cfg.delayTicks * 50) + "ms)"));
        client.player.sendSystemMessage(Component.literal("§7Commands:"));
        client.player.sendSystemMessage(Component.literal("  §e/autofillorders §7- Open GUI"));
        client.player.sendSystemMessage(Component.literal("  §e/autofillorders toggle §7- Start/stop"));
        client.player.sendSystemMessage(Component.literal("  §e/autofillorders command <cmd> §7- Change command"));
        client.player.sendSystemMessage(Component.literal("  §e/autofillorders item <id> §7- Change target item"));
        client.player.sendSystemMessage(Component.literal("  §e/autofillorders key <combo> §7- Change shortcut"));
        client.player.sendSystemMessage(Component.literal("§8§m----------------------------------------"));
    }
}
