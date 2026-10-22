package com.robertx22.addons.dungeon_realm;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;

public class MnsLeagues extends ExileKeyHolder<League> {

    public static MnsLeagues INSTANCE = new MnsLeagues(MMORPG.REGISTER_INFO);

    public MnsLeagues(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<League, KeyInfo> PROPHECY = ExileKey.ofId(this, "prophecy", x -> new ProphecyLeague(x.GUID()));

    @Override
    public void loadClass() {

    }
}
