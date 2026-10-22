package com.robertx22.forge_compat.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public class CookingRecipeBuilder implements RecipeBuilder {
    private final Item result;

    public CookingRecipeBuilder(ItemLike result) {
        this.result = result.asItem();
    }

    public static CookingRecipeBuilder smelting(Ingredient ingredient, ItemLike result, float exp, int time) {
        return new CookingRecipeBuilder(result);
    }

    public static CookingRecipeBuilder smelting(Ingredient ingredient, Object category, ItemLike result, float exp, int time) {
        return new CookingRecipeBuilder(result);
    }

    public CookingRecipeBuilder unlockedBy(String name, Object criterion) { return this; }

    @Override
    public Item getResult() { return this.result; }

    @Override
    public void save(Consumer<IFinishedRecipe> consumer) {
        Identifier id = BuiltInRegistries.ITEM.getKey(this.result);
        consumer.accept(new IFinishedRecipe() {
            @Override public Identifier getId() { return id; }
            @Override public JsonObject serializeRecipe() { return new JsonObject(); }
        });
    }
}
