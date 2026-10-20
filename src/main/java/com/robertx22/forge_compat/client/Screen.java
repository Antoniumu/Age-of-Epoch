package com.robertx22.forge_compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public abstract class Screen extends net.minecraft.client.gui.screens.Screen {

    public final List<Button> buttons = new ArrayList<>();
    public final List<GuiEventListener> children = new ArrayList<>();

    protected Screen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.children.clear();
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
            this.addWidget((GuiEventListener & net.minecraft.client.gui.narration.NarratableEntry) widget);
        }
        return widget;
    }

    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
    }

    public void renderBackground(PoseStack matrix) {
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

    public void renderTooltip(PoseStack matrices, List<? extends net.minecraft.util.FormattedCharSequence> lines, int x, int y) {
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

    public static void bindTexture(Identifier id) {
    }
}
