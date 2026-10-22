package com.robertx22.library_of_exile.recipe;

import com.robertx22.forge_compat.data.IFinishedRecipe;
import net.minecraft.resources.Identifier;
import com.robertx22.forge_compat.data.ConditionalRecipe;
import com.robertx22.forge_compat.data.ModLoadedCondition;

import java.util.function.Consumer;

public class ConditionalRecipeData {
    public ConditionalRecipe.Builder builder;
    public Identifier id;

    public ConditionalRecipeData(ConditionalRecipe.Builder builder, Identifier id) {
        this.builder = builder;
        this.id = id;
    }

    public static ConditionalRecipeData ofModLoaded(String id, Identifier loc, Consumer<Consumer<IFinishedRecipe>> recipe) {
        ConditionalRecipe.Builder sup = new ConditionalRecipe.Builder().addCondition(new ModLoadedCondition(id)).addRecipe(recipe);
        return new ConditionalRecipeData(sup, loc);
    }
}
