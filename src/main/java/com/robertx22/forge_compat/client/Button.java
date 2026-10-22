package com.robertx22.forge_compat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class Button extends AbstractButton {
    public interface OnPress {
        void onPress(Button button);
    }

    public interface CreateNarration {
        Component createNarrationMessage(java.util.function.Supplier<Component> defaultSupplier);
    }

    public static final CreateNarration DEFAULT_NARRATION = java.util.function.Supplier::get;

    protected final OnPress onPress;
    protected final CreateNarration createNarration;

    public Button(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public Button(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.createNarration = createNarration;
    }

    public static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress();
    }

    public void onPress() {
        if (this.onPress != null) {
            this.onPress.onPress(this);
        }
    }

    public void onClick(double mouseX, double mouseY) {
        this.onPress();
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.onRelease(event.x(), event.y());
        super.onRelease(event);
    }

    public void onRelease(double mouseX, double mouseY) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.mouseClicked(event.x(), event.y(), event.button())) return true;
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible
            && mouseX >= (double) this.getX()
            && mouseY >= (double) this.getY()
            && mouseX < (double) (this.getX() + this.getWidth())
            && mouseY < (double) (this.getY() + this.getHeight());
    }

    protected ClientTooltipPositioner createTooltipPositioner() {
        return DefaultTooltipPositioner.INSTANCE;
    }

    public void renderTexture(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int uOffset, int vOffset, int yDiff, int width, int height, int textureWidth, int textureHeight) {
        int v = vOffset + (this.isHoveredOrFocused() ? yDiff : 0);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, (float) uOffset, (float) v, width, height, textureWidth, textureHeight);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderWidget(graphics, mouseX, mouseY, partialTicks);
    }

    public void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderWidget(GuiGraphics.wrap(graphics), mouseX, mouseY, partialTicks);
    }

    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int bg = !this.active ? 0xFF333333 : (this.isHoveredOrFocused() ? 0xFF666688 : 0xFF444444);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), bg);
        graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0xFF000000);
        if (this.getMessage() != null) {
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    public static class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x, y;
        private int width = 150, height = 20;
        private net.minecraft.client.gui.components.Tooltip tooltip;

        public Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }
        public Builder pos(int x, int y) { this.x = x; this.y = y; return this; }
        public Builder size(int w, int h) { this.width = w; this.height = h; return this; }
        public Builder bounds(int x, int y, int w, int h) { this.x = x; this.y = y; this.width = w; this.height = h; return this; }
        public Builder tooltip(net.minecraft.client.gui.components.Tooltip t) { this.tooltip = t; return this; }
        public Button build() {
            Button b = new Button(x, y, width, height, message, onPress);
            if (tooltip != null) b.setTooltip(tooltip);
            return b;
        }
    }
}
