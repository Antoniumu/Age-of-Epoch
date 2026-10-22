package com.robertx22.forge_compat.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public class ShapedRecipeBuilder implements net.minecraft.data.recipes.RecipeBuilder {
    public static ShapedRecipeBuilder shaped(net.minecraft.data.recipes.RecipeCategory cat, net.minecraft.world.level.ItemLike out) { return shaped(out); }
    public static ShapedRecipeBuilder shaped(net.minecraft.data.recipes.RecipeCategory cat, net.minecraft.world.level.ItemLike out, int count) { return shaped(out, count); }
    public ShapedRecipeBuilder unlockedBy(String name, Object criterion) { return this; }
    @Override public ShapedRecipeBuilder unlockedBy(String name, net.minecraft.advancements.triggers.Criterion<?> criterion) { return this; }
    @Override public ShapedRecipeBuilder group(String group) { return this; }
    @Override public net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> defaultId() { return null; }
    @Override public void save(net.minecraft.data.recipes.RecipeOutput out, net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> id) {}

    private final Item result;
    private final int count;

    public ShapedRecipeBuilder(ItemLike result, int count) {
        this.result = result.asItem();
        this.count = count;
    }

    public static ShapedRecipeBuilder shaped(ItemLike result) {
        return new ShapedRecipeBuilder(result, 1);
    }

    public static ShapedRecipeBuilder shaped(ItemLike result, int count) {
        return new ShapedRecipeBuilder(result, count);
    }

    public ShapedRecipeBuilder define(Character symbol, TagKey<Item> tag) {
        return this;
    }

    public ShapedRecipeBuilder define(Character symbol, ItemLike item) {
        return this;
    }

    public ShapedRecipeBuilder define(Character symbol, Ingredient ingredient) {
        return this;
    }

    public ShapedRecipeBuilder pattern(String pattern) {
        return this;
    }

    public ShapedRecipeBuilder unlockedBy(String name, CriterionTriggerInstance criterion) {
        return this;
    }

    public ShapedRecipeBuilder group(String group) {
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
