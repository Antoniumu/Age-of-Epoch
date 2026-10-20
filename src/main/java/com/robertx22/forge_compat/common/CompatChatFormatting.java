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
        if (fmt == null) return 0xFFFFFF;
        return switch (fmt) {
            case BLACK -> 0x000000;
            case DARK_BLUE -> 0x0000AA;
            case DARK_GREEN -> 0x00AA00;
            case DARK_AQUA -> 0x00AAAA;
            case DARK_RED -> 0xAA0000;
            case DARK_PURPLE -> 0xAA00AA;
            case GOLD -> 0xFFAA00;
            case GRAY -> 0xAAAAAA;
            case DARK_GRAY -> 0x555555;
            case BLUE -> 0x5555FF;
            case GREEN -> 0x55FF55;
            case AQUA -> 0x55FFFF;
            case RED -> 0xFF5555;
            case LIGHT_PURPLE -> 0xFF55FF;
            case YELLOW -> 0xFFFF55;
            default -> 0xFFFFFF;
        };
    }
}
