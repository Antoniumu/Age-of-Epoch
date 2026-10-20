package com.robertx22.forge_compat.common;

public interface Tier {
    Tier WOOD = Tiers.WOOD;
    Tier STONE = Tiers.STONE;
    Tier IRON = Tiers.IRON;
    Tier DIAMOND = Tiers.DIAMOND;
    Tier GOLD = Tiers.GOLD;
    Tier NETHERITE = Tiers.NETHERITE;

    /** Number of uses (durability) this tool material provides. */
    int getUses();
}
