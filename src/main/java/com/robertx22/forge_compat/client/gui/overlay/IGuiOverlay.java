package com.robertx22.forge_compat.client.gui.overlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface IGuiOverlay {
    void render(ForgeGui gui, GuiGraphicsExtractor guiGraphics, float partialTick, int screenWidth, int screenHeight);
}
