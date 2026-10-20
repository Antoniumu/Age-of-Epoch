package com.robertx22.forge_compat.fml.client.registry;

import net.minecraft.client.KeyMapping;

public class ClientRegistry {
    public static void registerKeyBinding(KeyMapping key) {
        KeyMapping.register(key);
    }
}
