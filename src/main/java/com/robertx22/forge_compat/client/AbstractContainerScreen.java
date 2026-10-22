package com.robertx22.forge_compat.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<T> {
    public AbstractContainerScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    protected <W extends GuiEventListener & Renderable & NarratableEntry> W publicAddButton(W widget) {
        return this.addRenderableWidget(widget);
    }

    @Override
    public void tick() {
        super.tick();
        this.containerTick();
    }

    public void containerTick() {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        this.render(GuiGraphics.wrap(graphics), mouseX, mouseY, partialTicks);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.extractBackground(graphics, partialTicks, mouseX, mouseY);
        this.renderBg(GuiGraphics.wrap(graphics), partialTicks, mouseX, mouseY);
        this.renderBg(graphics, partialTicks, mouseX, mouseY);
    }

    protected void extractBackground(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {}
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {}
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {}

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.renderLabels(GuiGraphics.wrap(graphics), mouseX, mouseY);
        this.renderLabels(graphics, mouseX, mouseY);
    }

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics.unwrap(), mouseX, mouseY);
    }
    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        this.extractTooltip(graphics.unwrap(), x, y);
    }

    protected void renderTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        this.extractTooltip(graphics, x, y);
    }

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
        if (this.keyPressed(event.key(), event.keycode(), event.modifiers())) return true;
        return super.keyPressed(event);
    }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    public boolean superKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean superMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
    }

    public boolean superMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.charTyped((char) event.codepoint(), 0)) return true;
        return super.charTyped(event);
    }
    public boolean charTyped(char codePoint, int modifiers) { return false; }
}
