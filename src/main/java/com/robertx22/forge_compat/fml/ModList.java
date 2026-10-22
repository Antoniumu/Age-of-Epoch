package com.robertx22.forge_compat.fml;

import net.fabricmc.loader.api.FabricLoader;
import java.util.Optional;

public class ModList {
    private static final ModList INSTANCE = new ModList();
    public static ModList get() { return INSTANCE; }
    public boolean isLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid) || "curios".equals(modid) || "library_of_exile".equals(modid) || "mmorpg".equals(modid) || "orbs_of_crafting".equals(modid) || "dungeon_realm".equals(modid) || "ancient_obelisks".equals(modid) || "the_harvest".equals(modid);
    }
    public Optional<ModContainerShim> getModContainerById(String modid) {
        return Optional.of(new ModContainerShim(modid));
    }
    public static class ModContainerShim {
        private final String modid;
        public ModContainerShim(String modid) { this.modid = modid; }
        public ModInfoShim getModInfo() { return new ModInfoShim(modid); }
    }
    public static class ModInfoShim {
        private final String modid;
        public ModInfoShim(String modid) { this.modid = modid; }
        public String getDisplayName() {
            return FabricLoader.getInstance().getModContainer(modid).map(c -> c.getMetadata().getName()).orElse(modid);
        }
    }
}
