package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.database.item_reqs.BeMapReq;
import com.robertx22.dungeon_realm.database.item_reqs.BeNotUberReq;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;

public class DungeonItemReqs extends ExileKeyHolder<ItemRequirement> {

    public static DungeonItemReqs INSTANCE = new DungeonItemReqs(DungeonMain.REGISTER_INFO);

    private DungeonItemReqs(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<ItemRequirement, KeyInfo> IS_MAP_ITEM = ExileKey.ofId(this, "is_map", x -> new BeMapReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> IS_NOT_UBER = ExileKey.ofId(this, "is_not_uber", x -> new BeNotUberReq(x.GUID()));


    @Override
    public void loadClass() {

    }
}
