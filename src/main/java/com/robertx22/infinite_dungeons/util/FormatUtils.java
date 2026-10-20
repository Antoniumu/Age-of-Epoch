package com.robertx22.infinite_dungeons.util;

import net.minecraft.ChatFormatting;

public class FormatUtils {

    public static ChatFormatting of(String name) {
        ChatFormatting format = com.robertx22.forge_compat.common.CompatChatFormatting.getByName(name);
        return format == null ? ChatFormatting.GRAY : format;
    }
}
