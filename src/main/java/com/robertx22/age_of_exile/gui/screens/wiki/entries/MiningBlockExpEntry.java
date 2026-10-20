package com.robertx22.age_of_exile.gui.screens.wiki.entries;

import com.robertx22.age_of_exile.gui.screens.wiki.WikiEntry;
import com.robertx22.library_of_exile.utils.CLOC;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class MiningBlockExpEntry extends WikiEntry {

    Block block;
    int exp;

    public MiningBlockExpEntry(Block block, int exp) {
        this.block = block;
        this.exp = exp;
    }

    @Override
    public ItemStack createMainStack() {
        ItemStack stack = new ItemStack(block);
        return stack;
    }

    @Override
    public List<Component> getExtraInfo() {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal("Exp Given " + exp));
        return list;
    }

    @Override
    public String getName() {
        return CLOC.translate(block.getName()) + ", " + exp + " Exp";
    }

    @Override
    public ChatFormatting getFormat() {
        return ChatFormatting.YELLOW;
    }
}
