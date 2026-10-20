package com.robertx22.forge_compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class Button extends net.minecraft.client.gui.components.Button {

    public int x;
    public int y;

    @FunctionalInterface
    public interface IPressable extends net.minecraft.client.gui.components.Button.OnPress {
    }

    @FunctionalInterface
    public interface OnTooltip {
        void onTooltip(Button button, PoseStack matrices, int mouseX, int mouseY);
    }

    protected OnTooltip compatTooltip;

    public Button(int x, int y, int width, int height, Component message, net.minecraft.client.gui.components.Button.OnPress onPress) {
        super(x, y, width, height, message, onPress != null ? onPress : b -> {}, net.minecraft.client.gui.components.Button.DEFAULT_NARRATION);
        this.x = x;
        this.y = y;
    }

    public Button(int x, int y, int width, int height, Component message, net.minecraft.client.gui.components.Button.OnPress onPress, OnTooltip onTooltip) {
        this(x, y, width, height, message, onPress);
        this.compatTooltip = onTooltip;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        AbstractGui.CURRENT_GRAPHICS = graphics;
        if (this.getX() != this.x) this.setX(this.x);
        if (this.getY() != this.y) this.setY(this.y);
        PoseStack matrix = new PoseStack();
        this.renderButton(matrix, mouseX, mouseY, delta);
    }

    public void render(PoseStack matrix, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;
        if (this.getX() != this.x) this.setX(this.x);
        if (this.getY() != this.y) this.setY(this.y);
        this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        this.renderButton(matrix, mouseX, mouseY, delta);
    }

    public void renderButton(PoseStack matrix, int mouseX, int mouseY, float delta) {
        if (AbstractGui.CURRENT_GRAPHICS != null) {
            this.extractDefaultSprite(AbstractGui.CURRENT_GRAPHICS);
        }
    }

    public void renderToolTip(PoseStack matrix, int mouseX, int mouseY) {
        if (this.compatTooltip != null) {
            this.compatTooltip.onTooltip(this, matrix, mouseX, mouseY);
        }
    }

    @Override
    public void onPress(InputWithModifiers input) {
        super.onPress(input);
        this.onPress();
        this.onPress(this);
    }

    public void onPress() {
    }

    public void onPress(Button button) {
    }

    public boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible
            && mouseX >= (double) this.x && mouseY >= (double) this.y
            && mouseX < (double) (this.x + this.width) && mouseY < (double) (this.y + this.height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.getX() != this.x) this.setX(this.x);
        if (this.getY() != this.y) this.setY(this.y);
        if (this.mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public void onClick(double mouseX, double mouseY) {
    }

    public static void blit(PoseStack matrix, int x, int y, int u, int v, int w, int h) {
        AbstractGui.blit(matrix, x, y, u, v, w, h);
    }

    public static void blit(PoseStack matrix, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        AbstractGui.blit(matrix, x, y, u, v, w, h, texW, texH);
    }

    public static void blit(PoseStack matrix, int x, int y, int u, int v, int w, int h, int texW, int texH) {
        AbstractGui.blit(matrix, x, y, u, v, w, h, texW, texH);
    }

    public static void blit(PoseStack matrix, int x, int y, int w, int h, float u, float v, int regionW, int regionH, int texW, int texH) {
        AbstractGui.blit(matrix, x, y, w, h, u, v, regionW, regionH, texW, texH);
    }
}
