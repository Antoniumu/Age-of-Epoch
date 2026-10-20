package com.robertx22.forge_compat.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public class ShapelessRecipeBuilder {
    private final Item result;
    private final int count;

    public ShapelessRecipeBuilder(ItemLike result, int count) {
        this.result = result.asItem();
        this.count = count;
    }

    public static ShapelessRecipeBuilder shapeless(ItemLike result) {
        return new ShapelessRecipeBuilder(result, 1);
    }

    public static ShapelessRecipeBuilder shapeless(ItemLike result, int count) {
        return new ShapelessRecipeBuilder(result, count);
    }

    public ShapelessRecipeBuilder requires(TagKey<Item> tag) {
        return this;
    }

    public ShapelessRecipeBuilder requires(ItemLike item) {
        return this;
    }

    public ShapelessRecipeBuilder requires(ItemLike item, int quantity) {
        return this;
    }

    public ShapelessRecipeBuilder requires(Ingredient ingredient) {
        return this;
    }

    public ShapelessRecipeBuilder requires(Ingredient ingredient, int quantity) {
        return this;
    }

    public ShapelessRecipeBuilder unlockedBy(String name, CriterionTriggerInstance criterion) {
        return this;
    }

    public ShapelessRecipeBuilder group(String group) {
        return this;
    }

    public void save(Consumer<IFinishedRecipe> consumer) {
        save(consumer, BuiltInRegistries.ITEM.getKey(this.result));
    }

    public void save(Consumer<IFinishedRecipe> consumer, String id) {
        save(consumer, Identifier.parse(id));
    }

    public void save(Consumer<IFinishedRecipe> consumer, Identifier id) {
        consumer.accept(new IFinishedRecipe() {
            @Override
            public Identifier getId() {
                return id;
            }

            @Override
            public JsonObject serializeRecipe() {
                return new JsonObject();
            }
        });
    }
}
