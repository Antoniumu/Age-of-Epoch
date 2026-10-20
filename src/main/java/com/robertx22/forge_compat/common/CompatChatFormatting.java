package com.robertx22.forge_compat.common;

import net.minecraft.ChatFormatting;

public final class CompatChatFormatting {
    private CompatChatFormatting() {}

    public static int getColor(ChatFormatting fmt) {
        if (fmt == null) return 0xFFFFFFFF;
        Integer c = fmt.getColor();
        return c != null ? (c | 0xFF000000) : 0xFFFFFFFF;
    }
}
