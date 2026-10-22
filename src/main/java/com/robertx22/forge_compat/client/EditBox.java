package com.robertx22.forge_compat.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class EditBox extends net.minecraft.client.gui.components.EditBox {
    public EditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    public EditBox(Font font, int x, int y, int width, int height, EditBox copyFrom, Component message) {
        super(font, x, y, width, height, copyFrom, message);
    }

    public void tick() {}

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.extractRenderState(graphics.unwrap(), mouseX, mouseY, partialTicks);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(new CharacterEvent(codePoint, modifiers));
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);
    }
}
