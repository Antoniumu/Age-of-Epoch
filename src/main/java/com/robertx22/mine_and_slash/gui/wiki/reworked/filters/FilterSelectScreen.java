package com.robertx22.mine_and_slash.gui.wiki.reworked.filters;

import com.robertx22.mine_and_slash.gui.wiki.reworked.NewWikiScreen;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.network.chat.Component;

public class FilterSelectScreen extends Screen {


    private FilterEntryList list;
    public NewWikiScreen last;

    public FilterGroupButton button;

    public FilterSelectScreen(FilterGroupButton filterGroupButton, NewWikiScreen screen, GroupFilterType type) {
        super(Gui.CHOOSE_FILTER.locName());
        this.type = type;
        this.last = screen;
        this.button = filterGroupButton;
    }


    public void tick() {
    }

    public GroupFilterType type;

    protected void init() {
        this.list = new FilterEntryList(this, this.minecraft, this.width, this.height, 48, this.height - 64, 36);
        this.addWidget(this.list);
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    public void onClose() {
        this.minecraft.setScreen(this.last);
    }


    public void extractRenderState(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.list.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        com.robertx22.forge_compat.client.GuiBlitHelper.drawCenteredString(pGuiGraphics, this.font, this.title, this.width / 2, 8, 16777215);
        super.extractRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }


    public void removed() {


    }

}