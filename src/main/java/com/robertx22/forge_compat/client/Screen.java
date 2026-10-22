package com.robertx22.forge_compat.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class Screen extends net.minecraft.client.gui.screens.Screen {
    protected Screen(Component title) {
        super(title);
    }

    public void renderBackground(GuiGraphicsExtractor graphics) {
        super.extractTransparentBackground(graphics);
    }

    public void renderBackground(GuiGraphics graphics) {
        if (graphics != null && graphics.unwrap() != null) {
            super.extractTransparentBackground(graphics.unwrap());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        this.render(graphics, mouseX, mouseY, partialTicks);
        this.render(GuiGraphics.wrap(graphics), mouseX, mouseY, partialTicks);
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {}
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.mouseClicked(event.x(), event.y(), event.button())) return true;
        return super.mouseClicked(event, doubleClick);
    }
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.mouseReleased(event.x(), event.y(), event.button())) return true;
        return super.mouseReleased(event);
    }
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY)) return true;
        return super.mouseDragged(event, dragX, dragY);
    }
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { return false; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) { return false; }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.keyPressed(event.key(), event.scancode(), event.modifiers())) return true;
        return super.keyPressed(event);
    }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.charTyped((char) event.codepoint(), event.modifiers())) return true;
        return super.charTyped(event);
    }
    public boolean charTyped(char codePoint, int modifiers) { return false; }

    public void setTooltipForNextRenderPass(net.minecraft.client.gui.components.Tooltip tip, net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner pos, boolean override) {}
}
