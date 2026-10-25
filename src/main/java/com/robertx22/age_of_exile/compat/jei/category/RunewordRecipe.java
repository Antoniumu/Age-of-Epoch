package com.robertx22.age_of_exile.compat.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Gear and result cycle through every physical base accepted by the runeword.
 */
public record RunewordRecipe(
    Identifier id,
    List<ItemStack> runes,
    List<ItemStack> gear,
    List<ItemStack> result,
    List<Component> lines
) {
}
