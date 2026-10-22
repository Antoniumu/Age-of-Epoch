package com.robertx22.addons.dungeon_realm;

import com.robertx22.library_of_exile.database.league.League;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class ProphecyLeague extends League {
    public ProphecyLeague(String id) {
        super(id);
    }

    @Override
    public boolean isInSide(ServerLevel serverLevel, BlockPos blockPos) {
        return false;
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.LIGHT_PURPLE;
    }

    @Override
    public String modid() {
        return SlashRef.MODID;
    }

    @Override
    public String locName() {
        return "Prophecy";
    }
}
