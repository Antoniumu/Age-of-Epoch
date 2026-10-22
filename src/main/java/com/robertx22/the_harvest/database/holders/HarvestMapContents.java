package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.database.extra_map_content.MapContent;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.the_harvest.main.HarvestEntries;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestMapContents extends ExileKeyHolder<MapContent> {

    public static HarvestMapContents INSTANCE = new HarvestMapContents(HarvestMain.REGISTER_INFO);

    public HarvestMapContents(ModRequiredRegisterInfo info) {
        super(info);
    }

    public ExileKey<MapContent, KeyInfo> HARVEST = ExileKey.ofId(this, HarvestMain.MODID, x -> {
        return MapContent.of(x.GUID(), 1000, HarvestEntries.HARVEST_BLOCK.getKey().identifier().toString(), 1, 1);
    });

    @Override
    public void loadClass() {

    }
}
