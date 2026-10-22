package com.robertx22.dungeon_realm.stat_util;

import com.robertx22.dungeon_realm.database.holders.DungeonRelicStats;
import com.robertx22.library_of_exile.database.relic.stat.RelicStatsContainer;
import com.robertx22.library_of_exile.utils.RandomUtils;

public class MobPackSizeEffect {
    public static int tryIncreasePackSize(int mobs, RelicStatsContainer c) {
        float size = c.get(DungeonRelicStats.INSTANCE.PACK_SIZE);
        
        float pack = ((float) mobs * (1 + size / 100F));

        float remaining = (pack - ((int) pack)) * 100F;

        if (RandomUtils.roll(remaining)) {
            pack++;
        }

        return (int) pack;
    }

    public static int tryIncreasePackSize(int mobs, int packSizeStat) {

        float pack = ((float) mobs * (1 + packSizeStat / 100F));

        float remaining = (pack - ((int) pack)) * 100F;

        if (RandomUtils.roll(remaining)) {
            pack++;
        }

        return (int) pack;
    }
}
