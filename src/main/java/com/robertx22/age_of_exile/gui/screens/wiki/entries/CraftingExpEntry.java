package com.robertx22.age_of_exile.gui.screens.wiki.entries;

import com.robertx22.age_of_exile.gui.screens.wiki.WikiEntry;
import com.robertx22.library_of_exile.utils.CLOC;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class CraftingExpEntry extends WikiEntry {

    Item item;
    int exp;

    public CraftingExpEntry(Item item, int exp) {
        this.item = item;
        this.exp = exp;
    }

    @Override
    public ItemStack createMainStack() {
        ItemStack stack = new ItemStack(item);
        return stack;
    }

    @Override
    public List<Component> getExtraInfo() {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal("Опыт за создание: " + exp));
        return list;
    }

    @Override
    public String getName() {
        return cutName(CLOC.translate(Component.translatable(item.getDescriptionId()))) + " (" + exp + " опыта)";
    }

    @Override
    public ChatFormatting getFormat() {
        return ChatFormatting.RED;
    }
}

