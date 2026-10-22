package com.robertx22.mine_and_slash.capability.player.container;

import com.robertx22.mine_and_slash.gui.inv_gui.GuiInventoryGrids;
import com.robertx22.mine_and_slash.gui.inv_gui.InvGuiScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.library_of_exile.utils.TextUTIL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SpellButton extends ImageButton {

    public static int BUTTON_SIZE_X = 16;
    public static int BUTTON_SIZE_Y = 16;

    int slot;

    public SpellButton(int slot, int xPos, int yPos) {
        super(xPos, yPos, BUTTON_SIZE_X, BUTTON_SIZE_Y, 0, 0, BUTTON_SIZE_Y, SlashRef.guiId("empty_spell"), (button) -> {
            Minecraft.getInstance().gui.setScreen(new InvGuiScreen(GuiInventoryGrids.ofSelectableSpells(ClientOnly.getPlayer(), slot)));
        });
        this.slot = slot;
    }

    public void renderLegacyUnused(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        if (this.isHovered()) {
            setModTooltip();
        }
        super.extractContents(gui, mouseX, mouseY, delta);
    }

    public SkillGemData getSpell() {
        return Load.player(ClientOnly.getPlayer()).spellCastingData.getSpellData(slot).getData();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        //  super.renderWidget(gui, mouseX, mouseY, delta);


        boolean flicker = Load.player(ClientOnly.getPlayer()).spellCastingData.learnedSpellButHotbarIsEmpty();

        var mc = Minecraft.getInstance();

        // todo check if this causes seizures
        float color = flicker ? MathHelper.clamp((mc.player.tickCount % 25 + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)) * 0.13f, 0, 3) : 1F;

        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, color, 1.0F, 1.0F);
        if (hasSpell()) {
            com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, getSpell().getSpell().getIconLoc(), getX(), getY(), BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X);
        } else {
            com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, SlashRef.guiId("empty_spell"), getX(), getY(), BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X);
        }
    }

    public boolean hasSpell() {
        return getSpell() != null && getSpell().getSpell() != null;
    }

    public void setModTooltip() {

        List<Component> tooltip = new ArrayList<>();
        if (hasSpell()) {
            Minecraft mc = Minecraft.getInstance();
            tooltip = getSpell().getTooltip(mc.player);
        }
        this.setTooltip(Tooltip.create(TextUTIL.mergeList(tooltip)));

    }

    @Override
    protected ClientTooltipPositioner createTooltipPositioner() {
        return DefaultTooltipPositioner.INSTANCE;
    }

}
