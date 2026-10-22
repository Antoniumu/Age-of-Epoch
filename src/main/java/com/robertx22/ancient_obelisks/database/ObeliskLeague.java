package com.robertx22.ancient_obelisks.database;

import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.database.league.League;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class ObeliskLeague extends League {
    public ObeliskLeague(String id) {
        super(id);
    }

    @Override
    public boolean isInSide(ServerLevel serverLevel, BlockPos blockPos) {
        return League.structureLeagueCheck(ObelisksMain.MAP, ObelisksMain.OBELISK_MAP_STRUCTURE, serverLevel, blockPos);
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.DARK_PURPLE;
    }

    @Override
    public String modid() {
        return ObelisksMain.MODID;
    }

    @Override
    public String locName() {
        return "Ancient Obelisks";
    }
}
