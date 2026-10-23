package com.robertx22.age_of_exile.mmorpg.registers.common;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class SlashRecipeTypes {

    public static void init() {

    }

    static <T extends Recipe<?>> RecipeType<T> register(final String string) {
        return Registry.register(BuiltInRegistries.RECIPE_TYPE, (Identifier) SlashRef.id(string), new RecipeType<T>() {
            @Override
            public String toString() {
                return string;
            }
        });
    }
}
