package com.robertx22.infinite_dungeons.db_init;

import com.robertx22.infinite_dungeons.database.adders.DungeonModifiersAdder;
import com.robertx22.infinite_dungeons.database.adders.DungeonTypesAdder;

public class RegisterNonDatapackEntries {

    public static void register() {

        new DungeonTypesAdder().registerAll();
        new DungeonModifiersAdder().registerAll();

    }
}
