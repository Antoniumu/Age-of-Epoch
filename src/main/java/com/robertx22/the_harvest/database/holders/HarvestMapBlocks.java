package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.database.map_data_block.all.SetBlockMB;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.the_harvest.main.HarvestEntries;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestMapBlocks extends ExileKeyHolder<MapDataBlock> {

    public static HarvestMapBlocks INSTANCE = new HarvestMapBlocks(HarvestMain.REGISTER_INFO);

    public HarvestMapBlocks(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<MapDataBlock, KeyInfo> HARVEST_SPAWNER = ExileKey.ofId(this, "harvest_spawner", x -> new SetBlockMB(x.GUID(), HarvestEntries.SPAWNER_BLOCK.getId().toString()));

    @Override
    public void loadClass() {

    }
}
