package com.robertx22.age_of_exile.compat.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Gear and result cycle through examples that actually pass the currency's own
 * requirements whenever samples can be validated without a player.
 */
public record CurrencyRecipe(
    Identifier id,
    List<ItemStack> gear,
    ItemStack currency,
    List<ItemStack> result,
    List<Component> lines
) {
}
