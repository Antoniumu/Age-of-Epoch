package com.robertx22.forge_compat.common;

import net.minecraft.ChatFormatting;

import java.util.Locale;

public class CompatChatFormatting {

    public static String getName(ChatFormatting fmt) {
        return fmt == null ? "white" : fmt.name().toLowerCase(Locale.ROOT);
    }

    public static ChatFormatting getByName(String name) {
        if (name == null) return ChatFormatting.WHITE;
        String clean = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z_]", "");
        try {
            return ChatFormatting.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return ChatFormatting.WHITE;
        }
    }

    public static int getColor(ChatFormatting fmt) {
        if (fmt == null) return 0xFFFFFFFF;
        return switch (fmt) {
            case BLACK -> 0xFF000000;
            case DARK_BLUE -> 0xFF0000AA;
            case DARK_GREEN -> 0xFF00AA00;
            case DARK_AQUA -> 0xFF00AAAA;
            case DARK_RED -> 0xFFAA0000;
            case DARK_PURPLE -> 0xFFAA00AA;
            case GOLD -> 0xFFFFAA00;
            case GRAY -> 0xFFAAAAAA;
            case DARK_GRAY -> 0xFF555555;
            case BLUE -> 0xFF5555FF;
            case GREEN -> 0xFF55FF55;
            case AQUA -> 0xFF55FFFF;
            case RED -> 0xFFFF5555;
            case LIGHT_PURPLE -> 0xFFFF55FF;
            case YELLOW -> 0xFFFFFF55;
            default -> 0xFFFFFFFF;
        };
    }
}
