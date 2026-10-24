package com.robertx22.age_of_exile.compat.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * @param id     stable id for JEI bookmarks
 * @param runes  the exact set of runes the station consumes
 * @param gear   the base item the runeword can be applied to
 * @param result the unique item the runes turn that base into
 * @param lines  extra explanation drawn under the slots
 */
public record RunewordRecipe(
    Identifier id,
    List<ItemStack> runes,
    ItemStack gear,
    ItemStack result,
    List<Component> lines
) {
}
