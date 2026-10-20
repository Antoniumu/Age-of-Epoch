package com.robertx22.infinite_dungeons.database.adders;

import com.robertx22.infinite_dungeons.database.dungeon_types.CleanUpDungeon;
import com.robertx22.infinite_dungeons.database.dungeon_types.SlaughterDungeon;
import com.robertx22.infinite_dungeons.database.dungeon_types.WaveBasedDungeon;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class DungeonTypesAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {

        new CleanUpDungeon().registerToExileRegistry();
        new SlaughterDungeon().registerToExileRegistry();
        new WaveBasedDungeon().registerToExileRegistry();
    }
}
