package com.robertx22.forge_compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

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
            this.children.add(btn);
        } else if (widget != null) {
            this.children.add(widget);
        }
        return widget;
    }

    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        for (Button b : new ArrayList<>(this.buttons)) {
            if (b != null) {
                b.render(matrix, mouseX, mouseY, partialTicks);
            }
        }
    }

    public void renderBackground(PoseStack matrix) {
        AbstractGui.fill(matrix, 0, 0, this.width, this.height, 0xB0101010);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Handled explicitly via renderBackground / renderBg in subclasses
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.setCurrentGraphics(graphics);
        PoseStack pose = new PoseStack();
        AbstractGui.setCurrentPose(pose);
        this.render(pose, mouseX, mouseY, partialTicks);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        List<Button> snapshot = new ArrayList<>(this.buttons);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Button b = snapshot.get(i);
            if (b != null && b.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.mouseReleased(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseReleased(event);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (this.mouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public static void blit(PoseStack matrices, int x, int y, int u, int v, int width, int height) {
        AbstractGui.blit(matrices, x, y, u, v, width, height);
    }

    public static void blit(PoseStack matrices, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        AbstractGui.blit(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    public static void blit(PoseStack matrices, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        AbstractGui.blit(matrices, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void blit(PoseStack matrices, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        AbstractGui.blit(matrices, x, y, (float) u, (float) v, width, height, textureWidth, textureHeight);
    }

    public void renderTooltip(PoseStack matrices, Component text, int x, int y) {
        if (text != null) {
            AbstractGui.renderComponentTooltip(matrices, List.of(text), x, y);
        }
    }

    public void renderTooltip(PoseStack matrices, List<? extends FormattedCharSequence> lines, int x, int y) {
        AbstractGui.renderTooltip(matrices, lines, x, y);
    }

    public static boolean hasShiftDown() {
        return Minecraft.getInstance().hasShiftDown();
    }

    public static boolean hasControlDown() {
        return Minecraft.getInstance().hasControlDown();
    }

    public static boolean hasAltDown() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasAltDown()) {
            return true;
        }
        try {
            return com.mojang.blaze3d.platform.InputConstants.isKeyDown(342)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(346);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void bindTexture(Identifier id) {
        AbstractGui.bindTexture(id);
    }
}
