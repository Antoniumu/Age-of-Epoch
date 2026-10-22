package com.robertx22.dungeon_realm.item.relic;

import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.database.relic.stat.RelicMod;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.tooltip.TooltipBuilder;
import com.robertx22.library_of_exile.tooltip.TooltipItem;
import com.robertx22.library_of_exile.tooltip.order.ExileTooltipPart;
import com.robertx22.library_of_exile.tooltip.order.TooltipOrder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RelicTooltip extends TooltipItem {
    public static RelicTooltip DEFAULT = new RelicTooltip(ItemStack.EMPTY, null);

    public ItemStack stack;
    public RelicItemData data;


    public RelicTooltip(ItemStack stack, RelicItemData data) {
        super("relic");
        this.stack = stack;
        this.data = data;
    }

    public static List<Component> getTooltip(ItemStack stack) {

        var data = DungeonItemNbt.RELIC.loadFrom(stack);

        if (data == null) {
            return Arrays.asList();
        }

        TooltipBuilder<RelicTooltip> b = new TooltipBuilder<>(new RelicTooltip(stack, data));

        b.add(x -> {
            List<MutableComponent> all = new ArrayList<>();

            for (RelicAffixData affix : data.affixes) {
                for (RelicMod mod : affix.get().mods) {
                    var ex = mod.toExact(affix.p);
                    all.add(ex.getStat().getTooltip(ex.num));
                }
            }
            return new ExileTooltipPart(TooltipOrder.FIRST, all);
        });

        b.add(x -> {
            return new ExileTooltipPart(TooltipOrder.MIDDLE, data.getType().getTranslation(TranslationType.NAME).getTranslatedName().withStyle(ChatFormatting.GOLD));
        });
        b.add(x -> {
            return new ExileTooltipPart(TooltipOrder.MIDDLE, DungeonWords.RELIC_MAX_COUNT.get(data.getType().max_equipped).withStyle(ChatFormatting.BLUE));
        });

        b.add(x -> {
            return new ExileTooltipPart(TooltipOrder.LATE, data.getRarity().getTranslation(TranslationType.NAME).getTranslatedName().withStyle(data.getRarity().base_data.color()));
        });

        b.add(x -> {
            return new ExileTooltipPart(TooltipOrder.LAST, Arrays.asList(
                    DungeonWords.RELIC_ITEM_INFO.get().withStyle(ChatFormatting.BLUE),
                    DungeonWords.RELIC_ITEM_INFO2.get().withStyle(ChatFormatting.BLUE)
            ));
        });


        return b.build();
    }
}
