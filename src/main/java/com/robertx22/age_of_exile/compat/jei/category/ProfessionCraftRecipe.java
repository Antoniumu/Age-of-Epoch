package com.robertx22.age_of_exile.compat.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One "ingredient can be used in this profession" page. Gear, ingredient and result
 * cycle through every variant that is valid for the page, so looking up any physical
 * base or tier finds the same explanation.
 */
public record ProfessionCraftRecipe(
    Identifier id,
    List<ItemStack> gear,
    List<ItemStack> ingredient,
    List<ItemStack> result,
    ItemStack tool,
    List<Component> lines
) {
}
