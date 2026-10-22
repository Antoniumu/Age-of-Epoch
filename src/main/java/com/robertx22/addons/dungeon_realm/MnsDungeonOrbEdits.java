package com.robertx22.addons.dungeon_realm;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqs;
import com.robertx22.dungeon_realm.database.holders.DungeonOrbs;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.orbs_of_crafting.register.orb_edit.OrbEdit;

public class MnsDungeonOrbEdits extends ExileKeyHolder<OrbEdit> {

    public static MnsDungeonOrbEdits INSTANCE = new MnsDungeonOrbEdits(MMORPG.REGISTER_INFO);

    public MnsDungeonOrbEdits(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<OrbEdit, KeyInfo> UBER_UPGRADE = ExileKey.ofId(this, "uber_upgrade", x -> {
        var e = OrbEdit.of(x.GUID(), DungeonOrbs.INSTANCE.UBER_UPGRADE.GUID());
        e.req.add(ItemReqs.INSTANCE.MAP_IS_MYTHIC.GUID());
        return e;
    });

    @Override
    public void loadClass() {

    }
}
