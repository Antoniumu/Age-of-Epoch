package com.robertx22.dungeon_realm.database.league;

import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.league.League;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class MapRewardLeague extends League {
    public MapRewardLeague(String id) {
        super(id);
    }

    @Override
    public boolean isInSide(ServerLevel serverLevel, BlockPos blockPos) {
        return structureLeagueCheck(DungeonMain.MAP, DungeonMain.REWARD_ROOM, serverLevel, blockPos);
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.GOLD;
    }

    @Override
    public String modid() {
        return DungeonMain.MODID;
    }

    @Override
    public String locName() {
        return "Reward Room";
    }
}
