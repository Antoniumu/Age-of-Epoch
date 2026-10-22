package com.robertx22.forge_compat.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ShapelessRecipeBuilder implements RecipeBuilder {
    private final Item result;
    private final int count;
    private final List<String> ingredients = new ArrayList<>();
    private String group;

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

    public static ShapelessRecipeBuilder shapeless(Object category, ItemLike result) {
        return new ShapelessRecipeBuilder(result, 1);
    }

    public static ShapelessRecipeBuilder shapeless(Object category, ItemLike result, int count) {
        return new ShapelessRecipeBuilder(result, count);
    }

    public ShapelessRecipeBuilder requires(TagKey<Item> tag) {
        this.ingredients.add("#" + tag.identifier().toString());
        return this;
    }

    public ShapelessRecipeBuilder requires(ItemLike item) {
        return requires(item, 1);
    }

    public ShapelessRecipeBuilder requires(ItemLike item, int quantity) {
        String id = BuiltInRegistries.ITEM.getKey(item.asItem()).toString();
        for (int i = 0; i < quantity; i++) {
            this.ingredients.add(id);
        }
        return this;
    }

    public ShapelessRecipeBuilder requires(Ingredient ingredient) {
        return this;
    }

    public ShapelessRecipeBuilder requires(Ingredient ingredient, int quantity) {
        return this;
    }

    public ShapelessRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public ShapelessRecipeBuilder unlockedBy(String name, Object criterion) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    @Override
    public void save(Consumer<IFinishedRecipe> consumer) {
        Identifier id = BuiltInRegistries.ITEM.getKey(this.result);
        save(consumer, id);
    }

    @Override
    public void save(Consumer<IFinishedRecipe> consumer, Identifier id) {
        consumer.accept(new IFinishedRecipe() {
            @Override
            public Identifier getId() {
                return id;
            }

            @Override
            public JsonObject serializeRecipe() {
                JsonObject root = new JsonObject();
                root.addProperty("type", "minecraft:crafting_shapeless");
                root.addProperty("category", "misc");
                if (group != null && !group.isEmpty()) {
                    root.addProperty("group", group);
                }
                JsonArray arr = new JsonArray();
                for (String ing : ingredients) {
                    arr.add(ing);
                }
                root.add("ingredients", arr);
                JsonObject resObj = new JsonObject();
                resObj.addProperty("id", BuiltInRegistries.ITEM.getKey(result).toString());
                resObj.addProperty("count", count);
                root.add("result", resObj);
                return root;
            }
        });
    }
}
