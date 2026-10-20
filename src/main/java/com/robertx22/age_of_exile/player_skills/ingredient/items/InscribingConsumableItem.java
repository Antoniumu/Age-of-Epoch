package com.robertx22.age_of_exile.player_skills.ingredient.items;

import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public class InscribingConsumableItem extends BaseCraftedConsumableItem {

    public InscribingConsumableItem() {
        super(PlayerSkillEnum.INSCRIBING);
        this.isAOE = true;
    }

    @Override
    public String locNameForLangFile() {
        return "Enchanted Scroll";
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

}
