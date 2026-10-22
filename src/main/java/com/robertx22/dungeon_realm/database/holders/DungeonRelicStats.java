package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.relic.stat.ManualRelicStat;
import com.robertx22.library_of_exile.database.relic.stat.RelicStat;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

public class DungeonRelicStats extends ExileKeyHolder<RelicStat> {

    public static DungeonRelicStats INSTANCE = new DungeonRelicStats(DungeonMain.REGISTER_INFO);

    public DungeonRelicStats(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<RelicStat, KeyInfo> BONUS_BOSS_FRAG_CHANCE = ExileKey.ofId(this, "bonus_boss_frag_chance", x -> {
        var stat = new ManualRelicStat(x.GUID(), DungeonMain.MODID, "%1$s Increased Uber Fragment Drop chance from Map Bosses");
        return stat;
    });
    public ExileKey<RelicStat, KeyInfo> EXTRA_MAP_BOSS_CHANCE = ExileKey.ofId(this, "extra_map_boss_chance", x -> {
        var stat = new ManualRelicStat(x.GUID(), DungeonMain.MODID, "%1$s Chance to spawn an extra Map Boss");
        return stat;
    });
    public ExileKey<RelicStat, KeyInfo> BONUS_MAP_ITEM_FROM_BOSS_CHANCE = ExileKey.ofId(this, "bonus_map_from_boss_chance", x -> {
        var stat = new ManualRelicStat(x.GUID(), DungeonMain.MODID, "%1$s Chance to drop an extra Map from the Boss");
        return stat;
    });
    public ExileKey<RelicStat, KeyInfo> PACK_SIZE = ExileKey.ofId(this, "pack_size", x -> {
        var stat = new ManualRelicStat(x.GUID(), DungeonMain.MODID, "%1$s Increased Mob Pack Size");
        stat.max = 500;
        return stat;
    });
    public ExileKey<RelicStat, KeyInfo> BONUS_CONTENT_CHANCE = ExileKey.ofId(this, "bonus_content_chance", x -> {
        var stat = new ManualRelicStat(x.GUID(), DungeonMain.MODID, "%1$s Chance to Spawn Additional Bonus Content");
        return stat;
    });

    @Override
    public void loadClass() {

    }
}
