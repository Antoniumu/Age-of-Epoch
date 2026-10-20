package com.robertx22.forge_compat.common;

import net.minecraft.world.item.Item;

public class SwordItem extends Item {
    public SwordItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Item.Properties properties) {
        super(properties);
    }

    public SwordItem(Item.Properties properties) {
        super(properties);
    }
}
