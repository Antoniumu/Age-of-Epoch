package com.robertx22.infinite_dungeons.packets;

import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.main.DungeonEvents;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public class StartDungeonPacket extends MyPacket<StartDungeonPacket> {

    public String layout;
    public String diff;

    public StartDungeonPacket(String layout, String diff) {
        this.layout = layout;
        this.diff = diff;
    }

    @Override
    public Identifier getIdentifier() {
        return MainID.id("start_dungeon");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.layout = buf.readUtf(500);
        this.diff = buf.readUtf(500);
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeUtf(layout, 500);
        buf.writeUtf(diff, 500);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        Player player = ctx.getPlayer();

        DungeonEvents.startDungeon(player.getMainHandItem(), player, DungeonsDB.DungeonLayouts()
            .get(layout), DungeonsDB.Difficulties()
            .get(diff));

    }

    @Override
    public MyPacket newInstance() {
        return new StartDungeonPacket("", "");
    }
}
