package com.robertx22.age_of_exile.player_skills.items;

import com.robertx22.age_of_exile.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;

import net.minecraft.core.registries.BuiltInRegistries;
public abstract class TieredItem extends Item implements IAutoLocName, IAutoModel {

    public SkillItemTier tier;

    public TieredItem(SkillItemTier tier) {
        super(new Item.Properties().tab(CreativeTabs.Professions));
        this.tier = tier;
    }

    public TieredItem(SkillItemTier tier, Item.Properties settings) {
        super(settings.tab(CreativeTabs.Professions));
        this.tier = tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId()).withStyle(tier.format);
    }

    @Override
    public void generateModel(ItemModelManager manager) {
        manager.generated(this);
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Misc;
    }

    @Override
    public String locNameLangFileGUID() {
        return BuiltInRegistries.ITEM.getKey(this)
            .toString();
    }

}

