package com.robertx22.age_of_exile.compat.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Input cycles through valid gear/soul examples; every output slot cycles through the
 * possible tier variants of that output.
 */
public record SalvageRecipe(
    Identifier id,
    List<ItemStack> input,
    List<List<ItemStack>> outputs,
    List<Component> lines
) {
}
