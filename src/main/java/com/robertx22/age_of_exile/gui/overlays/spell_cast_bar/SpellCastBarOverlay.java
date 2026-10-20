package com.robertx22.age_of_exile.gui.overlays.spell_cast_bar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;

public class SpellCastBarOverlay extends Gui {

    private static final ResourceLocation GUI_BARS_TEXTURES = ResourceLocation.parse("textures/gui/bars.png");

    static int WIDTH = 182;
    static int HEIGHT = 5;

    Minecraft mc = Minecraft.getInstance();

    public void onHudRender(PoseStack matrix, float v) {

        EntitySpellCap.ISpellsCap data = Load.spells(mc.player);

        if (data == null) {
            return;
        }

        if (data.getCastingData()
            .isCasting() && data.getCastingData().castingTicksLeft > 0) {

            int x = mc.getWindow()
                .getGuiScaledWidth() / 2 - WIDTH / 2;
            int y = (int) (mc.getWindow()
                .getGuiScaledHeight() / 1.25F - HEIGHT / 2);

            float percent =
                ((float) data.getCastingData().lastSpellCastTimeInTicks - (float) data.getCastingData().castingTicksLeft) / (float) data
                    .getCastingData().lastSpellCastTimeInTicks;

            render(matrix, x, y, ServerBossEvent.Color.PURPLE, ServerBossEvent.Overlay.NOTCHED_20, percent);
        }
    }

    private void render(PoseStack matrix, int x, int y, ServerBossEvent.Color color, ServerBossEvent.Overlay overlay, float percent) {

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bind(GUI_BARS_TEXTURES);

        blit(matrix, x, y, 0, color.ordinal() * 5 * 2, WIDTH, HEIGHT);
        if (overlay != ServerBossEvent.Overlay.PROGRESS) {
            blit(matrix, x, y, 0, 80 + (overlay.ordinal() - 1) * 5 * 2, WIDTH, HEIGHT);
        }

        int i = (int) (percent * 183.0F);
        if (i > 0) {
            blit(matrix, x, y, 0, color.ordinal() * 5 * 2 + 5, i, HEIGHT);
            if (overlay != ServerBossEvent.Overlay.PROGRESS) {
                blit(matrix, x, y, 0, 80 + (overlay.ordinal() - 1) * 5 * 2 + 5, i, HEIGHT);
            }
        }

    }

}
