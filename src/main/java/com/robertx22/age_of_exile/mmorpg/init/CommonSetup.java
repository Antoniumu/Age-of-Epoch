package com.robertx22.age_of_exile.mmorpg.init;

import com.robertx22.age_of_exile.mmorpg.MMORPG;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CommonSetup {

    public static void register() {
        // Register key bindings for client
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Key bindings handled via ClientInit
        });
    }
}
