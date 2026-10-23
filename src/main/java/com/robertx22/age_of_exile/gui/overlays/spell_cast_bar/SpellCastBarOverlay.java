package com.robertx22.age_of_exile.gui.overlays.spell_cast_bar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.forge_compat.client.AbstractGui;
import net.minecraft.client.Minecraft;

public class SpellCastBarOverlay extends AbstractGui {

    static int WIDTH = 182;
    static int HEIGHT = 5;

    private static final int BORDER_COLOR = 0xFF000000;
    private static final int TRACK_COLOR = 0xFF3A3A3A;
    private static final int FILL_COLOR = 0xFFB03CE0;
    private static final int NOTCH_COLOR = 0x66000000;

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

            render(matrix, x, y, percent);
        }
    }

    // Drawn with plain fills on purpose: the old version blitted from
    // "textures/gui/bars.png" without ever binding it, so it sampled whatever
    // texture the spell hotbar bound last (a spell icon) and showed up as a
    // weird stretched stripe. That texture file also no longer exists in
    // modern Minecraft (boss bars moved into the GUI sprite atlas).
    private void render(PoseStack matrix, int x, int y, float percent) {

        percent = Math.max(0.0F, Math.min(1.0F, percent));

        // border + empty track
        fill(matrix, x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, BORDER_COLOR);
        fill(matrix, x, y, x + WIDTH, y + HEIGHT, TRACK_COLOR);

        // progress
        int filled = (int) (percent * WIDTH);
        if (filled > 0) {
            fill(matrix, x, y, x + filled, y + HEIGHT, FILL_COLOR);
        }

        // notches, like the old notched boss bar look
        int notches = 20;
        for (int i = 1; i < notches; i++) {
            int notchX = x + (WIDTH * i) / notches;
            fill(matrix, notchX, y, notchX + 1, y + HEIGHT, NOTCH_COLOR);
        }
    }

}
