package com.robertx22.age_of_exile.dimension.packets;

import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.tiers.base.Difficulty;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.dimension.teleporter.TeleportedBlockEntity;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class StartDelveMapPacket extends MyPacket<StartDelveMapPacket> {

    String diff;
    BlockPos pos;

    public StartDelveMapPacket() {

    }

    public StartDelveMapPacket(Difficulty diff, BlockPos pos) {
        this.diff = diff.GUID();
        this.pos = pos;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return ResourceLocation.fromNamespaceAndPath(SlashRef.MODID, "start_delve");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        pos = tag.readBlockPos();
        diff = tag.readUtf(500);
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeBlockPos(pos);
        tag.writeUtf(diff);

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        Player player = ctx.getPlayer();

        RPGPlayerData maps = Load.playerRPGData(player);

        Difficulty difficulty = ExileDB.Difficulties()
            .get(diff);

        BlockEntity tile = player.level.getBlockEntity(pos);

        if (tile instanceof TeleportedBlockEntity) {
            TeleportedBlockEntity be = (TeleportedBlockEntity) tile;
            if (be.data.type.isDungeon()) {
                maps.createRandomDungeon(difficulty);
                be.data.activated = true;
            }
        }
    }

    @Override
    public MyPacket<StartDelveMapPacket> newInstance() {
        return new StartDelveMapPacket();
    }
}

