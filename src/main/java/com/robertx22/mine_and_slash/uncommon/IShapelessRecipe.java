package com.robertx22.mine_and_slash.uncommon;


import com.robertx22.forge_compat.data.EnchantedItemTrigger;
import com.robertx22.forge_compat.data.ShapelessRecipeBuilder;

public interface IShapelessRecipe {

    ShapelessRecipeBuilder getRecipe();

    default Object trigger() {
        return null;
    }
}



