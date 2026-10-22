package com.robertx22.library_of_exile.gui;


import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.utils.RenderUtils;
import com.robertx22.library_of_exile.utils.TextUTIL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ItemSlotButton extends ImageButton {

    public static int xSize = 16;
    public static int ySize = 16;

    static Identifier buttonLoc = Identifier.fromNamespaceAndPath(Ref.MODID, "");

    static Identifier fancyBorderLoc = Identifier.fromNamespaceAndPath(Ref.MODID, "textures/gui/pretty_icon_border.png");
    static int FX = 20;
    static int FY = 20;
    ItemStack stack;
    Minecraft mc = Minecraft.getInstance();

    public boolean renderFancyBorder = false;

    public ItemSlotButton(ItemStack stack, int xPos, int yPos) {
        this(stack, xPos, yPos, (button) -> {
        });
        this.stack = stack;

    }

    public ItemSlotButton(ItemStack stack, int xPos, int yPos, Button.OnPress onclick) {
        super(xPos + 1, yPos + 1, xSize, ySize, 0, 0, ySize + 1, buttonLoc, onclick);
        this.stack = stack;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // todo would this work?
        setTooltip(Tooltip.create(TextUTIL.mergeList(com.robertx22.forge_compat.common.CompatHelper.getTooltipLines(stack, mc.player, TooltipFlag.Default.NORMAL))));

        RenderUtils.renderStack(pGuiGraphics, stack, this.getX(), this.getY());

        super.extractContents(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    /*
    @Override
    public void renderButton(MatrixStack matrix, int x, int y, float ticks) {

        if (renderFancyBorder) {
            mc.getTextureManager()
                    .bind(fancyBorderLoc);
            blit(matrix, this.x - 2, this.y - 2, 0, 0, FX, FY);
        }

        RenderUtils.renderStack(stack, this.x, this.y);

    }

     */


    /*
    @Override
    public void renderToolTip(GuiGraphicsExtractor matrix, int x, int y) {
        if (!stack.isEmpty()) {
            if (isInside(x, y)) {
                List<ITextComponent> tooltip = new ArrayList<>();
                tooltip.addAll(com.robertx22.forge_compat.common.CompatHelper.getTooltipLines(stack, mc.player, ITooltipFlag.TooltipFlags.NORMAL));
                com.robertx22.forge_compat.client.GuiBlitHelper.renderTooltip(GuiUtils, matrix, tooltip, x, y);
            }
        }
    }

     */

    /*
    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, xSize, ySize, x, y);
    }


     */
}

