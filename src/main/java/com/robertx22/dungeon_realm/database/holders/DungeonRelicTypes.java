package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.main.DungeonEntries;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.relic.relic_type.RelicType;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.library_of_exile.util.TranslateInfo;

public class DungeonRelicTypes extends ExileKeyHolder<RelicType> {

    public static DungeonRelicTypes INSTANCE = new DungeonRelicTypes(DungeonMain.REGISTER_INFO);

    public DungeonRelicTypes(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<RelicType, KeyInfo> GENERAL = ExileKey.ofId(this, DungeonMain.MODID, x -> {
        var r = new RelicType(x.GUID(), new TranslateInfo(DungeonMain.MODID, "General Relic"));
        r.max_equipped = 9;
        r.weight = 3000;
        r.item_id = DungeonEntries.RELIC_ITEM.getId().toString();
        return r;
    });

    @Override
    public void loadClass() {

    }
}
