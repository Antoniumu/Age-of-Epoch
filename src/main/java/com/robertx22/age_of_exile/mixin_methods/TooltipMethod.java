package com.robertx22.age_of_exile.mixin_methods;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.currency.base.ICurrencyItemEffect;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftingProcessData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData;
import com.robertx22.age_of_exile.player_skills.items.TieredItem;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.unit.Unit;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.ICommonDataItem;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TooltipUtils;
import com.robertx22.library_of_exile.registry.Database;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import com.robertx22.forge_compat.common.Tier;
public class TooltipMethod {
    public static void getTooltip(ItemStack stack, Player entity, TooltipFlag tooltipContext, CallbackInfoReturnable<List<Component>> list) {

        List<Component> tooltip = list.getReturnValue();

        boolean addCurrencyTooltip = stack
            .getItem() instanceof ICurrencyItemEffect;

        Player player = Minecraft.getInstance().player;

        try {

            CraftingProcessData pdata = StackSaving.CRAFT_PROCESS.loadFrom(stack);

            if (pdata != null) {
                pdata.makeTooltip(stack, tooltip);
                return;
            }

            if (StackSaving.INGREDIENTS.has(stack)) {
                IngredientData data = StackSaving.INGREDIENTS.loadFrom(stack);
                if (data != null) {
                    data.makeTooltip(tooltip);
                    return;
                }
            }

            if (stack.getItem() instanceof TieredItem) {
                TieredItem tier = (TieredItem) stack.getItem();

                tooltip.add(Component.literal("Tier " + tier.tier.getDisplayTierNumber()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            if (Screen.hasControlDown()) {
                GearItemData gear = Gear.Load(stack);
                if (gear != null) {
                    return;
                }
            }

            if (player == null || player.level() == null) {
                return;
            }

            EntityData unitdata = Load.Unit(player);

            if (unitdata == null) {
                return;
            }

            Unit unit = unitdata.getUnit();

            if (unit == null) {
                return;
            }
            if (!Database.areDatapacksLoaded(player.level())) {
                return;
            }

            com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipContext ctx = new com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipContext(stack, tooltip, unitdata);

            boolean hasdata = false;

            if (StackNBT.has(stack)) {

                ICommonDataItem data = ICommonDataItem.load(stack);

                if (data != null) {
                    data.BuildTooltip(ctx);
                    hasdata = true;
                }

                MutableComponent broken = TooltipUtils.itemBrokenText(stack, data);
                if (broken != null) {
                    tooltip.add(broken);
                }

            }

            if (!hasdata) {

                GearSlot slot = GearSlot.getSlotOf(stack.getItem());

                if (slot != null) {
                    tooltip.add(TooltipUtils.gearSlot(slot));
                }
            }

            if (addCurrencyTooltip) {
                ICurrencyItemEffect currency = (ICurrencyItemEffect) stack
                    .getItem();
                currency.addToTooltip(tooltip);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
