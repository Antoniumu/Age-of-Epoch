package com.robertx22.age_of_exile;

import com.robertx22.age_of_exile.mmorpg.init.ClientInit;
import com.robertx22.age_of_exile.mmorpg.init.CommonSetup;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.PlayerInteractEvents;
import net.fabricmc.fabric.api.object.builder.v1.registry.FabricRegistryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Keyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class AoEMod implements ModInitializer {
    public static final String MODID = "age_of_exile";
    public static final Logger LOG = LoggerFactory.getLogger(MODID);

    // Key bindings
    public static final KeyBinding OPEN_QUEST_LOG_KEY = new KeyBinding(
            "key.age_of_exile.open_quest",
            GLFW.GLFW_KEY_Q,
            "category.age_of_exile",
            "desc.age_of_exile.open_quest"
    );

    @Override
    public void onInitialize() {
        LOG.info("Age of Exile initializing for Minecraft 26.3 (Fabric)");

        // Register key bindings
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindingHelper.isKeyPressed(OPEN_QUEST_LOG_KEY, Minecraft.getInstance().getWindow().getHandle())) {
                // Open quest log
            }
        });

        // Register common setup
        CommonSetup.register();

        // Initialize core features
        initCoreFeatures();

        LOG.info("Age of Exile initialized successfully");
    }

    private void initCoreFeatures() {
        // Placeholder for core initialization
        // This would include:
        // - Registry initialization (items, blocks, entities)
        // - Spell system setup
        // - Leveling/progression system
        // - Gear/rarity system
        // - Dungeon generation
    }
}
