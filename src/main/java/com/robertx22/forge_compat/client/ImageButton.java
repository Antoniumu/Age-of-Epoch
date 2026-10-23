package com.robertx22.forge_compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ImageButton extends Button {

    protected final Identifier resourceLocation;
    protected final int xTexStart;
    protected final int yTexStart;
    protected final int yDiffTex;
    protected final int textureWidth;
    protected final int textureHeight;

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex, Identifier resourceLocation, net.minecraft.client.gui.components.Button.OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, yDiffTex, resourceLocation, 256, 256, onPress);
    }

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex, Identifier resourceLocation, int textureWidth, int textureHeight, net.minecraft.client.gui.components.Button.OnPress onPress) {
        this(x, y, width, height, xTexStart, yTexStart, yDiffTex, resourceLocation, textureWidth, textureHeight, onPress, Component.empty());
    }

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex, Identifier resourceLocation, int textureWidth, int textureHeight, net.minecraft.client.gui.components.Button.OnPress onPress, Component message) {
        this(x, y, width, height, xTexStart, yTexStart, yDiffTex, resourceLocation, textureWidth, textureHeight, onPress, null, message);
    }

    public ImageButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex, Identifier resourceLocation, int textureWidth, int textureHeight, net.minecraft.client.gui.components.Button.OnPress onPress, OnTooltip onTooltip, Component message) {
        super(x, y, width, height, message, onPress, onTooltip);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.resourceLocation = resourceLocation;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void renderButton(PoseStack matrix, int mouseX, int mouseY, float delta) {
        AbstractGui.bindTexture(this.resourceLocation);
        int v = this.yTexStart;
        if (this.isHovered()) {
            v += this.yDiffTex;
        }
        AbstractGui.blit(matrix, this.x, this.y, (float) this.xTexStart, (float) v, this.width, this.height, this.textureWidth, this.textureHeight);
    }
}
