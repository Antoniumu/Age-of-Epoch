package com.robertx22.library_of_exile.gui;

import com.robertx22.library_of_exile.main.Ref;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class HelpButton extends ImageButton {

    static Identifier ID = Identifier.fromNamespaceAndPath(Ref.MODID, "textures/gui/spell_help.png");

    public HelpButton(int x, int y) {
        super(x, y, 20, 20, 0, 0, 1, ID,
                action -> {

                });


    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        setTooltip(Tooltip.create(Component.literal("test button text")));
        super.extractWidgetRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }


/*
    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, 20, 20, x, y);
    }

 */

    // todo
    /*
    @Override
    public void renderToolTip(GuiGraphicsExtractor matrices, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY)) {
            com.robertx22.forge_compat.client.GuiBlitHelper.renderTooltip(GuiUtils, matrices, tooltip, mouseX, mouseY);
        }
    }

     */
}