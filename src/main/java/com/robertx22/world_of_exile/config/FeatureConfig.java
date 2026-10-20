package com.robertx22.world_of_exile.config;

import com.robertx22.forge_compat.worldgen.StructureSeparationSettings;

/**
 * Compile-time stub of the World of Exile mod's FeatureConfig (World of Exile is
 * not part of this port; see PORTING.md).
 */
public class FeatureConfig {

    public interface MyStructureConfig {
        StructureSeparationSettings get();
    }

    public final MyStructureConfig structure;

    public FeatureConfig(MyStructureConfig structure) {
        this.structure = structure;
    }

    public MyStructureConfig getStructureConfig() {
        return structure;
    }
}
