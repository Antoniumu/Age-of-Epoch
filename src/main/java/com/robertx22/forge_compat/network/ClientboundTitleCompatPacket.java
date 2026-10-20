package com.robertx22.forge_compat.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ClientboundTitleCompatPacket {

    public enum Type { TITLE, SUBTITLE, ACTIONBAR, TIMES, CLEAR, RESET }

    public final Type type;
    public final Component text;
    public final int fadeIn;
    public final int stay;
    public final int fadeOut;

    public ClientboundTitleCompatPacket(Type type, Component text) {
        this(type, text, 5, 15, 8);
    }

    public ClientboundTitleCompatPacket(Type type, Component text, int fadeIn, int stay, int fadeOut) {
        this.type = type;
        this.text = text;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public void sendTo(ServerPlayer player) {
        if (player == null || text == null) return;
        if (type == Type.ACTIONBAR) {
            player.sendOverlayMessage(text);
        } else if (type == Type.TITLE || type == Type.SUBTITLE) {
            player.sendOverlayMessage(text);
        }
    }
}
