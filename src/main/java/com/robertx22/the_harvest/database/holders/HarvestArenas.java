package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.the_harvest.database.HarvestArena;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestArenas extends ExileKeyHolder<HarvestArena> {
    public static HarvestArenas INSTANCE = new HarvestArenas(HarvestMain.REGISTER_INFO);

    public HarvestArenas(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public static String roomId(String id) {
        return HarvestMain.MODID + ":harvest/" + id;
    }

    public ExileKey<HarvestArena, KeyInfo> AZTEC = ExileKey.ofId(this, "aztec", x -> {
        var arena = new HarvestArena(new SimplePrebuiltMapData(2, roomId(x.GUID())), 1000, x.GUID());
        return arena;
    });
    
    public ExileKey<HarvestArena, KeyInfo> THE_EVENT = ExileKey.ofId(this, "theevent", x -> {
        var arena = new HarvestArena(new SimplePrebuiltMapData(2, roomId(x.GUID())), 1000, x.GUID());
        return arena;
    });

    @Override
    public void loadClass() {

    }
}
