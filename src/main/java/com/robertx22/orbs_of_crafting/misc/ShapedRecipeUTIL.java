package com.robertx22.orbs_of_crafting.misc;

import com.robertx22.forge_compat.data.EnchantedItemTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import com.robertx22.forge_compat.data.ShapedRecipeBuilder;
import net.minecraft.world.level.ItemLike;

public class ShapedRecipeUTIL {
    public static ShapedRecipeBuilder of(ItemLike item, int count) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item, count)
                .unlockedBy("player_level", (Object) null);

    }
}
