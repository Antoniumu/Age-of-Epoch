package com.robertx22.mine_and_slash.gui.inv_gui;

import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class InvGuiScreen extends BaseScreen {
    static Identifier TEX = SlashRef.guiId("inv_gui/background");

    InvGuiGrid grid;

    public InvGuiScreen(InvGuiGrid grid) {
        super(177, 136);

        this.grid = grid;
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int x, int y, float ticks) {


        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, TEX, mc.getWindow()
                        .getGuiScaledWidth() / 2 - sizeX / 2, mc.getWindow()
                        .getGuiScaledHeight() / 2 - sizeY / 2, 0, 0, sizeX, sizeY);

        super.extractRenderState(gui, x, y, ticks);


    }

    @Override
    protected void init() {
        super.init();
        int i = 0;
        for (int y = 0; y < InvGuiGrid.Y_MAX; y++) {
            for (int x = 0; x < InvGuiGrid.X_MAX; x++) {

                int xpos = guiLeft + 8 + (x * InvGuiButton.BUTTON_SIZE_X);
                int ypos = guiTop + 17 + (y * InvGuiButton.BUTTON_SIZE_Y);

                this.publicAddButton(new InvGuiButton(grid.list.get(i), xpos, ypos));

                i++;
            }
        }

    }
}
