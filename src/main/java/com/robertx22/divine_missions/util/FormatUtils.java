package com.robertx22.divine_missions.util;

import net.minecraft.ChatFormatting;

public class FormatUtils {

    public static ChatFormatting of(String name) {
        ChatFormatting format = com.robertx22.forge_compat.common.CompatChatFormatting.getByName(name);
        return format == null ? ChatFormatting.GRAY : format;
    }
}
