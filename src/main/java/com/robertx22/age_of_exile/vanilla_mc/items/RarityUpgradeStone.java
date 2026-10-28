package com.robertx22.age_of_exile.vanilla_mc.items;

import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.currency.base.ICurrencyItemEffect;
import com.robertx22.age_of_exile.database.data.currency.base.ModifyResult;
import com.robertx22.age_of_exile.database.data.currency.base.ResultItem;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.BaseLocRequirement;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.LocReqContext;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.library_of_exile.registry.IGUID;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import com.robertx22.forge_compat.common.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.List;

public class RarityUpgradeStone extends Item implements ICurrencyItemEffect, IGUID {

    public RarityUpgradeStone() {
        super(new Item.Properties().stacksTo(64));
    }

    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {

        if (true) {

            for (GearRarity rar : ExileDB.GearRarities()
                .getList()) {
                if (rar.canUpgradeRarityIfUpgradeLevelIsHighEnough()) {
                    stacks.add(rar.getRarityUpgradeStack());
                }
            }

        }
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {

        try {

            GearRarity rar = GearRarity.getRarityFromEssence(stack);

            if (rar != null) {
                tooltip.clear();
                tooltip.add(rar.locName()
                    .withStyle(rar.textFormatting())
                    .append(" ")
                    .append(Words.UpgradeStone.locName())
                    .withStyle(rar.textFormatting()));

                tooltip.add(Component.literal("Улучшает предмет +").append(String.valueOf(rar.upgrade_lvl_to_increase_rar)).append(" (").append(rar.locName())
                    .append(") до редкости ")
                    .append(rar.getHigherRarity()
                        .locName()));

                tooltip.add(Component.literal("Нажмите по предмету, чтобы улучшить"));

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String GUID() {
        return "rarity/upgrade_stone/base";
    }

    @Override
    public boolean canItemBeModified(LocReqContext context) {

        GearRarity rar = GearRarity.getRarityFromEssence(context.Currency);

        if (context.isGear()) {
            GearItemData gear = (GearItemData) context.data;
            if (rar == null || !rar.hasHigherRarity()) {
                return false;
            }
            // Камень работает только на предмете СВОЕЙ редкости и только если следующая
            // редкость существует. Без этой проверки камень uncommon на epic-предмете
            // уходил в upgradeToHigherRarity() -> getHigherRarity() == null -> NPE.
            if (gear.getRarity() == null || !gear.getRarity()
                .GUID()
                .equals(rar.GUID())) {
                return false;
            }
            if (gear.up.getUpgradeLevel() >= rar.upgrade_lvl_to_increase_rar) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ResultItem modifyItem(LocReqContext context) {
        if (context.effect.canItemBeModified(context)) {

            ItemStack copy = context.stack.copy();

            if (context.isGear()) {
                GearItemData gear = Gear.Load(copy);
                // Редкость бесдушной основы задаёт душа, а не камень: иначе основу
                // можно было бы «прокачать» до эпика с аффиксами и обойти правило
                // «у основы только базовые статы и common».
                if (gear == null || gear.isSoulless()) {
                    return new ResultItem(ItemStack.EMPTY, ModifyResult.NONE);
                }
                gear.upgradeToHigherRarity();
                Gear.Save(copy, gear);
            }

            return new ResultItem(copy, ModifyResult.SUCCESS);
        } else {
            return new ResultItem(ItemStack.EMPTY, ModifyResult.NONE);
        }

    }

    @Override
    public ItemStack internalModifyMethod(LocReqContext ctx, ItemStack stack, ItemStack currency) {
        return null;
    }

    @Override
    public List<BaseLocRequirement> requirements() {
        return Arrays.asList(); // todo
    }

}

