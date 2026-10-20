package com.robertx22.age_of_exile.database.data.currency.base;

import net.minecraft.advancements.critereon.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.EnchantedItemTrigger;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;

public interface IShapelessRecipe {

    ShapelessRecipeBuilder getRecipe();

    default CriterionTriggerInstance trigger() {
        return EnchantedItemTrigger.Instance.enchantedItem();
    }
}



