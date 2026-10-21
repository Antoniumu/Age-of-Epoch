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
                Identifier delveId = Identifier.fromNamespaceAndPath("world_of_exile", "delve_processor");
                if (!BuiltInRegistries.STRUCTURE_PROCESSOR.containsKey(delveId)) {
                    Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, delveId, com.robertx22.world_of_exile.delve.DelveStructureProcessor.CODEC);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
