package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.database.relic.affix.RelicAffix;
import com.robertx22.library_of_exile.database.relic.stat.RelicMod;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

public class DungeonRelicAffixes extends ExileKeyHolder<MapDataBlock> {

    public static DungeonRelicAffixes INSTANCE = new DungeonRelicAffixes(DungeonMain.REGISTER_INFO);

    public DungeonRelicAffixes(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    static String TYPE = DungeonMain.MODID;

    public ExileKey<RelicAffix, KeyInfo> UBER_FRAG = ExileKey.ofId(this, "uber_frag_chance", x -> {
        return new RelicAffix(x.GUID(), TYPE, new RelicMod(DungeonRelicStats.INSTANCE.BONUS_BOSS_FRAG_CHANCE, 1, 5));
    });
    public ExileKey<RelicAffix, KeyInfo> PACK_SIZE = ExileKey.ofId(this, "pack_size", x -> {
        return new RelicAffix(x.GUID(), TYPE, new RelicMod(DungeonRelicStats.INSTANCE.PACK_SIZE, 1, 5));
    });
    public ExileKey<RelicAffix, KeyInfo> EXTRA_MAP_BOSS_CHANCE = ExileKey.ofId(this, "extra_map_boss_chance", x -> {
        return new RelicAffix(x.GUID(), TYPE, new RelicMod(DungeonRelicStats.INSTANCE.EXTRA_MAP_BOSS_CHANCE, 1, 5));
    });
    
    public ExileKey<RelicAffix, KeyInfo> BONUS_CONTENT_CHANCE = ExileKey.ofId(this, "bonus_content_chance", x -> {
        return new RelicAffix(x.GUID(), TYPE, new RelicMod(DungeonRelicStats.INSTANCE.BONUS_CONTENT_CHANCE, 5, 25));
    });

    @Override
    public void loadClass() {

    }
}
