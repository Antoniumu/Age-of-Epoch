package com.robertx22.mine_and_slash.gui.wiki.reworked.filters;

import com.robertx22.mine_and_slash.gui.wiki.reworked.NewWikiScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClearFilterButton extends AbstractButton {

    public static int HEIGHT = 30;
    static Identifier tex = SlashRef.guiId("bestiary/cancel");

    NewWikiScreen screen;
    GroupFilterType filter;

    public ClearFilterButton(NewWikiScreen screen, GroupFilterType filter, int pX, int pY) {
        super(pX, pY, HEIGHT, HEIGHT, Component.literal(""));
        this.filter = filter;
        this.screen = screen;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        
        Identifier tex = SlashRef.guiId("bestiary/cancel");


        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int off = (HEIGHT - 16) / 2;
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, tex, getX() + off, getY() + off, 16, 16, 16, 16, 16, 16);

    }


    public void onPress() {
        screen.setFilterGroup(filter, GroupFilterEntry.NONE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    @Override public void onPress(net.minecraft.client.input.InputWithModifiers in) { this.onPress(); }
}
