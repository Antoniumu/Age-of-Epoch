package com.robertx22.forge_compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractContainerScreen<T extends AbstractContainerMenu>
    extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<T> {

    public final List<Button> buttons = new ArrayList<>();
    public final List<GuiEventListener> children = new ArrayList<>();
    /**
     * В MC 26.3 ванильные imageWidth/imageHeight — final (176x166), поэтому шим
     * держит собственные изменяемые поля с теми же именами (затенение вынужденное).
     * Из-за этого ванильная проверка «клик вне окна» (doClick(-999) -> выброс
     * предмета из инвентаря) считает окно высотой 166: весь хотбар (GUI-y 183..199)
     * и правая часть широких экранов (станция рун 276x195) попадают в «ложную
     * полосу вне окна», и клик по ним выбрасывал предмет. Обработку этой полосы
     * шим берёт на себя в {@link #handleFalseOutsideClick}.
     */
    public int imageWidth = 176;
    public int imageHeight = 166;
    /** Размеры окна, которые ваниль считает настоящими (final-поля 26.3). */
    private static final int VANILLA_WINDOW_W = 176;
    private static final int VANILLA_WINDOW_H = 166;
    public int x = 0;
    public int y = 0;
    public int backgroundWidth = 176;
    public int backgroundHeight = 166;
    private boolean superExtractedInFrame = false;
    private boolean inExtractRenderState = false;

    public AbstractContainerScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();
        this.children.clear();
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
            this.children.add(btn);
        } else if (widget != null) {
            this.children.add(widget);
        }
        return widget;
    }

    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        GuiGraphicsExtractor g = AbstractGui.currentGraphics();
        if (g != null && !superExtractedInFrame) {
            superExtractedInFrame = true;
            super.extractRenderState(g, mouseX, mouseY, partialTicks);
        }
        for (Button b : new ArrayList<>(this.buttons)) {
            if (b != null) {
                b.render(matrix, mouseX, mouseY, partialTicks);
            }
        }
    }

    public void renderBackground(PoseStack matrix) {
        if (!inExtractRenderState) {
            AbstractGui.fill(matrix, 0, 0, this.width, this.height, 0xB0101010);
        }
    }

    protected void renderBg(PoseStack matrix, float partialTicks, int mouseX, int mouseY) {
    }

    protected void renderLabels(PoseStack matrix, int mouseX, int mouseY) {
    }

    protected void renderTooltip(PoseStack matrix, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            List<Component> lines = stack.getTooltipLines(
                Item.TooltipContext.EMPTY,
                Minecraft.getInstance().player,
                Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
            );
            AbstractGui.renderComponentTooltip(matrix, lines, mouseX, mouseY);
        }
    }

    public void renderTooltip(PoseStack matrix, Component text, int x, int y) {
        if (text != null) {
            AbstractGui.renderComponentTooltip(matrix, List.of(text), x, y);
        }
    }

    public void renderTooltip(PoseStack matrix, List<? extends FormattedCharSequence> lines, int x, int y) {
        AbstractGui.renderTooltip(matrix, lines, x, y);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.setCurrentGraphics(graphics);
        PoseStack pose = new PoseStack();
        AbstractGui.setCurrentPose(pose);
        AbstractGui.fill(pose, 0, 0, this.width, this.height, 0xB0101010);
        this.renderBg(pose, partialTicks, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        AbstractGui.setCurrentGraphics(graphics);
        this.renderLabels(AbstractGui.currentPose(), mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        AbstractGui.setCurrentGraphics(graphics);
        PoseStack pose = new PoseStack();
        AbstractGui.setCurrentPose(pose);
        superExtractedInFrame = false;
        inExtractRenderState = true;
        try {
            this.render(pose, mouseX, mouseY, partialTicks);
            if (!superExtractedInFrame) {
                superExtractedInFrame = true;
                super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
            }
        } finally {
            inExtractRenderState = false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (handleFalseOutsideClick(event)) {
            return true;
        }
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

    /**
     * Клики в «ложной полосе вне окна»: внутри настоящего окна, но за границами
     * ванильного 176x166 (нижние ряды инвентаря/хотбар, правая часть широких
     * экранов). Ваниль такие клики не доводит до слотов и вместо этого делает
     * doClick(-999) — выброс предмета с курсора. Здесь слот находится вручную,
     * и обычный PICKUP выполняется без ванильного «вне окна»-пути.
     */
    private boolean handleFalseOutsideClick(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int gx = (int) (mouseX - this.leftPos);
        int gy = (int) (mouseY - this.topPos);
        if (gx < 0 || gy < 0 || gx >= this.imageWidth || gy >= this.imageHeight) {
            return false; // настоящее «вне окна»: выброс предмета курсором — штатное поведение
        }
        if (gx < VANILLA_WINDOW_W && gy < VANILLA_WINDOW_H) {
            return false; // ваниль и так видит этот клик
        }
        net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
        if (player == null) {
            return true;
        }
        Slot slot = null;
        for (Slot s : this.menu.slots) {
            if (gx >= s.x && gx < s.x + 16 && gy >= s.y && gy < s.y + 16) {
                slot = s;
                break;
            }
        }
        if (slot == null) {
            // Кнопки в этой зоне (например, кнопка крафта станции рун) обработает
            // штатная цепочка; а вот одиночный клик мимо слота с предметом на
            // курсоре не должен выбрасывать предмет.
            for (Button b : this.buttons) {
                if (b != null && b.clicked(mouseX, mouseY)) {
                    return false;
                }
            }
            return !this.menu.getCarried().isEmpty();
        }
        ItemStack carried = this.menu.getCarried();
        int button = event.button();
        if (carried.isEmpty()) {
            if (slot.hasItem() && slot.mayPickup(player)) {
                ItemStack inSlot = slot.getItem();
                this.menu.setCarried(inSlot);
                slot.set(ItemStack.EMPTY);
                slot.onTake(player, inSlot);
                slot.setChanged();
            }
            return true;
        }
        if (!slot.hasItem()) {
            if (slot.mayPlace(carried)) {
                if (button == 1 && carried.getCount() > 1) {
                    slot.set(carried.split(1));
                } else {
                    slot.set(carried);
                    this.menu.setCarried(ItemStack.EMPTY);
                }
                slot.setChanged();
            }
            return true;
        }
        ItemStack inSlot = slot.getItem();
        boolean same = inSlot.getItem() == carried.getItem()
            && inSlot.getComponents().equals(carried.getComponents());
        int max = Math.min(slot.getMaxStackSize(inSlot), inSlot.getMaxStackSize());
        if (same && inSlot.getCount() < max) {
            int move = Math.min(carried.getCount(), max - inSlot.getCount());
            inSlot.grow(move);
            carried.shrink(move);
            if (carried.isEmpty()) {
                this.menu.setCarried(ItemStack.EMPTY);
            }
            slot.setChanged();
            return true;
        }
        if (slot.mayPickup(player) && slot.mayPlace(carried)) {
            slot.set(carried);
            this.menu.setCarried(inSlot);
            slot.setChanged();
        }
        return true;
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

    public Slot getSlotUnderMouse() {
        return this.hoveredSlot;
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
        AbstractGui.bindTexture(id);
    }
}
