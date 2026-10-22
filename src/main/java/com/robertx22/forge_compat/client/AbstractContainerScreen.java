package com.robertx22.forge_compat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class AbstractContainerScreen<T extends AbstractContainerMenu>
    extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<T> {

    public int imageWidth = 176;
    public int imageHeight = 166;

    protected AbstractContainerScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    public static boolean hasShiftDown() {
        return Minecraft.getInstance().hasShiftDown();
    }

    public static boolean hasControlDown() {
        return Minecraft.getInstance().hasControlDown();
    }

    public static boolean hasAltDown() {
        return Minecraft.getInstance().hasAltDown();
    }

    public int getGuiLeft() {
        return this.leftPos;
    }

    public int getGuiTop() {
        return this.topPos;
    }

    public int getXSize() {
        return this.imageWidth;
    }

    public int getYSize() {
        return this.imageHeight;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
    }

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    public void renderBackground(GuiGraphicsExtractor graphics) {
        this.extractTransparentBackground(graphics);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        AbstractGui.setCurrentGraphics(graphics);
        this.extractTransparentBackground(graphics);
        this.extractBackground(graphics, partialTick, mouseX, mouseY);
        renderBg(graphics, partialTick, mouseX, mouseY);
        renderBg(GuiGraphics.wrap(graphics), partialTick, mouseX, mouseY);
    }

    protected void extractBackground(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        AbstractGui.setCurrentGraphics(graphics);
        super.extractLabels(graphics, mouseX, mouseY);
        renderLabels(graphics, mouseX, mouseY);
        renderLabels(GuiGraphics.wrap(graphics), mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        AbstractGui.setCurrentGraphics(graphics);
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        render(GuiGraphics.wrap(graphics), mouseX, mouseY, partialTick);
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mouseReleased(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseReleased(event);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (mouseDragged(event.x(), event.y(), event.button(), dx, dy)) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (mouseScrolled(x, y, scrollY)) {
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), 0, event.modifiers()) || super.keyPressed(event);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return charTyped((char) event.codepoint(), 0) || super.charTyped(event);
    }

    protected void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.extractTooltip(graphics, mouseX, mouseY);
    }
}
