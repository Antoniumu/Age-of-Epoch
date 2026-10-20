package com.robertx22.age_of_exile.player_skills.ingredient.items;

import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

public class CookingConsumableItem extends BaseCraftedConsumableItem {

    public CookingConsumableItem() {
        super(PlayerSkillEnum.COOKING);
    }

    @Override
    public String locNameForLangFile() {
        return "Cooked Meal";
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

}
