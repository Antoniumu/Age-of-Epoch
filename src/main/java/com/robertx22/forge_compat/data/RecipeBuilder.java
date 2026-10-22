package com.robertx22.forge_compat.data;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;

public interface RecipeBuilder {
    Item getResult();
    void save(Consumer<IFinishedRecipe> consumer);
    default void save(Consumer<IFinishedRecipe> consumer, Identifier id) {
        save(consumer);
    }
}
