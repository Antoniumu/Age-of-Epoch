package com.robertx22.forge_compat.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class Button extends AbstractButton {

    @FunctionalInterface
    public interface OnPress {
        void onPress(Button button);
    }

    @FunctionalInterface
    public interface CreateNarration {
        Component createNarrationMessage(Supplier<Component> defaultNarration);
    }

    public interface OnTooltip {
        void onTooltip(Button button, GuiGraphics graphics, int mouseX, int mouseY);
    }

    public static final CreateNarration DEFAULT_NARRATION = Supplier::get;
    public static final OnTooltip NO_TOOLTIP = (b, g, x, y) -> {};

    protected final OnPress onPress;
    protected OnTooltip onTooltip = NO_TOOLTIP;

    public Button(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public Button(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration narration) {
        this(x, y, width, height, message, onPress);
    }

    public Button(int x, int y, int width, int height, Component message, OnPress onPress, OnTooltip onTooltip) {
        this(x, y, width, height, message, onPress);
        this.onTooltip = onTooltip != null ? onTooltip : NO_TOOLTIP;
    }

    public static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    public static class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x, y;
        private int width = 150;
        private int height = 20;
        private Tooltip tooltip;

        public Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder pos(int x, int y) { this.x = x; this.y = y; return this; }
        public Builder size(int w, int h) { this.width = w; this.height = h; return this; }
        public Builder bounds(int x, int y, int w, int h) { this.x = x; this.y = y; this.width = w; this.height = h; return this; }
        public Builder tooltip(Tooltip t) { this.tooltip = t; return this; }

        public Button build() {
            Button b = new Button(x, y, width, height, message, onPress);
            if (tooltip != null) b.setTooltip(tooltip);
            return b;
        }
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

    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    public void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.extractDefaultSprite(graphics);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.onTooltip != null) {
            this.onTooltip.onTooltip(this, graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.setCurrentGraphics(graphics);
        GuiGraphics wrapped = GuiGraphics.wrap(graphics);
        this.renderWidget(graphics, mouseX, mouseY, partialTicks);
        this.renderWidget(wrapped, mouseX, mouseY, partialTicks);
        if (this.isHoveredOrFocused()) {
            this.renderToolTip(wrapped, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        
    }
}
