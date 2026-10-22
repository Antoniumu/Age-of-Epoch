package com.robertx22.mine_and_slash.gui.wiki.reworked.filters;

import com.robertx22.mine_and_slash.gui.wiki.reworked.NewWikiScreen;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class FilterGroupButton extends AbstractButton {

    public static int WIDTH = 120;
    public static int HEIGHT = 30;

    NewWikiScreen screen;
    GroupFilterType filter;

    public FilterGroupButton(NewWikiScreen screen, GroupFilterType filter, int pX, int pY) {
        super(pX, pY, WIDTH, HEIGHT, filter.word.locName());
        this.filter = filter;
        this.screen = screen;
    }

    public void onPress() {
        Minecraft.getInstance().gui.setScreen(new FilterSelectScreen(this, screen, filter));
    }

    @Override
    public Component getMessage() {

        var cur = screen.getFilter(filter);

        if (cur == GroupFilterEntry.NONE) {
            return Words.PICK_SLOT.locName(filter.word.locName());
        } else {
            return screen.getFilter(filter).getName();
        }
    }


    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    @Override public void onPress(net.minecraft.client.input.InputWithModifiers in) { this.onPress(); }
}
