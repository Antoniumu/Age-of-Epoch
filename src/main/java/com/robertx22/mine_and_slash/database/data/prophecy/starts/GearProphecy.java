package com.robertx22.mine_and_slash.database.data.prophecy.starts;

import com.robertx22.addons.dungeon_realm.MnsLeagues;
import com.robertx22.mine_and_slash.database.data.prophecy.ProphecyModifierType;
import com.robertx22.mine_and_slash.database.data.prophecy.ProphecyStart;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.ItemBlueprint;

public class GearProphecy extends ProphecyStart {

    @Override
    public ItemBlueprint create(int lvl, int tier) {
        var info = LootInfo.ofLevel(lvl);

        info.league = MnsLeagues.INSTANCE.PROPHECY.get();

        info.map_tier = tier;
        return new GearBlueprint(info);
    }

    @Override
    public boolean acceptsModifier(ProphecyModifierType type) {
        return type == ProphecyModifierType.GEAR_TYPE
                || type == ProphecyModifierType.GEAR_RARITY;
    }

    @Override
    public String GUID() {
        return "gear";
    }

    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public String locNameForLangFile() {
        return "Gear Prophecy";
    }
}
