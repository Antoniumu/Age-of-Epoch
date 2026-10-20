package com.robertx22.forge_compat.common;

import net.minecraft.world.item.Item;

public class AxeItem extends Item {
    public AxeItem(Tier tier, float attackDamageModifier, float attackSpeedModifier, Item.Properties properties) {
        super(properties);
    }

    public AxeItem(Item.Properties properties) {
        super(properties);
    }
}
