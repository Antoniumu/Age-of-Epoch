package com.robertx22.infinite_dungeons.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.Screen;
import com.robertx22.forge_compat.client.Button;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

public class BaseScreen extends Screen {

    protected BaseScreen(int width, int height) {
        super(Component.literal(""));
        this.sizeX = width;
        this.sizeY = height;
    }

    public Minecraft mc = Minecraft.getInstance();

    public int guiLeft = 0;
    public int guiTop = 0;

    public int sizeX = 0;
    public int sizeY = 0;

    public void renderBackground(PoseStack matrix, Identifier id) {
        renderBackground(matrix, id, 256, 256);
    }

    public void renderBackground(PoseStack matrix, Identifier id, int x, int y) {
        com.robertx22.forge_compat.client.AbstractGui.bindTexture(id);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, mc.getWindow()
                .getGuiScaledWidth() / 2 - sizeX / 2,
            mc.getWindow()
                .getGuiScaledHeight() / 2 - sizeY / 2, 0, 0, sizeX, sizeY, x, y
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - this.sizeX) / 2;
        this.guiTop = (this.height - this.sizeY) / 2;
    }

    public <T extends Button> T publicAddButton(T w) {
        return this.addButton(w);
    }

}
