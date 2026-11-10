package com.example.autofillorders.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

public class ItemHelper {

    public static Item getItem(String id) {
        if (id == null || id.isBlank()) return Items.BONE;
        String clean = id.trim().toLowerCase(Locale.ROOT);
        if (!clean.contains(":")) {
            clean = "minecraft:" + clean;
        }
        try {
            Identifier identifier = Identifier.parse(clean);
            Item item = BuiltInRegistries.ITEM.getValue(identifier);
            if (item != null && item != Items.AIR) {
                return item;
            }
        } catch (Exception ignored) {}
        return Items.BONE;
    }

    public static String getItemId(Item item) {
        if (item == null) return "minecraft:air";
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return (id != null) ? id.toString() : "minecraft:air";
    }

    public static Component getItemDisplayName(Item item) {
        if (item == null) return Component.literal("Unknown Item");
        return item.getName(new ItemStack(item));
    }

    public static int countInInventory(Player player, Item targetItem) {
        if (player == null || targetItem == null) return 0;
        int count = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(targetItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}

