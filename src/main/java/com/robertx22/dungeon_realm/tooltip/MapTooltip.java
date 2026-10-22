package com.robertx22.dungeon_realm.tooltip;

import com.robertx22.dungeon_realm.item.DungeonItemMapData;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.tooltip.TooltipBuilder;
import com.robertx22.library_of_exile.tooltip.TooltipItem;
import com.robertx22.library_of_exile.tooltip.order.ExileTooltipPart;
import com.robertx22.library_of_exile.tooltip.order.TooltipOrder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public class MapTooltip extends TooltipItem {
    public static MapTooltip DEFAULT = new MapTooltip(ItemStack.EMPTY, null);

    public ItemStack stack;
    public DungeonItemMapData data;


    public MapTooltip(ItemStack stack, DungeonItemMapData data) {
        super("dungeon_map");
        this.stack = stack;
        this.data = data;
    }

    public static List<Component> getTooltip(ItemStack stack) {

        var map = DungeonItemNbt.DUNGEON_MAP.loadFrom(stack);

        if (map == null) {
            return Arrays.asList();
        }

        TooltipBuilder<MapTooltip> b = new TooltipBuilder<>(new MapTooltip(stack, map));

        if (map.uber) {
            b.add(x -> {
                return new ExileTooltipPart(TooltipOrder.LATE, DungeonWords.MAP_HAS_UBER_ARENA.get().withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            });
        }
        b.add(x -> {
            return new ExileTooltipPart(TooltipOrder.LAST, Arrays.asList(
                    DungeonWords.MAP_ITEM_DESC.get().withStyle(ChatFormatting.BLUE),
                    DungeonWords.MAP_ITEM_USE_INFO.get().withStyle(ChatFormatting.BLUE)
            ));
        });


        return b.build();
    }
}
