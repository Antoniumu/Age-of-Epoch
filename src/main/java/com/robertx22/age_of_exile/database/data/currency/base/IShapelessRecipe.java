package com.robertx22.age_of_exile.database.data.currency.base;

import com.robertx22.forge_compat.data.CriterionTriggerInstance;
import com.robertx22.forge_compat.data.EnchantedItemTrigger;
import com.robertx22.forge_compat.data.ShapelessRecipeBuilder;

public interface IShapelessRecipe {

    ShapelessRecipeBuilder getRecipe();

    default CriterionTriggerInstance trigger() {
        return EnchantedItemTrigger.Instance.enchantedItem();
    }
}
