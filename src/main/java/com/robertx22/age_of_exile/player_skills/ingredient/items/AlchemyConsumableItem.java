package com.robertx22.age_of_exile.player_skills.ingredient.items;

import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

public class AlchemyConsumableItem extends BaseCraftedConsumableItem {
    public AlchemyConsumableItem() {
        super(PlayerSkillEnum.ALCHEMY);
    }

    @Override
    public String locNameForLangFile() {

        return "Brewed Potion";
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

}
