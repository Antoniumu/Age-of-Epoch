package com.robertx22.age_of_exile.vanilla_mc.items.misc;

import com.robertx22.age_of_exile.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.currency.base.IShapelessRecipe;
import com.robertx22.age_of_exile.database.data.level_ranges.LevelRange;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.ProfessionItems;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TooltipUtils;
import com.robertx22.library_of_exile.registry.IWeighted;
import net.minecraft.world.item.TooltipFlag;
import com.robertx22.forge_compat.data.ShapelessRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
public class SalvagedDustItem extends Item implements IAutoLocName, IWeighted, IAutoModel, IShapelessRecipe {

    String name;
    public SkillItemTier tier;
    public LevelRange range;

    public int durabilityRepair;

    public SalvagedDustItem(String name, SkillItemTier tier, LevelRange range, int durabilityRepair) {
        super(new Properties().stacksTo(64));
        this.name = name;
        this.tier = tier;
        this.range = range;
        this.durabilityRepair = durabilityRepair;
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {

        tooltip.add(Words.CreatedInSalvageStation.locName());

        tooltip.add(TooltipUtils.gearTier(tier.getDisplayTierNumber()));

        tooltip.add(Component.literal(""));

        tooltip.add(Component.literal("Восстанавливает " + durabilityRepair + " ед. прочности."));
        tooltip.add(Component.literal("Менее эффективно на снаряжении более высокого ранга."));

        tooltip.add(TooltipUtils.dragOntoGearToUse());

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

    @Override
    public String locNameForLangFile() {
        return name;
    }

    @Override
    public String GUID() {
        return "mat/salvage/salvage" + tier.tier;
    }

    @Override
    public int Weight() {
        return 100;
    }

    @Override
    public void generateModel(ItemModelManager manager) {
        manager.generated(this);
    }

    @Override
    public ShapelessRecipeBuilder getRecipe() {
        // de-craft recipe into lower tiers
        if (tier.tier < 1) {
            return null;
        }

        Item output = ProfessionItems.SALVAGED_ESSENCE_MAP.values()
            .stream()
            .filter(x -> x.get().tier.tier == tier.tier - 1)
            .findAny()
            .get()
            .get();

        return ShapelessRecipeBuilder.shapeless(output, 3)
            .requires(this, 1)
            .unlockedBy("player_level", trigger());

    }
}
