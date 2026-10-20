package com.robertx22.age_of_exile.database.data.currency.base;

import com.robertx22.age_of_exile.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.BaseLocRequirement;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.LocReqContext;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.Rarity;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.ItemType;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocDesc;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TooltipUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.ItemDefault;
import com.robertx22.library_of_exile.registry.ExileRegistry;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IWeighted;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
public abstract class CurrencyItem extends Item implements ExileRegistry<CurrencyItem>, IRarity, ICurrencyItemEffect, IWeighted, IAutoLocDesc, IAutoLocName, IAutoModel {

    public ItemType itemTypesUsableOn = ItemType.GEAR;

    public static ChatFormatting nameColor = ChatFormatting.RED;

    public abstract String GUID();

    public CurrencyItem(String name) {
        super(new ItemDefault().stacksTo(64)
            .tab(CreativeTabs.GemRuneCurrency));

    }

    public abstract int getTier();

    @Override
    public void generateModel(ItemModelManager manager) {
        manager.generated(this);
    }

    @Override
    public final boolean canItemBeModified(LocReqContext context) {

        if (this.itemTypesUsableOn.isType(context.stack) == false) {
            return false;
        }

        if (context.isGear()) {

            GearItemData gear = (GearItemData) context.data;

            if (this.getInstability() > 0) {

                if (gear.getInstability() >= ServerContainer.get().MAX_INSTABILITY.get()) {
                    return false;
                }
            }
        }
        for (BaseLocRequirement req : requirements()) {
            if (req.isNotAllowed(context)) {
                return false;
            }

        }
        return true;
    }

    @Override
    public AutoLocGroup locDescGroup() {
        return AutoLocGroup.Currency_Items;
    }

    @Override
    public String locDescLangFileGUID() {
        return BuiltInRegistries.ITEM.getKey(this)
            .toString() + ".desc";
    }

    public abstract int getWeight();

    @Override
    public int Weight() {
        return getWeight();
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Currency_Items;
    }

    @Override
    public String locNameLangFileGUID() {
        return BuiltInRegistries.ITEM.getKey(this)
            .toString();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip,
                                TooltipFlag flagIn) {

        tooltip.add(
            this.locDesc()
                .withStyle(ChatFormatting.YELLOW));

        tooltip.add(ItemType.getTooltipString(this.itemTypesUsableOn));

        TooltipUtils.addEmpty(tooltip);

        if (this.getInstability() > 0) {
            tooltip.add(Words.Instability.locName()
                .withStyle(ChatFormatting.RED)
                .append(": " + getInstability()));

        }

        if (this.getBreakChance() > 0) {
            tooltip.add(Words.BreakChance.locName()
                .append(": " + (int) getBreakChance())
                .withStyle(ChatFormatting.RED));
        }
        tooltip.add(TooltipUtils.dragOntoGearToUse());

        TooltipUtils.addEmpty(tooltip);

    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.CURRENCY_ITEMS;
    }

    @Override
    public String getRarityRank() {
        return IRarity.UNCOMMON;
    }

    @Override
    public Rarity getRarity() {
        return ExileDB.MobRarities()
            .get(getRarityRank());
    }

}
