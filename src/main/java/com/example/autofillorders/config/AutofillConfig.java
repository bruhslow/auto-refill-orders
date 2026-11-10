package com.example.autofillorders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class AutofillConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "autofillorders.json");

    public boolean enabled = true;
    public String command = "orders macalo";
    public String targetItemId = "minecraft:bone";
    public String keyCombo = "V";
    public int delayTicks = 0; // Default 0 (Instant / Maximum Speed)
    public boolean autoClose = true;
    public boolean soundAlert = true;
    public boolean chatMessages = true;

    private static AutofillConfig INSTANCE;

    public static AutofillConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static AutofillConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                AutofillConfig config = GSON.fromJson(reader, AutofillConfig.class);
                if (config != null) {
                    INSTANCE = config;
                    return config;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        AutofillConfig newConfig = new AutofillConfig();
        newConfig.save();
        INSTANCE = newConfig;
        return newConfig;
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
