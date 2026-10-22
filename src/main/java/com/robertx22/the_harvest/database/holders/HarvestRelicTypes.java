package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.database.relic.relic_type.RelicType;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.library_of_exile.util.TranslateInfo;
import com.robertx22.the_harvest.main.HarvestEntries;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestRelicTypes extends ExileKeyHolder<RelicType> {

    public static HarvestRelicTypes INSTANCE = new HarvestRelicTypes(HarvestMain.REGISTER_INFO);

    public HarvestRelicTypes(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<RelicType, KeyInfo> GENERAL = ExileKey.ofId(this, HarvestMain.MODID, x -> {
        var r = new RelicType(x.GUID(), new TranslateInfo(HarvestMain.MODID, "Harvest Relic"));
        r.max_equipped = 3;
        r.weight = 1000;
        r.item_id = HarvestEntries.RELIC.getId().toString();
        return r;
    });

    @Override
    public void loadClass() {

    }
}
