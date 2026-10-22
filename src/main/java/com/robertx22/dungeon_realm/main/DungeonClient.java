package com.robertx22.dungeon_realm.main;

import com.robertx22.dungeon_realm.item.relic.RelicTooltip;
import com.robertx22.dungeon_realm.tooltip.MapTooltip;
import com.robertx22.library_of_exile.database.relic.relic_type.RelicItem;
import com.robertx22.library_of_exile.main.ApiForgeEvents;
import net.minecraft.world.item.ItemStack;
import com.robertx22.forge_compat.event.entity.player.ItemTooltipEvent;

public class DungeonClient {

    public static void init() {


        ApiForgeEvents.registerForgeEvent(ItemTooltipEvent.class, event -> {
            try {
                if (event.getItemStack().is(DungeonEntries.DUNGEON_MAP_ITEM.get())) {
                    ItemStack stack = event.getItemStack();
                    event.getToolTip().addAll(MapTooltip.getTooltip(stack));
                }
                if (event.getItemStack().getItem() instanceof RelicItem) {
                    ItemStack stack = event.getItemStack();
                    event.getToolTip().addAll(RelicTooltip.getTooltip(stack));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
