package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.database.DatabaseCaches;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public class TellClientResetCaches extends MyPacket<TellClientResetCaches> {


    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "reset_caches");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {

    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        DatabaseCaches.resetCaches();
    }

    @Override
    public MyPacket<TellClientResetCaches> newInstance() {
        return new TellClientResetCaches();
    }


}