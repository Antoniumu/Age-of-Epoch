package com.robertx22.forge_compat.client.event;

import com.robertx22.forge_compat.client.GuiGraphics;
import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ScreenEvent extends Event {
    private final Screen screen;
    public ScreenEvent(Screen screen) { this.screen = screen; }
    public Screen getScreen() { return screen; }

    public static class Render extends ScreenEvent {
        private final GuiGraphics guiGraphics;
        private final int mouseX, mouseY;
        private final float partialTick;
        public Render(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super(screen);
            this.guiGraphics = g;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.partialTick = partialTick;
        }
        public GuiGraphics getGuiGraphics() { return guiGraphics; }
        public int getMouseX() { return mouseX; }
        public int getMouseY() { return mouseY; }
        public float getPartialTick() { return partialTick; }
        public static class Pre extends Render {
            public Pre(Screen s, GuiGraphics g, int mx, int my, float pt) { super(s, g, mx, my, pt); }
        }
        public static class Post extends Render {
            public Post(Screen s, GuiGraphics g, int mx, int my, float pt) { super(s, g, mx, my, pt); }
        }
    }

    public static class Init extends ScreenEvent {
        private final List<GuiEventListener> listeners = new ArrayList<>();
        private final Consumer<GuiEventListener> add;
        public Init(Screen s, Consumer<GuiEventListener> add) {
            super(s);
            this.add = add;
        }
        public List<GuiEventListener> getListenersList() { return listeners; }
        public void addListener(GuiEventListener l) { if (add != null) add.accept(l); listeners.add(l); }
        public static class Post extends Init {
            public Post(Screen s, Consumer<GuiEventListener> add) { super(s, add); }
        }
    }

    public static class MouseScrolled extends ScreenEvent {
        private final double mouseX, mouseY, scrollDelta;
        public MouseScrolled(Screen s, double mx, double my, double delta) {
            super(s);
            this.mouseX = mx;
            this.mouseY = my;
            this.scrollDelta = delta;
        }
        public double getMouseX() { return mouseX; }
        public double getMouseY() { return mouseY; }
        public double getScrollDelta() { return scrollDelta; }
        public static class Pre extends MouseScrolled {
            public Pre(Screen s, double mx, double my, double d) { super(s, mx, my, d); }
        }
    }

    public static class MouseButtonPressed extends ScreenEvent {
        private final double mouseX, mouseY;
        private final int button;
        public MouseButtonPressed(Screen s, double mx, double my, int btn) {
            super(s);
            this.mouseX = mx;
            this.mouseY = my;
            this.button = btn;
        }
        public double getMouseX() { return mouseX; }
        public double getMouseY() { return mouseY; }
        public int getButton() { return button; }
        public static class Pre extends MouseButtonPressed {
            public Pre(Screen s, double mx, double my, int btn) { super(s, mx, my, btn); }
        }
        public static class Post extends MouseButtonPressed {
            public Post(Screen s, double mx, double my, int btn) { super(s, mx, my, btn); }
        }
    }
}
