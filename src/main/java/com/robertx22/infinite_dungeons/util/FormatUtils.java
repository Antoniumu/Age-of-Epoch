package com.robertx22.infinite_dungeons.util;

import net.minecraft.ChatFormatting;

public class FormatUtils {

    public static ChatFormatting of(String name) {
        ChatFormatting format = ChatFormatting.getByName(name);
        return format == null ? ChatFormatting.GRAY : format;
    }
}
