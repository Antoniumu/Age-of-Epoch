package com.robertx22.divine_missions.util;

import net.minecraft.ChatFormatting;

public class FormatUtils {

    public static ChatFormatting of(String name) {
        ChatFormatting format = ChatFormatting.getByName(name);
        return format == null ? ChatFormatting.GRAY : format;
    }
}
