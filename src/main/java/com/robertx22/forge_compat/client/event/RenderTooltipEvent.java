package com.robertx22.forge_compat.client.event;

import com.robertx22.forge_compat.client.GuiGraphics;
import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.FormattedText;

import java.util.List;

public class RenderTooltipEvent extends Event {
    protected final ItemStack itemStack;
    protected final List<ClientTooltipComponent> components;
    protected int x, y;

    public RenderTooltipEvent(ItemStack itemStack, int x, int y, List<ClientTooltipComponent> components) {
        this.itemStack = itemStack;
        this.x = x;
        this.y = y;
        this.components = components;
    }

    public ItemStack getItemStack() { return itemStack; }
    public List<ClientTooltipComponent> getComponents() { return components; }
    public int getX() { return x; }
    public int getY() { return y; }

    public static class GatherComponents extends Event {
        private final ItemStack itemStack;
        private final List<Either<FormattedText, TooltipComponent>> tooltipElements;
        public GatherComponents(ItemStack stack, List<Either<FormattedText, TooltipComponent>> elements) {
            this.itemStack = stack;
            this.tooltipElements = elements;
        }
        public ItemStack getItemStack() { return itemStack; }
        public List<Either<FormattedText, TooltipComponent>> getTooltipElements() { return tooltipElements; }
    }

    public static class Color extends RenderTooltipEvent {
        private int backgroundStart, backgroundEnd, borderStart, borderEnd;
        private final GuiGraphics graphics;
        public Color(ItemStack stack, GuiGraphics g, int x, int y, List<ClientTooltipComponent> comps) {
            super(stack, x, y, comps);
            this.graphics = g;
        }
        public GuiGraphics getGraphics() { return graphics; }
        public int getBackgroundStart() { return backgroundStart; }
        public int getBackgroundEnd() { return backgroundEnd; }
        public int getBorderStart() { return borderStart; }
        public int getBorderEnd() { return borderEnd; }
        public void setBackground(int c) { this.backgroundStart = c; this.backgroundEnd = c; }
        public void setBackgroundStart(int c) { this.backgroundStart = c; }
        public void setBackgroundEnd(int c) { this.backgroundEnd = c; }
        public void setBorderStart(int c) { this.borderStart = c; }
        public void setBorderEnd(int c) { this.borderEnd = c; }
    }
}
