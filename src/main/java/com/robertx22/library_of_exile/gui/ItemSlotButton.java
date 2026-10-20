package com.robertx22.library_of_exile.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.library_of_exile.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ItemSlotButton extends ImageButton {

    public static int xSize = 16;
    public static int ySize = 16;

    static ResourceLocation buttonLoc = ResourceLocation.fromNamespaceAndPath(Ref.MODID, "");

    static ResourceLocation fancyBorderLoc = ResourceLocation.fromNamespaceAndPath(Ref.MODID, "textures/gui/pretty_icon_border.png");
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

    public ItemSlotButton(ItemStack stack, int xPos, int yPos, IPressable onclick) {
        super(xPos + 1, yPos + 1, xSize, ySize, 0, 0, ySize + 1, buttonLoc, onclick);
        this.stack = stack;
    }

    @Override
    public void renderButton(PoseStack matrix, int x, int y, float ticks) {

        if (renderFancyBorder) {
            mc.getTextureManager()
                .bind(fancyBorderLoc);
            blit(matrix, this.x - 2, this.y - 2, 0, 0, FX, FY);
        }

        RenderUtils.renderStack(stack, this.x, this.y);

    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (!stack.isEmpty()) {
            if (isInside(x, y)) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.addAll(stack.getTooltipLines(mc.player, TooltipFlag.TooltipFlags.NORMAL));
                GuiUtils.renderTooltip(matrix, tooltip, x, y);
            }
        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, xSize, ySize, x, y);
    }

}
