package com.robertx22.age_of_exile.database.data.currency.base;

import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.BaseLocRequirement;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.LocReqContext;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TooltipUtils;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

public interface ICurrencyItemEffect {

    public abstract ItemStack internalModifyMethod(LocReqContext ctx, ItemStack stack, ItemStack currency);

    public abstract List<BaseLocRequirement> requirements();

    public default float getInstability() {
        return 0;
    }

    public default float getBreakChance() {
        return 0;
    }

    default boolean canItemBeModified(LocReqContext context) {

        if (context.isGear()) {
            GearItemData gear = (GearItemData) context.data;
            if (gear.getInstability() >= ServerContainer.get().MAX_INSTABILITY.get()) {
                return false;
            }
        }

        for (BaseLocRequirement req : requirements()) {
            if (req.isNotAllowed(context)) {
                return false;
            }

        }
        return true;
    }

    public default void addToTooltip(List<Component> tooltip) {

        if (Screen.hasShiftDown()) {
            tooltip.add(TooltipUtils.color(ChatFormatting.RED, Words.Requirements.locName()
                .append(": ")));

            for (BaseLocRequirement req : requirements()) {
                tooltip.add(TooltipUtils.color(ChatFormatting.RED,
                    Component.literal(" * ").append(req.getText())
                ));
            }
        } else {
            tooltip.add(TooltipUtils.color(ChatFormatting.GREEN, Words.PressShiftForRequirements.locName()));

        }
    }

    default ResultItem modifyItem(LocReqContext context) {
        if (context.effect.canItemBeModified(context)) {

            // Запоминаем, была ли вещь бесдушной основой: валюта может менять её
            // редкость и аффиксы, но не может снять слабость основы. Силу даёт
            // только душа — иначе основа в обход души становилась бы полным предметом.
            GearItemData before = context.isGear() ? (GearItemData) context.data : null;
            boolean wasSoulless = before != null && before.isSoulless();
            boolean wasWholeItem = wasSoulless && before.isWholeItemBase();
            float baseQuality = wasSoulless ? before.bq : 1F;

            ItemStack copy = context.stack.copy();
            copy = context.effect.internalModifyMethod(context, copy, context.Currency);

            if (context.isGear()) {
                if (context.effect.getInstability() > 0) {
                    GearItemData gear = Gear.Load(copy);
                    gear.setInstability(gear.getInstability() + context.effect.getInstability());
                    Gear.Save(copy, gear);
                }
            }

            if (wasSoulless) {
                GearItemData after = Gear.Load(copy);
                // Валюта могла подменить предмет (например, сломать его) — тогда
                // данных нет и восстанавливать нечего.
                if (after != null && after.gear_type != null && !after.gear_type.isEmpty()) {
                    after.sl = true;
                    after.wi = wasWholeItem;
                    after.bq = baseQuality;
                    if (after.item_id == null || after.item_id.isEmpty()) {
                        after.item_id = com.robertx22.age_of_exile.saveclasses.stat_soul.SoullessGear.itemId(copy);
                    }
                    Gear.Save(copy, after);
                }
            }

            if (RandomUtils.roll(context.effect.getBreakChance())) {
                return new ResultItem(new ItemStack(Items.GUNPOWDER), ModifyResult.BREAK);
            }

            return new ResultItem(copy, ModifyResult.SUCCESS);
        } else {
            return new ResultItem(ItemStack.EMPTY, ModifyResult.NONE);
        }

    }

}
