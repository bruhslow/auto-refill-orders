package com.example.autofillorders.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class KeyCombo {
    private static final Map<String, Integer> NAME_TO_KEY = new HashMap<>();
    private static final Map<Integer, String> KEY_TO_NAME = new HashMap<>();

    static {
        registerKey("TAB", GLFW.GLFW_KEY_TAB);
        registerKey("SPACE", GLFW.GLFW_KEY_SPACE);
        registerKey("ENTER", GLFW.GLFW_KEY_ENTER);
        registerKey("RETURN", GLFW.GLFW_KEY_ENTER);
        registerKey("BACKSPACE", GLFW.GLFW_KEY_BACKSPACE);
        registerKey("ESCAPE", GLFW.GLFW_KEY_ESCAPE);
        registerKey("ESC", GLFW.GLFW_KEY_ESCAPE);
        registerKey("CAPSLOCK", GLFW.GLFW_KEY_CAPS_LOCK);
        registerKey("CAPS", GLFW.GLFW_KEY_CAPS_LOCK);

        registerKey("UP", GLFW.GLFW_KEY_UP);
        registerKey("DOWN", GLFW.GLFW_KEY_DOWN);
        registerKey("LEFT", GLFW.GLFW_KEY_LEFT);
        registerKey("RIGHT", GLFW.GLFW_KEY_RIGHT);

        registerKey("INSERT", GLFW.GLFW_KEY_INSERT);
        registerKey("DELETE", GLFW.GLFW_KEY_DELETE);
        registerKey("HOME", GLFW.GLFW_KEY_HOME);
        registerKey("END", GLFW.GLFW_KEY_END);
        registerKey("PAGEUP", GLFW.GLFW_KEY_PAGE_UP);
        registerKey("PAGEDOWN", GLFW.GLFW_KEY_PAGE_DOWN);

        // Letters A-Z
        for (int i = 0; i < 26; i++) {
            char c = (char) ('A' + i);
            registerKey(String.valueOf(c), GLFW.GLFW_KEY_A + i);
        }

        // Numbers 0-9
        for (int i = 0; i < 10; i++) {
            char c = (char) ('0' + i);
            registerKey(String.valueOf(c), GLFW.GLFW_KEY_0 + i);
            registerKey("NUM" + c, GLFW.GLFW_KEY_KP_0 + i);
        }

        // Function keys F1-F24
        for (int i = 1; i <= 24; i++) {
            registerKey("F" + i, GLFW.GLFW_KEY_F1 + (i - 1));
        }

        // Punctuation
        registerKey("GRAVE", GLFW.GLFW_KEY_GRAVE_ACCENT);
        registerKey("TILDE", GLFW.GLFW_KEY_GRAVE_ACCENT);
        registerKey("MINUS", GLFW.GLFW_KEY_MINUS);
        registerKey("EQUAL", GLFW.GLFW_KEY_EQUAL);
        registerKey("EQUALS", GLFW.GLFW_KEY_EQUAL);
        registerKey("LBRACKET", GLFW.GLFW_KEY_LEFT_BRACKET);
        registerKey("RBRACKET", GLFW.GLFW_KEY_RIGHT_BRACKET);
        registerKey("BACKSLASH", GLFW.GLFW_KEY_BACKSLASH);
        registerKey("SEMICOLON", GLFW.GLFW_KEY_SEMICOLON);
        registerKey("APOSTROPHE", GLFW.GLFW_KEY_APOSTROPHE);
        registerKey("QUOTE", GLFW.GLFW_KEY_APOSTROPHE);
        registerKey("COMMA", GLFW.GLFW_KEY_COMMA);
        registerKey("PERIOD", GLFW.GLFW_KEY_PERIOD);
        registerKey("SLASH", GLFW.GLFW_KEY_SLASH);
    }

    private static void registerKey(String name, int glfwCode) {
        NAME_TO_KEY.put(name.toUpperCase(Locale.ROOT), glfwCode);
        KEY_TO_NAME.putIfAbsent(glfwCode, name.toUpperCase(Locale.ROOT));
    }

    public static String getKeyName(int glfwCode) {
        if (glfwCode == GLFW.GLFW_KEY_LEFT_CONTROL || glfwCode == GLFW.GLFW_KEY_RIGHT_CONTROL) return "CTRL";
        if (glfwCode == GLFW.GLFW_KEY_LEFT_SHIFT || glfwCode == GLFW.GLFW_KEY_RIGHT_SHIFT) return "SHIFT";
        if (glfwCode == GLFW.GLFW_KEY_LEFT_ALT || glfwCode == GLFW.GLFW_KEY_RIGHT_ALT) return "ALT";
        String registered = KEY_TO_NAME.get(glfwCode);
        if (registered != null) return registered;
        try {
            return InputConstants.Type.KEYSYM.getOrCreate(glfwCode).getDisplayName().getString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "KEY_" + glfwCode;
        }
    }

    public static String format(String input) {
        if (input == null || input.isBlank()) return "None";
        String[] parts = input.split("[+\\s,]+");
        List<String> valid = new ArrayList<>();
        for (String p : parts) {
            String clean = p.trim().toUpperCase(Locale.ROOT);
            if (!clean.isEmpty()) {
                if (clean.equals("CONTROL") || clean.equals("CTRL") || clean.equals("LCTRL") || clean.equals("RCTRL")
                        || clean.equals("TRL") || clean.equals("CTL") || clean.equals("CTR")) {
                    clean = "CTRL";
                } else if (clean.equals("SHIFT") || clean.equals("LSHIFT") || clean.equals("RSHIFT") || clean.equals("SHFT")) {
                    clean = "SHIFT";
                } else if (clean.equals("ALT") || clean.equals("LALT") || clean.equals("RALT") || clean.equals("OPTION")) {
                    clean = "ALT";
                }
                if (!valid.contains(clean)) {
                    valid.add(clean);
                }
            }
        }
        if (valid.isEmpty()) return "None";

        valid.sort((a, b) -> {
            int scoreA = getSortOrder(a);
            int scoreB = getSortOrder(b);
            if (scoreA != scoreB) {
                return Integer.compare(scoreA, scoreB);
            }
            return a.compareTo(b);
        });

        return String.join(" + ", valid);
    }

    private static int getSortOrder(String key) {
        if (key.equals("CTRL")) return 1;
        if (key.equals("ALT")) return 2;
        if (key.equals("SHIFT")) return 3;
        return 10;
    }

    public static boolean isComboPressed(String combo) {
        if (combo == null || combo.isBlank() || combo.equalsIgnoreCase("None")) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) return false;

        String[] parts = combo.split("[+\\s,]+");
        if (parts.length == 0) return false;

        for (String part : parts) {
            String token = part.trim().toUpperCase(Locale.ROOT);
            if (token.isEmpty()) continue;

            if (token.equals("CTRL") || token.equals("CONTROL") || token.equals("TRL") || token.equals("CTL")) {
                boolean left = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL);
                boolean right = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
                if (!left && !right) return false;
            } else if (token.equals("SHIFT") || token.equals("SHFT")) {
                boolean left = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
                boolean right = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
                if (!left && !right) return false;
            } else if (token.equals("ALT") || token.equals("OPTION")) {
                boolean left = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT);
                boolean right = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
                if (!left && !right) return false;
            } else {
                Integer code = NAME_TO_KEY.get(token);
                if (code == null) {
                    try {
                        InputConstants.Key k = InputConstants.getKey("key.keyboard." + token.toLowerCase(Locale.ROOT));
                        code = k.getValue();
                    } catch (Exception ignored) {}
                }
                if (code == null || code <= 0) {
                    return false;
                }
                if (!InputConstants.isKeyDown(client.getWindow(), code)) {
                    return false;
                }
            }
        }
        return true;
    }
}

