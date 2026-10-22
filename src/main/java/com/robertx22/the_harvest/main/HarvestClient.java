package com.robertx22.the_harvest.main;

import com.robertx22.library_of_exile.main.ApiForgeEvents;
import com.robertx22.library_of_exile.tooltip.ExileTooltipUtils;
import com.robertx22.the_harvest.item.HarvestItemNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.robertx22.forge_compat.event.entity.player.ItemTooltipEvent;

public class HarvestClient {

    public static void init() {


        ApiForgeEvents.registerForgeEvent(ItemTooltipEvent.class, event -> {
            if (event.getItemStack().is(HarvestEntries.HARVEST_MAP_ITEM.get())) {
                ItemStack stack = event.getItemStack();

                if (HarvestItemNbt.HARVEST_MAP.has(stack)) {
                    var map = HarvestItemNbt.HARVEST_MAP.loadFrom(stack);

                    var tip = event.getToolTip();

                    tip.add(Component.empty());

                    tip.add(Component.empty());

                    if (map.relic) {
                        tip.add(HarvestWords.RELIC_MAP.get().withStyle(ChatFormatting.LIGHT_PURPLE));
                    }

                    tip.add(HarvestWords.MAP_ITEM_USE_INFO.get().withStyle(ChatFormatting.BLUE));

                    ExileTooltipUtils.removeBlankLines(tip, ExileTooltipUtils.RemoveOption.ONLY_DOUBLE_BLANK_LINES);

                }
            }
        });
    }
}
