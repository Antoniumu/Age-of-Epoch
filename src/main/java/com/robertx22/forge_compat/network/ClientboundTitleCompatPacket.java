package com.robertx22.forge_compat.network;

import net.minecraft.network.chat.Component;

/**
 * 1.16's STitlePacket merged three modern packets (times/title/subtitle/actionbar);
 * this holder keeps the old constructor shape and is adapted in one place.
 */
public class ClientboundTitleCompatPacket {

    public enum Type { TITLE, SUBTITLE, ACTIONBAR, TIMES, CLEAR, RESET }

    public final Type type;
    public final Component text;
    public final int fadeIn;
    public final int stay;
    public final int fadeOut;

    public ClientboundTitleCompatPacket(Type type, Component text, int fadeIn, int stay, int fadeOut) {
        this.type = type;
        this.text = text;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }
}
