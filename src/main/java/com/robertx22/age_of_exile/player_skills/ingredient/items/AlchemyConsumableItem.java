package com.robertx22.age_of_exile.player_skills.ingredient.items;

import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;

public class AlchemyConsumableItem extends BaseCraftedConsumableItem {

    static final Consumable CONSUMABLE = ItemUtils.drinkConsumable(2F);

    public AlchemyConsumableItem() {
        super(PlayerSkillEnum.ALCHEMY, new Item.Properties()
            .component(DataComponents.CONSUMABLE, CONSUMABLE));
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
