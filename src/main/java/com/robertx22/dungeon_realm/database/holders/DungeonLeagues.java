package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.database.league.MapRewardLeague;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

public class DungeonLeagues extends ExileKeyHolder<League> {

    public static DungeonLeagues INSTANCE = new DungeonLeagues(DungeonMain.REGISTER_INFO);

    private DungeonLeagues(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<League, KeyInfo> REWARD_ROOM = ExileKey.ofId(this, "reward_room", x -> new MapRewardLeague(x.GUID()));

    @Override
    public void loadClass() {

    }
}
