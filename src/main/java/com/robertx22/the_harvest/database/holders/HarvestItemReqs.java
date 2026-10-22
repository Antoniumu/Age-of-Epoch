package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestItemReqs extends ExileKeyHolder<ItemRequirement> {
    public static HarvestItemReqs INSTANCE = new HarvestItemReqs(HarvestMain.REGISTER_INFO);

    public HarvestItemReqs(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    //public ExileKey<ItemRequirement, KeyInfo> IS_OBELISK_MAP = ExileKey.ofId(this, "is_obelisk_map", x -> new BeObeliskMapReq(x.GUID()));

    @Override
    public void loadClass() {

    }
}
