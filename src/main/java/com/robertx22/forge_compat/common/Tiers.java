package com.robertx22.forge_compat.common;

public enum Tiers implements Tier {
    WOOD(59),
    STONE(131),
    IRON(250),
    DIAMOND(1561),
    GOLD(32),
    NETHERITE(2031);

    private final int uses;

    Tiers(int uses) {
        this.uses = uses;
    }

    @Override
    public int getUses() {
        return this.uses;
    }
}
