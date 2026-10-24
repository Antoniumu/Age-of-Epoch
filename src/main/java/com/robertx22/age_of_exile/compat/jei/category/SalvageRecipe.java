package com.robertx22.age_of_exile.compat.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * @param id      stable id for JEI bookmarks
 * @param gear    example gear piece of the shown rarity
 * @param outputs salvaged essence (and rarity essence for higher rarities)
 * @param lines   extra explanation drawn under the slots
 */
public record SalvageRecipe(
    Identifier id,
    ItemStack gear,
    List<ItemStack> outputs,
    List<Component> lines
) {
}
