package com.robertx22.age_of_exile.dimension.packets;

import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.dimension.dungeon_data.DungeonData;
import com.robertx22.age_of_exile.dimension.dungeon_data.TeamSize;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TeamUtils;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class StartDungeonPacket extends MyPacket<StartDungeonPacket> {

    BlockPos pos;
    TeamSize teamSize = TeamSize.SOLO;

    public StartDungeonPacket() {

    }

    public StartDungeonPacket(TeamSize teamSize, BlockPos pos) {

        this.pos = pos;
        this.teamSize = teamSize;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "start_dun");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        pos = tag.readBlockPos();
        teamSize = tag.readEnum(TeamSize.class);

    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeBlockPos(pos);
        tag.writeEnum(teamSize);

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        RPGPlayerData maps = Load.playerRPGData(ctx.getPlayer());

        DungeonData dungeon = maps.maps.dungeonData;

        if (teamSize.requiredMemberAmount > 1) {
            if (ServerContainer.get().REQUIRE_TEAM_FOR_TEAM_DUNGEONS.get()) {
                if (TeamUtils.getOnlineTeamMembersInRange(ctx.getPlayer())
                    .size() < teamSize.requiredMemberAmount) {
                    ctx.getPlayer()
                        .sendSystemMessage(Component.literal("You need at least " + teamSize.requiredMemberAmount + "  party members nearby to start a dungeon in this mode."));
                    ctx.getPlayer()
                        .sendSystemMessage(Component.literal("Use /age_of_exile teams"));
                    return;

                }
            }
        }

        maps.maps.onStartDungeon(ctx.getPlayer(), teamSize, pos, dungeon.uuid);

    }

    @Override
    public MyPacket<StartDungeonPacket> newInstance() {
        return new StartDungeonPacket();
    }
}

