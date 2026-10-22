package com.robertx22.forge_compat.client.event;

import com.robertx22.forge_compat.client.GuiGraphics;
import com.robertx22.forge_compat.client.gui.overlay.VanillaGuiOverlay;
import com.robertx22.forge_compat.eventbus.api.Event;
import com.mojang.blaze3d.platform.Window;

public class RenderGuiOverlayEvent extends Event {
    private final Window window;
    private final GuiGraphics guiGraphics;
    private final float partialTick;
    private final VanillaGuiOverlay overlay;

    public RenderGuiOverlayEvent(Window window, GuiGraphics guiGraphics, float partialTick, VanillaGuiOverlay overlay) {
        this.window = window;
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
        this.overlay = overlay;
    }

    public Window getWindow() { return window; }
    public GuiGraphics getGuiGraphics() { return guiGraphics; }
    public float getPartialTick() { return partialTick; }
    public VanillaGuiOverlay getOverlay() { return overlay; }

    public static class Pre extends RenderGuiOverlayEvent {
        public Pre(Window w, GuiGraphics g, float pt, VanillaGuiOverlay o) { super(w, g, pt, o); }
    }
    public static class Post extends RenderGuiOverlayEvent {
        public Post(Window w, GuiGraphics g, float pt, VanillaGuiOverlay o) { super(w, g, pt, o); }
    }
}
