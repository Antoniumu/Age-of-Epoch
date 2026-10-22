package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.the_harvest.database.HarvestLeague;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestLeagues extends ExileKeyHolder<League> {

    public static HarvestLeagues INSTANCE = new HarvestLeagues(HarvestMain.REGISTER_INFO);

    public HarvestLeagues(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<League, KeyInfo> HARVEST = ExileKey.ofId(this, "harvest", x -> new HarvestLeague(x.GUID()));

    @Override
    public void loadClass() {

    }
}
