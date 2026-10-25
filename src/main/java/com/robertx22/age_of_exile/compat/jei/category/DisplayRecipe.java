package com.robertx22.age_of_exile.compat.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Generic Age of Epoch transformation or random-pool page.
 *
 * @param inputs   consumed ingredients; each inner list cycles in one JEI slot
 * @param catalyst a non-consumed tool/station/source shown as a crafting station
 * @param outputs  result or random-pool slots; each inner list cycles in one JEI slot
 */
public record DisplayRecipe(
    Identifier id,
    List<List<ItemStack>> inputs,
    List<ItemStack> catalyst,
    List<List<ItemStack>> outputs,
    List<Component> lines
) {
    public DisplayRecipe {
        inputs = inputs == null ? List.of() : inputs;
        catalyst = catalyst == null ? List.of() : catalyst;
        outputs = outputs == null ? List.of() : outputs;
        lines = lines == null ? List.of() : lines;
    }
}
