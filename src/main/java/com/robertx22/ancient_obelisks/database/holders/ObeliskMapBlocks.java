package com.robertx22.ancient_obelisks.database.holders;

import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.database.map_data_block.all.SetBlockMB;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

public class ObeliskMapBlocks extends ExileKeyHolder<MapDataBlock> {

    public static ObeliskMapBlocks INSTANCE = new ObeliskMapBlocks(ObelisksMain.REGISTER_INFO);

    public ObeliskMapBlocks(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<MapDataBlock, KeyInfo> OBELISK_SPAWNER = ExileKey.ofId(this, "obelisk_spawner", x -> new SetBlockMB(x.GUID(), ObeliskEntries.SPAWNER_BLOCK.getId().toString()));
    public ExileKey<MapDataBlock, KeyInfo> REWARD = ExileKey.ofId(this, "obelisk_reward", x -> new SetBlockMB(x.GUID(), ObeliskEntries.OBELISK_REWARD_BLOCK.getId().toString()));

    @Override
    public void loadClass() {

    }
}
