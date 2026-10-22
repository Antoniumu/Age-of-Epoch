package com.robertx22.ancient_obelisks.database.holders;

import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.database.relic.relic_type.RelicType;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.library_of_exile.util.TranslateInfo;

public class ObeliskRelicTypes extends ExileKeyHolder<RelicType> {

    public static ObeliskRelicTypes INSTANCE = new ObeliskRelicTypes(ObelisksMain.REGISTER_INFO);

    public ObeliskRelicTypes(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<RelicType, KeyInfo> GENERAL = ExileKey.ofId(this, ObelisksMain.MODID, x -> {
        var r = new RelicType(x.GUID(), new TranslateInfo(ObelisksMain.MODID, "Obelisk Relic"));
        r.max_equipped = 3;
        r.weight = 1000;
        r.item_id = ObeliskEntries.RELIC.getId().toString();
        return r;
    });

    @Override
    public void loadClass() {

    }
}
