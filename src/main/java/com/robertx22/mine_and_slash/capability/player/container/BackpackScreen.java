package com.robertx22.mine_and_slash.capability.player.container;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.mixin_ducks.MouseHandlerDuck;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import com.robertx22.forge_compat.client.event.ScreenEvent;

public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {


    public static final Identifier BACKGROUND_LOCATION = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/master_bag.png");

    public static double iMouseX = 0;
    public static double iMouseY = 0;

    public BackpackScreen(BackpackMenu pMenu, Inventory pPlayerInventory, Component txt) {
        super(pMenu, pPlayerInventory, Component.literal(""));
        this.imageWidth = 199;
        this.imageHeight = 222;


    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos + 175;
        int y = topPos + 18;

        for (Backpacks.BackpackType type : Backpacks.BackpackType.values()) {
            this.addRenderableWidget(new BackpackButton(type, x, y));
            y += 18;
        }

        if (Minecraft.getInstance().mouseHandler instanceof MouseHandlerDuck mouseHandler) {
            mouseHandler.setXPos(iMouseX);
            mouseHandler.setYPos(iMouseY);
        }
        //from MouseHandler.class releaseMouse()
        
    }

    public void extractRenderState(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.extractBackground(pGuiGraphics, 0, 0, 0F);
        super.extractRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.extractTooltip(pGuiGraphics, pMouseX, pMouseY);

    }

    @Override
    public void onClose() {
        super.onClose();
        //reset position
        iMouseX = (double)(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2);
        iMouseY = (double)(Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2);
    }



    @Override
    protected void renderLabels(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY) {
        //  pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        // pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void extractBackground(GuiGraphicsExtractor pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(pGuiGraphics, BACKGROUND_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);

    }

}
