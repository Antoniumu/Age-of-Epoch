package com.robertx22.age_of_exile.gui.screens.wiki;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public abstract class WikiEntry {

    public abstract ItemStack createMainStack();

    public abstract List<Component> getExtraInfo();

    public abstract String getName();

    public abstract ChatFormatting getFormat();

    public String cutName(String str) {
        return StringUtils.abbreviate(str, 23);

    }
}
