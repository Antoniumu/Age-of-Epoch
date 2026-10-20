package com.robertx22.age_of_exile.dimension;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class DimensionInit {

    public static void init() {
        DungeonDimensionJigsaw.initStatics();

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, SlashRef.id("dungeon_flat"), VoidChunkGenerator.VOID_CODED);
    }

}
