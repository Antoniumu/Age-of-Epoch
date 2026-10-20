package com.robertx22.infinite_dungeons.data_gen.adders;

import com.robertx22.infinite_dungeons.data_gen.builders.DungeonLayoutBuilder;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonSpawn;
import com.robertx22.infinite_dungeons.database.db_types.layout.config.CleanUpDungeonConfig;
import com.robertx22.infinite_dungeons.database.db_types.layout.config.SlaughterDungeonConfig;
import com.robertx22.infinite_dungeons.database.db_types.layout.config.WaveBasedDungeonConfig;
import com.robertx22.infinite_dungeons.database.ids.DungeonGroupIds;
import com.robertx22.infinite_dungeons.database.ids.DungeonLayoutIds;
import com.robertx22.infinite_dungeons.database.ids.DungeonModifierIds;
import com.robertx22.infinite_dungeons.database.ids.DungeonTypeIds;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.world.entity.EntityType;

public class DungeonLayoutsAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {

        DungeonLayoutBuilder.of(DungeonLayoutIds.UNDEAD_GRAVEYARD, DungeonTypeIds.WAVE, DungeonGroupIds.SKELETAL)
            .edit(x -> {
                x.wave_config = new WaveBasedDungeonConfig();
                x.spawns.add(new DungeonSpawn(EntityType.SKELETON, 1000, 0).addModifier(DungeonModifierIds.MELEE_SKELETONS_CHANCE));
            })
            .addSpawn(EntityType.WITHER_SKELETON, 50)
            .build();

        DungeonLayoutBuilder.of(DungeonLayoutIds.ABANDONED_PRISON, DungeonTypeIds.SLAUGHTER, DungeonGroupIds.SKELETAL)
            .edit(x -> {
                x.slaughter_config = new SlaughterDungeonConfig();
            })
            .addSpawn(EntityType.ZOMBIFIED_PIGLIN, 50)
            .addSpawn(EntityType.ZOMBIE_VILLAGER, 100)
            .addSpawn(EntityType.ZOMBIE, 1000)
            .addSpawn(EntityType.WITCH, 25)
            .build();

        DungeonLayoutBuilder.of(DungeonLayoutIds.DIAMOND_LICH_CRYPT, DungeonTypeIds.CLEAN_UP, DungeonGroupIds.SKELETAL)
            .edit(x -> {
                x.clean_up_config = new CleanUpDungeonConfig();
                x.spawns.add(new DungeonSpawn(EntityType.SKELETON, 1000, 0).addModifier(DungeonModifierIds.MELEE_SKELETONS_CHANCE));
            })
            .addSpawn(EntityType.STRAY, 200)
            .build();

        DungeonLayoutBuilder.of(DungeonLayoutIds.IMBUED_TREE_CAVE, DungeonTypeIds.SLAUGHTER, DungeonGroupIds.VOID)
            .edit(x -> {
                x.slaughter_config = new SlaughterDungeonConfig();
            })
            .addSpawn(EntityType.PILLAGER, 500)
            .addSpawn(EntityType.EVOKER, 25)
            .addSpawn(EntityType.WITCH, 75)
            .build();

        DungeonLayoutBuilder.of(DungeonLayoutIds.NETHER_FORTRESS, DungeonTypeIds.SLAUGHTER, DungeonGroupIds.NETHER)
            .edit(x -> {
                x.slaughter_config = new SlaughterDungeonConfig();
            })
            .addSpawn(EntityType.BLAZE, 100)
            .addSpawn(EntityType.MAGMA_CUBE, 50)
            .addSpawn(EntityType.WITHER_SKELETON, 500)
            .build();

    }
}
