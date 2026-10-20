package com.robertx22.forge_compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<T> {

    public final List<Button> buttons = new ArrayList<>();
    public final List<GuiEventListener> children = new ArrayList<>();
    public int imageWidth = 176;
    public int imageHeight = 166;
    public int x = 0;
    public int y = 0;
    public int backgroundWidth = 176;
    public int backgroundHeight = 166;

    public AbstractContainerScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.x = this.leftPos;
        this.y = this.topPos;
        this.backgroundWidth = this.imageWidth;
        this.backgroundHeight = this.imageHeight;
    }

    protected <W extends GuiEventListener> W addButton(W widget) {
        if (widget instanceof Button btn) {
            this.buttons.add(btn);
            this.addRenderableWidget(btn);
        } else if (widget instanceof AbstractWidget aw) {
            this.addRenderableWidget(aw);
        } else if (widget instanceof Renderable && widget instanceof NarratableEntry) {
            this.addRenderableWidget((GuiEventListener & Renderable & NarratableEntry) widget);
        } else {
            this.addWidget((GuiEventListener & NarratableEntry) widget);
        }
        return widget;
    }

    protected void renderBg(PoseStack matrices, float delta, int mouseX, int mouseY) {
    }

    protected void renderLabels(PoseStack matrices, int mouseX, int mouseY) {
    }

    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
    }

    public void renderBackground(PoseStack matrices) {
    }

    protected void renderTooltip(PoseStack matrices, int mouseX, int mouseY) {
    }

    public void renderTooltip(PoseStack matrices, List<? extends net.minecraft.util.FormattedCharSequence> lines, int x, int y) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        AbstractGui.CURRENT_GRAPHICS = graphics;
        this.renderBg(new PoseStack(), a, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.CURRENT_GRAPHICS = graphics;
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        this.render(new PoseStack(), mouseX, mouseY, partialTicks);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public static void blit(PoseStack matrices, int x, int y, int u, int v, int width, int height) {
    }

    public static void blit(PoseStack matrices, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
    }

    public static void blit(PoseStack matrices, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
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
}
