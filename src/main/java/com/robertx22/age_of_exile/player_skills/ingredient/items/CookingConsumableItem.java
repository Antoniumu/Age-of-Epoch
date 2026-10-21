package com.robertx22.age_of_exile.player_skills.ingredient.items;

import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;

public class CookingConsumableItem extends BaseCraftedConsumableItem {

    static final Consumable CONSUMABLE = ItemUtils.eatConsumable(2F);

    public CookingConsumableItem() {
        super(PlayerSkillEnum.COOKING, new Item.Properties()
            .component(DataComponents.CONSUMABLE, CONSUMABLE));
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
