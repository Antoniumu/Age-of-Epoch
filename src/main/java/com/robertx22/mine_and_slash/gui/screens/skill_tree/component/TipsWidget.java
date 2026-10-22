package com.robertx22.mine_and_slash.gui.screens.skill_tree.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class TipsWidget extends AbstractWidget {
    public TipsWidget(int x, int y, int w, int h, net.minecraft.network.chat.Component tip) { super(x, y, w, h, tip); this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tip)); }
    public TipsWidget(int x, int y) {
        super(x, y, 20, 20, Component.empty());
    }

    public void render(GuiGraphicsExtractor gui, int mx, int my, float pt) {
        extractContents(gui, mx, my, pt);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor gui, int mx, int my, float pt) {
        gui.text(Minecraft.getInstance().font, Component.literal("[?]"), getX(), getY(), 0xFFFFFF55, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }
}
