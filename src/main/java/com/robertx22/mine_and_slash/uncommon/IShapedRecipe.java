package com.robertx22.mine_and_slash.uncommon;


import com.robertx22.forge_compat.data.EnchantedItemTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import com.robertx22.forge_compat.data.ShapedRecipeBuilder;
import net.minecraft.world.level.ItemLike;

public interface IShapedRecipe {
    ShapedRecipeBuilder getRecipe();

    default ShapedRecipeBuilder shaped(ItemLike pro) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, pro, 1);
    }

    default ShapedRecipeBuilder shaped(ItemLike pro, int i) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, pro, i);
    }

    default Object trigger() {
        return null;
    }

}



