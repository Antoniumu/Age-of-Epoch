package com.robertx22.mine_and_slash.gui.overlays.spell_cast_bar;

import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.config.forge.overlay.OverlayType;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class SpellCastBarOverlay {

    static Identifier GUI_BARS_TEXTURES = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/cast_bar/cast_bar.png");
    static Identifier FILLED = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/cast_bar/cast_bar_fill.png");

    static int WIDTH = 172;
    static int HEIGHT = 20;

    Minecraft mc = Minecraft.getInstance();

    public void onHudRender(GuiGraphicsExtractor gui, float partialtick) {

        var data = Load.player(mc.player);

        if (data == null) {
            return;
        }

        if (data.spellCastingData.isCasting() && data.spellCastingData.castTickLeft > 0) {

            float total = data.spellCastingData.spellTotalCastTicks;
            float current = data.spellCastingData.castTickLeft;

            float percent = (total - current + partialtick) / total;

            renderCastBar(gui, data.spellCastingData.getSpellBeingCast(), percent);

        }


    }

    private void renderCastBar(GuiGraphicsExtractor gui, Spell spell, float percent) {

        int spellSize = 14;

        int x = mc.getWindow().getGuiScaledWidth() / 2 - WIDTH / 2;
        int y = (int) (mc.getWindow().getGuiScaledHeight() / 1.25F - HEIGHT / 2);

        var config = ClientConfigs.getConfig().getOverlayConfig(OverlayType.SPELL_CAST_BAR);

        x = config.getPos().x;
        y = config.getPos().y;

        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, GUI_BARS_TEXTURES, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);

        int i = (int) (percent * (float) WIDTH);
        if (i > 0) {
            com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, FILLED, x, y, 0, 0, i, HEIGHT, WIDTH, HEIGHT);
        }

        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, spell.getIconLoc(), x + 79, y + 3, 0, 0, spellSize, spellSize, spellSize, spellSize);

    }


}
