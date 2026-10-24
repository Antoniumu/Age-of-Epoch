package com.robertx22.age_of_exile.compat.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Currency orbs, gems and every other "drag this onto gear" item. The result cannot be
 * predicted (it is random), so the page explains the effect with text and shows the
 * same item on both sides.
 *
 * @param id       stable id for JEI bookmarks
 * @param gear     example gear piece the item can be used on
 * @param currency the orb / gem / tool being applied
 * @param result   the gear piece after the modification
 * @param lines    description + requirements
 */
public record CurrencyRecipe(
    Identifier id,
    ItemStack gear,
    ItemStack currency,
    ItemStack result,
    List<Component> lines
) {
}
