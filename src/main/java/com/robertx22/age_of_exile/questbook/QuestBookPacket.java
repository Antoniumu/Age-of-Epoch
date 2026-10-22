package com.robertx22.age_of_exile.questbook;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class QuestBookPacket extends MyPacket<QuestBookPacket> {

    public String action = "sync";

    public QuestBookPacket() {
    }

    public QuestBookPacket(String action) {
        this.action = action;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath("mmorpg", "questbook_sync");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        this.action = buf.readUtf();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeUtf(this.action == null ? "sync" : this.action);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            QuestBookService.handleClientPacket(sp, this);
        }
    }

    @Override
    public MyPacket<QuestBookPacket> newInstance() {
        return new QuestBookPacket();
    }
}
