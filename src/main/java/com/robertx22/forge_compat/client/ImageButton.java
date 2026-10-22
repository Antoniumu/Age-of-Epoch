package com.robertx22.forge_compat.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ImageButton extends Button {

    protected final Identifier resourceLocation;
    protected final int xTexStart;
    protected final int yTexStart;
    protected final int yDiffTex;
    protected final int textureWidth;
    protected final int textureHeight;

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex,
                       Identifier resourceLocation, OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, yDiffTex, resourceLocation, 256, 256, onPress);
    }

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex,
                       Identifier resourceLocation, int textureWidth, int textureHeight, OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, yDiffTex, resourceLocation, textureWidth, textureHeight,
            onPress, Component.empty());
    }

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex,
                       Identifier resourceLocation, int textureWidth, int textureHeight, OnPress onPress, Component message) {
        super(x, y, width, height, message, onPress);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.resourceLocation = resourceLocation;
    }

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart,
                       Identifier resourceLocation, OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, height, resourceLocation, 256, 256, onPress);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        AbstractGui.setCurrentGraphics(graphics);
        int v = this.yTexStart;
        if (!this.isActive()) {
            v += this.yDiffTex * 2;
        } else if (this.isHoveredOrFocused()) {
            v += this.yDiffTex;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.resourceLocation,
            this.getX(), this.getY(), (float) this.xTexStart, (float) v,
            this.width, this.height, this.textureWidth, this.textureHeight);
        GuiGraphics wrapped = GuiGraphics.wrap(graphics);
        this.renderWidget(wrapped, mouseX, mouseY, partialTick);
        if (this.isHoveredOrFocused()) {
            this.renderToolTip(wrapped, mouseX, mouseY);
        }
    }
}
