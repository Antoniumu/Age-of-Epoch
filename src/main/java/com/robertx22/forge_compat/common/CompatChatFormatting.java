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

    /**
     * Код цвета/стиля без "§" (например, 'c' для RED).
     * В 26.3 у ChatFormatting больше нет получения символа кодом, поэтому таблица своя.
     */
    public static char getCode(ChatFormatting fmt) {
        if (fmt == null) return 'f';
        return switch (fmt) {
            case BLACK -> '0';
            case DARK_BLUE -> '1';
            case DARK_GREEN -> '2';
            case DARK_AQUA -> '3';
            case DARK_RED -> '4';
            case DARK_PURPLE -> '5';
            case GOLD -> '6';
            case GRAY -> '7';
            case DARK_GRAY -> '8';
            case BLUE -> '9';
            case GREEN -> 'a';
            case AQUA -> 'b';
            case RED -> 'c';
            case LIGHT_PURPLE -> 'd';
            case YELLOW -> 'e';
            case WHITE -> 'f';
            case OBFUSCATED -> 'k';
            case BOLD -> 'l';
            case STRIKETHROUGH -> 'm';
            case UNDERLINE -> 'n';
            case ITALIC -> 'o';
            case RESET -> 'r';
            default -> 'f';
        };
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
