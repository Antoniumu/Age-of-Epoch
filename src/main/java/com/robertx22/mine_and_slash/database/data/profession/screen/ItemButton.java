package com.robertx22.mine_and_slash.database.data.profession.screen;

import com.robertx22.library_of_exile.utils.TextUTIL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public class ItemButton extends ImageButton {
    public static int xSize = 16;
    public static int ySize = 16;
    static Identifier buttonLoc = Identifier.fromNamespaceAndPath("library_of_exile", "");
    static Identifier fancyBorderLoc = Identifier.fromNamespaceAndPath("library_of_exile", "textures/gui/pretty_icon_border.png");
    static int FX = 20;
    static int FY = 20;
    ItemStack stack;
    Minecraft mc;
    public boolean renderFancyBorder;

    public List<Component> extraText = new ArrayList<>();

    public ItemButton(ItemStack stack, int xPos, int yPos) {
        this(stack, xPos, yPos, (button) -> {
        });
        this.stack = stack;
    }

    public ItemButton(ItemStack stack, int xPos, int yPos, Button.OnPress onclick) {
        super(xPos + 1, yPos + 1, xSize, ySize, 0, 0, ySize + 1, buttonLoc, onclick);
        this.mc = Minecraft.getInstance();
        this.renderFancyBorder = false;
        this.stack = stack;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {

    }


    public void renderLegacyUnused(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {


        this.setTooltip(Tooltip.create(TextUTIL.mergeList(com.robertx22.forge_compat.common.CompatHelper.getTooltipLines(stack, this.mc.player, TooltipFlag.Default.NORMAL))));
        pGuiGraphics.item(stack, getX(), getY());
        pGuiGraphics.itemDecorations(mc.font, stack, getX(), getY());


        var tip = new ArrayList<Component>();
        tip.addAll(extraText);
        tip.addAll(com.robertx22.forge_compat.common.CompatHelper.getTooltipLines(stack, mc.player, TooltipFlag.Default.NORMAL));

        this.setTooltip(Tooltip.create(TextUTIL.mergeList(tip)));


        super.extractWidgetRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        //pGuiGraphics.renderite
        // RenderUtils.renderStack(pGuiGraphics, this.stack, this.getX(), this.getY());

    }
}
