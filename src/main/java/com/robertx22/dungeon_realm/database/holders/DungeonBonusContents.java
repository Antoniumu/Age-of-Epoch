package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.main.DungeonEntries;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.extra_map_content.MapContent;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

public class DungeonBonusContents extends ExileKeyHolder<MapContent> {

    public static DungeonBonusContents INSTANCE = new DungeonBonusContents(DungeonMain.REGISTER_INFO);

    public DungeonBonusContents(ModRequiredRegisterInfo info) {
        super(info);
    }

    public ExileKey<MapContent, KeyInfo> UBER_BOSS = ExileKey.ofId(this, "uber_boss", x -> MapContent.of(x.GUID(), 0, DungeonEntries.UBER_TELEPORT.getKey().identifier().toString(), 1, 1));

    public ExileKey<MapContent, KeyInfo> BOSS = ExileKey.ofId(this, "boss", x -> {
        var c = MapContent.of(x.GUID(), 0, DungeonEntries.BOSS_TELEPORT.getKey().identifier().toString(), 1, 1);
        c.always_spawn = true;
        return c;
    });

    @Override
    public void loadClass() {

    }
}
