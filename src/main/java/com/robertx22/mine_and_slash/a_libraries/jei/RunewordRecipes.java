package com.robertx22.mine_and_slash.a_libraries.jei;

import com.robertx22.library_of_exile.registry.ExileRegistryContainer;
import com.robertx22.mine_and_slash.database.data.runewords.RuneWord;
import com.robertx22.mine_and_slash.database.data.runewords.RunewordRecipe;

import java.util.List;

public class RunewordRecipes {
    public static List<RunewordRecipe> Generate(ExileRegistryContainer<RuneWord> runeWordExileRegistryContainer) {
        return runeWordExileRegistryContainer.getList()
                .stream().filter(x -> !x.isEmpty())
                .map(x -> {
                    RunewordRecipe recipe = new RunewordRecipe();
                    recipe.id = x.id;
                    recipe.name = x.name;
                    recipe.runes = x.runes;
                    recipe.slots = x.slots;
                    recipe.stats = x.stats;
                    return recipe;
                })
                .toList();
    }
}
