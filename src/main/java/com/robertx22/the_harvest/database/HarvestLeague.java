package com.robertx22.the_harvest.database;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.the_harvest.main.HarvestMain;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class HarvestLeague extends League {
    public HarvestLeague(String id) {
        super(id);
    }

    @Override
    public boolean isInSide(ServerLevel serverLevel, BlockPos blockPos) {
        return League.structureLeagueCheck(HarvestMain.MAP, HarvestMain.HARVEST_MAP_STRUCTURE, serverLevel, blockPos);
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.GREEN;
    }

    @Override
    public String modid() {
        return HarvestMain.MODID;
    }

    @Override
    public String locName() {
        return "The Harvest";
    }
}
