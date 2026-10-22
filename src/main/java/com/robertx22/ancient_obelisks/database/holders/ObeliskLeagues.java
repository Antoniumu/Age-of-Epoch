package com.robertx22.ancient_obelisks.database.holders;

import com.robertx22.ancient_obelisks.database.ObeliskLeague;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

public class ObeliskLeagues extends ExileKeyHolder<League> {

    public static ObeliskLeagues INSTANCE = new ObeliskLeagues(ObelisksMain.REGISTER_INFO);

    public ObeliskLeagues(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<League, KeyInfo> OBELISK = ExileKey.ofId(this, "obelisk", x -> new ObeliskLeague(x.GUID()));

    @Override
    public void loadClass() {

    }
}
