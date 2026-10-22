package com.robertx22.forge_compat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ObjectSelectionList<E extends ObjectSelectionList.Entry<E>> extends AbstractWidget {
    public static final Identifier WIDGETS_LOCATION = Identifier.withDefaultNamespace("textures/gui/widgets.png");
    protected final Minecraft minecraft;
    protected final int itemHeight;
    private final List<E> children = new ArrayList<>();
    private E selected;
    private double scrollAmount;
    protected int y0, y1, x0, x1;
    private boolean renderBackground = true;
    private boolean renderTopAndBottom = true;

    public ObjectSelectionList(Minecraft mc, int width, int height, int y0, int y1, int itemHeight) {
        super(0, y0, width, Math.max(10, y1 - y0), Component.empty());
        this.minecraft = mc;
        this.itemHeight = Math.max(1, itemHeight);
        this.y0 = y0;
        this.y1 = y1;
        this.x0 = 0;
        this.x1 = width;
    }

    public void setRenderBackground(boolean v) { this.renderBackground = v; }
    public void setRenderTopAndBottom(boolean v) { this.renderTopAndBottom = v; }
    public void setRenderHeader(boolean v, int h) {}
    public void setLeftPos(int left) {
        this.x0 = left;
        this.x1 = left + this.width;
        this.setX(left);
    }

    public List<E> children() { return this.children; }
    protected void clearEntries() { this.children.clear(); this.selected = null; }
    protected int addEntry(E entry) { this.children.add(entry); return this.children.size() - 1; }
    protected void replaceEntries(Collection<E> entries) {
        this.children.clear();
        this.children.addAll(entries);
    }
    public E getSelected() { return this.selected; }
    public void setSelected(E entry) { this.selected = entry; }
    public int getRowWidth() { return Math.max(40, this.width - 12); }
    public int getRowLeft() { return this.getX() + this.width / 2 - this.getRowWidth() / 2; }
    protected int getScrollbarPosition() { return this.getX() + this.width - 6; }
    public double getScrollAmount() { return this.scrollAmount; }
    public void setScrollAmount(double s) {
        int maxScroll = Math.max(0, this.children.size() * this.itemHeight - this.height);
        this.scrollAmount = Math.clamp(s, 0.0, (double) maxScroll);
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) { this.extractRenderState(graphics, mouseX, mouseY, partialTicks); }
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { this.extractRenderState(graphics.unwrap(), mouseX, mouseY, partialTicks); }
    protected void renderBackground(GuiGraphics graphics) {}
    protected void renderBackground(GuiGraphicsExtractor graphics) {}

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics wrapped = GuiGraphics.wrap(graphics);
        this.renderBackground(graphics);
        this.renderBackground(wrapped);
        graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
        int rowLeft = this.getRowLeft();
        int rowWidth = this.getRowWidth();
        for (int i = 0; i < this.children.size(); i++) {
            int rowTop = this.getY() + 2 + i * this.itemHeight - (int) this.scrollAmount;
            if (rowTop + this.itemHeight < this.getY() || rowTop > this.getY() + this.height) continue;
            E entry = this.children.get(i);
            boolean hovered = mouseX >= rowLeft && mouseX <= rowLeft + rowWidth && mouseY >= rowTop && mouseY <= rowTop + this.itemHeight;
            entry.extractContents(graphics, i, rowTop, rowLeft, rowWidth, this.itemHeight, mouseX, mouseY, hovered, partialTicks);
            entry.render(wrapped, i, rowTop, rowLeft, rowWidth, this.itemHeight, mouseX, mouseY, hovered, partialTicks);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.visible || !this.active) return false;
        double mouseX = event.x(), mouseY = event.y();
        if (mouseX < this.getX() || mouseX > this.getX() + this.width || mouseY < this.getY() || mouseY > this.getY() + this.height) {
            return false;
        }
        int rowLeft = this.getRowLeft();
        int rowWidth = this.getRowWidth();
        for (int i = 0; i < this.children.size(); i++) {
            int rowTop = this.getY() + 2 + i * this.itemHeight - (int) this.scrollAmount;
            if (mouseY >= rowTop && mouseY <= rowTop + this.itemHeight && mouseX >= rowLeft && mouseX <= rowLeft + rowWidth) {
                E entry = this.children.get(i);
                this.setSelected(entry);
                entry.mouseClicked(event, doubleClick);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= this.getY() && mouseY <= this.getY() + this.height) {
            this.setScrollAmount(this.scrollAmount - scrollY * (this.itemHeight * 0.75));
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    public abstract static class Entry<E extends Entry<E>> implements GuiEventListener, NarratableEntry {
        public static final Identifier WIDGETS_LOCATION = Identifier.withDefaultNamespace("textures/gui/widgets.png");
        private boolean focused;
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {}
        public void extractContents(GuiGraphicsExtractor graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {}
        public Component getNarration() { return Component.empty(); }
        @Override public void setFocused(boolean f) { this.focused = f; }
        @Override public boolean isFocused() { return this.focused; }
        @Override public NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
        @Override public void updateNarration(NarrationElementOutput output) {}
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }
        public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    }
}
