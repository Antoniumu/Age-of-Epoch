package com.robertx22.age_of_exile.compat.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One "ingredient can be used in this profession" page.
 *
 * @param id          stable id for JEI bookmarks
 * @param gear        the gear piece being enchanted (empty for cooking/inscribing/alchemy)
 * @param ingredient  the crafting ingredient shown as the focus of the page
 * @param result      the item the player gets back after the craft resolves
 * @param tool        the profession tool that opens the grid (also registered as a catalyst)
 * @param lines       extra explanation drawn under the grid
 */
public record ProfessionCraftRecipe(
    Identifier id,
    ItemStack gear,
    ItemStack ingredient,
    ItemStack result,
    ItemStack tool,
    List<Component> lines
) {
}
