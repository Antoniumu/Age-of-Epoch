package com.robertx22.forge_compat.fml.client.registry;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class ClientRegistry {
    public static void registerKeyBinding(KeyMapping key) {
        try {
            KeyMappingHelper.registerKeyMapping(key);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
