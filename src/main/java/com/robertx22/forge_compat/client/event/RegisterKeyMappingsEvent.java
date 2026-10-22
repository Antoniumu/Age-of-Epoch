package com.robertx22.forge_compat.client.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class RegisterKeyMappingsEvent extends Event {
    public void register(KeyMapping key) {
        KeyMappingHelper.registerKeyMapping(key);
    }
}
