package com.robertx22.ancient_obelisks.database.holders;

import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.database.extra_map_content.MapContent;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

public class ObeliskMapContents extends ExileKeyHolder<MapContent> {

    public static ObeliskMapContents INSTANCE = new ObeliskMapContents(ObelisksMain.REGISTER_INFO);

    public ObeliskMapContents(ModRequiredRegisterInfo info) {
        super(info);
    }

    public ExileKey<MapContent, KeyInfo> OBELISK = ExileKey.ofId(this, "obelisk", x -> {
        return MapContent.of(x.GUID(), 1000, ObeliskEntries.OBELISK_BLOCK.getKey().identifier().toString(), 1, 1);
    });

    @Override
    public void loadClass() {

    }
}
