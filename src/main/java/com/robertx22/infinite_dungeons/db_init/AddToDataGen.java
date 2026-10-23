package com.robertx22.infinite_dungeons.db_init;

import com.robertx22.infinite_dungeons.data_gen.adders.*;

public class AddToDataGen {

    public static void addAll() {

        new DungeonLayoutsAdder().registerAll();
        new DungeonGroupsAdder().registerAll();
        new ShopListsAdder().registerAll();
        new DungeonDifficultiesAdder().registerAll();

        new RewardListAdder().registerAll();

    }
}
