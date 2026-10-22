package com.robertx22.mine_and_slash.database.data.profession.screen;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.tooltip.ExileTooltipUtils;
import com.robertx22.mine_and_slash.a_libraries.jei.LockRecipePacket;
import com.robertx22.mine_and_slash.database.data.profession.ProfessionRecipe;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public class RecipeButton extends ImageButton {

    public static int XS = 18;
    public static int YS = 19;

    Minecraft mc = Minecraft.getInstance();


    ProfessionRecipe recipe;

    public RecipeButton(CraftingStationScreen screen, ProfessionRecipe recipe, int xPos, int yPos) {
        super(xPos, yPos, XS, YS, 0, 0, YS, SlashRef.guiId(""), (button) -> {
            Packets.sendToServer(new LockRecipePacket(recipe.GUID()));
            screen.refreshRequiredMats(recipe);
        });
        this.recipe = recipe;
    }

    public void renderLegacyUnused(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        setModTooltip();
        super.extractWidgetRenderState(gui, mouseX, mouseY, delta);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {

        gui.fakeItem(recipe.toResultStackForJei(), getX(), getY());

        //   Identifier tex = SlashRef.guiId("craftbutton");
        // com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        //com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, tex, getX(), getY(), 0, (pbe.getSyncedData().craftingState == Crafting_State.ACTIVE || pbe.getSyncedData().craftingState == Crafting_State.IDLE) ? 0 : 19, 18, 19);
    }

    public void setModTooltip() {

        List<MutableComponent> list = new ArrayList<>();
        for (Component l : com.robertx22.forge_compat.common.CompatHelper.getTooltipLines(recipe.toResultStackForJei(), mc.player, TooltipFlag.Default.NORMAL)) {
            list.add((MutableComponent) l);
        }

        this.setTooltip(Tooltip.create(ExileTooltipUtils.joinMutableComps(list.listIterator(), Component.literal("\n"))));
    }

}