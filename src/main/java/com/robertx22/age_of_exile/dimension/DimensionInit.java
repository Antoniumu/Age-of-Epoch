package com.robertx22.age_of_exile.dimension;

import com.robertx22.world_of_exile.world_gen.TowerStructureProcessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class DimensionInit {

    private static boolean registered = false;

    public static void init() {
        DungeonDimensionJigsaw.initStatics();
        if (!registered) {
            registered = true;
            try {
                Identifier id = Identifier.fromNamespaceAndPath("world_of_exile", "tower_processor");
                if (!BuiltInRegistries.STRUCTURE_PROCESSOR.containsKey(id)) {
                    Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, id, TowerStructureProcessor.CODEC);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
