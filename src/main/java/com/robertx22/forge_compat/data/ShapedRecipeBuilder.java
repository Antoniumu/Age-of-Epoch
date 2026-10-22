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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ShapedRecipeBuilder implements RecipeBuilder {
    private final Item result;
    private final int count;
    private final Map<Character, String> key = new LinkedHashMap<>();
    private final List<String> rows = new ArrayList<>();
    private String group;

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

    public static ShapedRecipeBuilder shaped(Object category, ItemLike result) {
        return new ShapedRecipeBuilder(result, 1);
    }

    public static ShapedRecipeBuilder shaped(Object category, ItemLike result, int count) {
        return new ShapedRecipeBuilder(result, count);
    }

    public ShapedRecipeBuilder define(Character symbol, TagKey<Item> tag) {
        this.key.put(symbol, "#" + tag.location().toString());
        return this;
    }

    public ShapedRecipeBuilder define(Character symbol, ItemLike item) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item.asItem());
        this.key.put(symbol, itemId.toString());
        return this;
    }

    public ShapedRecipeBuilder define(Character symbol, Ingredient ingredient) {
        return this;
    }

    public ShapedRecipeBuilder pattern(String pattern) {
        this.rows.add(pattern);
        return this;
    }

    public ShapedRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public ShapedRecipeBuilder unlockedBy(String name, Object criterion) {
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
                root.addProperty("type", "minecraft:crafting_shaped");
                root.addProperty("category", "misc");
                if (group != null && !group.isEmpty()) {
                    root.addProperty("group", group);
                }
                JsonObject keyObj = new JsonObject();
                for (Map.Entry<Character, String> e : key.entrySet()) {
                    keyObj.addProperty(String.valueOf(e.getKey()), e.getValue());
                }
                root.add("key", keyObj);
                JsonArray patArr = new JsonArray();
                for (String r : rows) {
                    patArr.add(r);
                }
                root.add("pattern", patArr);
                JsonObject resObj = new JsonObject();
                resObj.addProperty("id", BuiltInRegistries.ITEM.getKey(result).toString());
                resObj.addProperty("count", count);
                root.add("result", resObj);
                return root;
            }
        });
    }
}
