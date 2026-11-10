package com.example.autofillorders.client;

import com.example.autofillorders.config.AutofillConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AutofillConfigScreen extends Screen {
    private final Screen parent;

    private EditBox commandBox;
    private EditBox itemBox;
    private Button useHeldItemBtn;
    private Button keybindBtn;
    private Button delayBtn;
    private Button soundAlertBtn;
    private Button chatMessagesBtn;
    private Button enabledBtn;

    private boolean isRecording = false;
    private final Set<Integer> recordedKeys = new LinkedHashSet<>();

    public AutofillConfigScreen(Screen parent) {
        super(Component.literal("Autofill Orders Settings"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new AutofillConfigScreen(parent);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        AutofillConfig config = AutofillConfig.get();

        int startY = 46;

        // 1. Command Input Field
        this.commandBox = new EditBox(this.font, centerX - 120, startY, 240, 20, Component.literal("Command"));
        this.commandBox.setMaxLength(64);
        this.commandBox.setValue(config.command.startsWith("/") ? config.command : "/" + config.command);
        this.commandBox.setResponder(val -> {
            config.command = val.trim();
            config.save();
        });
        this.addRenderableWidget(this.commandBox);

        // 2. Item Input Field & Use Held Item Button
        int itemY = startY + 38;
        this.itemBox = new EditBox(this.font, centerX - 120, itemY, 195, 20, Component.literal("Target Item"));
        this.itemBox.setMaxLength(64);
        this.itemBox.setValue(config.targetItemId);
        this.itemBox.setResponder(val -> {
            config.targetItemId = val.trim();
            config.save();
        });
        this.addRenderableWidget(this.itemBox);

        this.useHeldItemBtn = Button.builder(
                Component.literal("🖐 Held"),
                btn -> selectHeldItem()
        ).bounds(centerX + 80, itemY, 40, 20).build();
        this.addRenderableWidget(this.useHeldItemBtn);

        // Item presets: Bone, Iron, and Browse All Items search picker
        int presetY = itemY + 24;
        this.addRenderableWidget(Button.builder(Component.literal("Bone"), btn -> setPreset("minecraft:bone"))
                .bounds(centerX - 120, presetY, 48, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Iron"), btn -> setPreset("minecraft:iron_ingot"))
                .bounds(centerX - 70, presetY, 48, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("🔍 Browse All Items..."), btn -> openItemPicker())
                .bounds(centerX - 20, presetY, 140, 18).build());

        // 3. Keybind Button
        int optionsY = presetY + 26;
        this.keybindBtn = Button.builder(
                getKeybindButtonText(),
                btn -> startRecording()
        ).bounds(centerX - 120, optionsY, 240, 20).build();
        this.addRenderableWidget(this.keybindBtn);

        // 4. Delay Button
        this.delayBtn = Button.builder(
                getDelayButtonText(),
                btn -> cycleDelay()
        ).bounds(centerX - 120, optionsY + 24, 240, 20).build();
        this.addRenderableWidget(this.delayBtn);

        // 5. Sound & Chat Toggles side by side
        this.soundAlertBtn = Button.builder(
                getSoundAlertButtonText(),
                btn -> toggleSoundAlert()
        ).bounds(centerX - 120, optionsY + 48, 118, 20).build();
        this.addRenderableWidget(this.soundAlertBtn);

        this.chatMessagesBtn = Button.builder(
                getChatMessagesButtonText(),
                btn -> toggleChatMessages()
        ).bounds(centerX + 2, optionsY + 48, 118, 20).build();
        this.addRenderableWidget(this.chatMessagesBtn);

        // 6. Mod Enabled toggle button (full width)
        this.enabledBtn = Button.builder(
                getEnabledButtonText(),
                btn -> toggleEnabled()
        ).bounds(centerX - 120, optionsY + 72, 240, 20).build();
        this.addRenderableWidget(this.enabledBtn);

        // Done button
        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                btn -> onClose()
        ).bounds(centerX - 80, this.height - 28, 160, 20).build());
    }

    private void selectHeldItem() {
        if (this.minecraft == null || this.minecraft.player == null) return;
        ItemStack held = this.minecraft.player.getMainHandItem();
        if (held.isEmpty()) {
            held = this.minecraft.player.getOffhandItem();
        }
        if (!held.isEmpty()) {
            String id = ItemHelper.getItemId(held.getItem());
            this.itemBox.setValue(id);
            AutofillConfig.get().targetItemId = id;
            AutofillConfig.get().save();
            this.minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        }
    }

    private void setPreset(String itemId) {
        this.itemBox.setValue(itemId);
        AutofillConfig.get().targetItemId = itemId;
        AutofillConfig.get().save();
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        }
    }

    private void openItemPicker() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new ItemPickerScreen(this, item -> {
                String id = ItemHelper.getItemId(item);
                this.itemBox.setValue(id);
                AutofillConfig.get().targetItemId = id;
                AutofillConfig.get().save();
            }));
        }
    }

    private void startRecording() {
        this.isRecording = true;
        this.recordedKeys.clear();
        this.keybindBtn.setMessage(Component.literal("§e> Press any key (e.g. V or CTRL+O)... <"));
    }

    private void finishRecording() {
        if (!this.isRecording) return;
        this.isRecording = false;

        AutofillConfig config = AutofillConfig.get();
        if (this.recordedKeys.isEmpty()) {
            config.keyCombo = "None";
        } else {
            List<String> keyNames = new ArrayList<>();
            for (int k : this.recordedKeys) {
                String name = KeyCombo.getKeyName(k);
                if (!keyNames.contains(name)) {
                    keyNames.add(name);
                }
            }
            config.keyCombo = KeyCombo.format(String.join(" + ", keyNames));
        }
        config.save();

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        }

        this.keybindBtn.setMessage(getKeybindButtonText());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.isRecording) {
            int key = event.key();

            if (key == GLFW.GLFW_KEY_ESCAPE) {
                this.isRecording = false;
                this.recordedKeys.clear();
                this.keybindBtn.setMessage(getKeybindButtonText());
                return true;
            }

            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                finishRecording();
                return true;
            }

            this.recordedKeys.add(key);

            boolean isModifier = (key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL
                    || key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT
                    || key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT);

            if (!isModifier) {
                finishRecording();
            } else {
                List<String> current = new ArrayList<>();
                for (int k : this.recordedKeys) {
                    current.add(KeyCombo.getKeyName(k));
                }
                this.keybindBtn.setMessage(Component.literal("§e> " + String.join(" + ", current) + " + [Press Key]... <"));
            }
            return true;
        }

        return super.keyPressed(event);
    }

    private Component getKeybindButtonText() {
        return Component.literal("§7Shortcut: §b[ " + AutofillConfig.get().keyCombo + " ] §8(Click to change)");
    }

    private Component getDelayButtonText() {
        int ticks = AutofillConfig.get().delayTicks;
        String desc;
        if (ticks == 0) {
            desc = "Instant / 0 tick";
        } else if (ticks == 1) {
            desc = "Fast / 1 tick";
        } else if (ticks == 2) {
            desc = "Safe / 2 tick";
        } else {
            desc = "Normal / 3 tick";
        }
        return Component.literal("§7Transfer Speed: §e" + desc);
    }

    private Component getSoundAlertButtonText() {
        return Component.literal("§7Sound: " + (AutofillConfig.get().soundAlert ? "§aON" : "§cOFF"));
    }

    private Component getEnabledButtonText() {
        return Component.literal("§7Mod: " + (AutofillConfig.get().enabled ? "§aEnabled" : "§cDisabled"));
    }

    private void cycleDelay() {
        AutofillConfig config = AutofillConfig.get();
        int[] delays = {0, 1, 2, 3};
        int next = 0;
        for (int i = 0; i < delays.length; i++) {
            if (delays[i] == config.delayTicks) {
                next = delays[(i + 1) % delays.length];
                break;
            }
        }
        config.delayTicks = next;
        config.save();
        this.delayBtn.setMessage(getDelayButtonText());
    }

    private Component getChatMessagesButtonText() {
        return Component.literal("§7Chat Msgs: " + (AutofillConfig.get().chatMessages ? "§aON" : "§cOFF"));
    }

    private void toggleSoundAlert() {
        AutofillConfig config = AutofillConfig.get();
        config.soundAlert = !config.soundAlert;
        config.save();
        this.soundAlertBtn.setMessage(getSoundAlertButtonText());
    }

    private void toggleChatMessages() {
        AutofillConfig config = AutofillConfig.get();
        config.chatMessages = !config.chatMessages;
        config.save();
        this.chatMessagesBtn.setMessage(getChatMessagesButtonText());
    }

    private void toggleEnabled() {
        AutofillConfig config = AutofillConfig.get();
        config.enabled = !config.enabled;
        config.save();
        this.enabledBtn.setMessage(getEnabledButtonText());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        // Title and header
        extractor.centeredText(this.font, Component.literal("§6§lAutofill Orders §7[26.x] §f- Settings"), centerX, 12, 0xFFFFFF);
        extractor.centeredText(this.font, Component.literal("§7Automatically fill orders until out of items or toggled off"), centerX, 25, 0x888888);

        // Labels above edit boxes
        extractor.text(this.font, Component.literal("§eOrder Command:"), centerX - 120, 36, 0xDDDDDD);
        extractor.text(this.font, Component.literal("§eTarget Item (ID or held):"), centerX - 120, 74, 0xDDDDDD);

        // Live item preview next to itemBox
        Item item = ItemHelper.getItem(AutofillConfig.get().targetItemId);
        ItemStack previewStack = new ItemStack(item != null ? item : Items.BONE);
        extractor.item(previewStack, centerX - 142, 86);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }
}

