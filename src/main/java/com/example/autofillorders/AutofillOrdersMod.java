package com.example.autofillorders;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutofillOrdersMod implements ModInitializer {
    public static final String MOD_ID = "autofillorders";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("AutofillOrders Mod initialized for Minecraft 26.2!");
    }
}

