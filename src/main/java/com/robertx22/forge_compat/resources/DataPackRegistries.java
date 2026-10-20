package com.robertx22.forge_compat.resources;

import net.minecraft.server.packs.resources.ReloadableResourceManager;

/**
 * Placeholder for 1.16's DataPackRegistries container. The 26.3 port reloads
 * custom (JSON) registries through the library's own reload listeners.
 */
public class DataPackRegistries {
    public ReloadableResourceManager getResourceManager() {
        return null;
    }

    public void close() {
    }
}
