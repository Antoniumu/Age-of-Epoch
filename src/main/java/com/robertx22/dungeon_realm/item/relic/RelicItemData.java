package com.robertx22.dungeon_realm.item.relic;

import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.database.relic.relic_rarity.RelicRarity;
import com.robertx22.library_of_exile.database.relic.relic_type.RelicType;

import java.util.ArrayList;
import java.util.List;

public class RelicItemData {

    public List<RelicAffixData> affixes = new ArrayList<>();


    public String rar = "common";

    public String type = DungeonMain.MODID;

    public RelicRarity getRarity() {
        return LibDatabase.RelicRarities().get(rar);
    }

    public RelicType getType() {
        return LibDatabase.RelicTypes().get(type);
    }

}
