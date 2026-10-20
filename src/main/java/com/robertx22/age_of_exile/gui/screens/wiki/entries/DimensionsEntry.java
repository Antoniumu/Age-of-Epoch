package com.robertx22.age_of_exile.gui.screens.wiki.entries;

import com.robertx22.age_of_exile.database.data.DimensionConfig;
import com.robertx22.age_of_exile.gui.screens.wiki.WikiEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Mob;
public class DimensionsEntry extends WikiEntry {

    DimensionConfig dim;

    public DimensionsEntry(DimensionConfig dim) {
        this.dim = dim;
    }

    @Override
    public ItemStack createMainStack() {
        ItemStack stack = new ItemStack(Items.MAP);
        return stack;
    }

    @Override
    public List<Component> getExtraInfo() {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal("Mob levels: " + dim.min_lvl + "-" + dim.max_lvl));
        return list;
    }

    @Override
    public String getName() {
        return dim.dimension_id;
    }

    @Override
    public ChatFormatting getFormat() {
        return ChatFormatting.AQUA;
    }
}
