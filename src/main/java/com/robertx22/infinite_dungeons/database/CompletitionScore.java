package com.robertx22.infinite_dungeons.database;

import net.minecraft.ChatFormatting;

public enum CompletitionScore {
    GOLD(ChatFormatting.GOLD, 2),
    SILVER(ChatFormatting.GREEN, 1),
    BRONZE(ChatFormatting.WHITE, 0),
    FAIL(ChatFormatting.RED, -1);

    public ChatFormatting format;
    public int tier;

    CompletitionScore(ChatFormatting format, int tier) {
        this.format = format;
        this.tier = tier;
    }
}
