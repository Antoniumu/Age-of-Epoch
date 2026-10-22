package com.robertx22.ancient_obelisks.main;

import com.robertx22.ancient_obelisks.item.ObeliskItemNbt;
import com.robertx22.library_of_exile.main.ApiForgeEvents;
import com.robertx22.library_of_exile.tooltip.ExileTooltipUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.robertx22.forge_compat.event.entity.player.ItemTooltipEvent;

public class ObeliskClient {

    public static void init() {


        ApiForgeEvents.registerForgeEvent(ItemTooltipEvent.class, event -> {
            if (event.getItemStack().is(ObeliskEntries.OBELISK_MAP_ITEM.get())) {
                ItemStack stack = event.getItemStack();

                if (ObeliskItemNbt.OBELISK_MAP.has(stack)) {
                    var map = ObeliskItemNbt.OBELISK_MAP.loadFrom(stack);

                    var tip = event.getToolTip();

                    tip.add(Component.empty());

                    tip.addAll(map.getAffixesTooltip());

                    tip.add(Component.empty());

                    tip.addAll(map.getTierTooltip());

                    tip.add(Component.empty());

                    tip.addAll(map.spawnRateTooltip());
                    tip.addAll(map.getLootBonusTooltip());

                    tip.add(Component.empty());

                    if (map.relic) {
                        tip.add(ObeliskWords.RELIC_MAP.get().withStyle(ChatFormatting.LIGHT_PURPLE));
                    }

                    tip.add(ObeliskWords.MAP_ITEM_USE_INFO.get().withStyle(ChatFormatting.BLUE));

                    ExileTooltipUtils.removeBlankLines(tip, ExileTooltipUtils.RemoveOption.ONLY_DOUBLE_BLANK_LINES);

                }
            }
        });
    }
}
