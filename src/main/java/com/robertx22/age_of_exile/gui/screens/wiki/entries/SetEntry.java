package com.robertx22.age_of_exile.gui.screens.wiki.entries;

import com.robertx22.age_of_exile.database.data.set.GearSet;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.gui.screens.wiki.WikiEntry;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.library_of_exile.utils.CLOC;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class SetEntry extends WikiEntry {

    GearSet set;

    public SetEntry(GearSet set) {
        this.set = set;
    }

    @Override
    public ItemStack createMainStack() {
        return new ItemStack(Items.PAPER);
    }

    @Override
    public List<Component> getExtraInfo() {

        List<Component> list = new ArrayList<>();

        list.add(Component.literal("Set Pieces: ").withStyle(ChatFormatting.GREEN));
        ExileDB.UniqueGears()
            .getList()
            .forEach(x -> {
                if (x.getSet()
                    .equals(set)) {
                    list.add(x.locName()
                        .append(" ")
                        .append(x.getBaseGear()
                            .locName()));
                }
            });

        list.add(Component.literal(""));

        TooltipInfo info = new TooltipInfo();

        list.addAll(set.GetTooltipString(info));

        return list;
    }

    @Override
    public String getName() {
        return CLOC.translate(set.locName());
    }

    @Override
    public ChatFormatting getFormat() {
        return ChatFormatting.GREEN;
    }
}
