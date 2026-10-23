package com.robertx22.age_of_exile.gui.overlays.spell_hotbar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.age_of_exile.capability.entity.CooldownsData;
import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ChatUtils;
import com.robertx22.library_of_exile.utils.CLOC;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.ChatFormatting;

import java.util.Locale;

public class SpellHotbarOverlay extends AbstractGui {

    private static final Identifier HOTBAR_TEX = Identifier.fromNamespaceAndPath(SlashRef.MODID,
        "textures/gui/spells/hotbar.png"
    );
    private static final Identifier COOLDOWN_TEX = Identifier.fromNamespaceAndPath(SlashRef.MODID,
        "textures/gui/spells/cooldown.png"
    );
    private static final Identifier SPELL_READY_TEX = Identifier.fromNamespaceAndPath(SlashRef.MODID,
        "textures/gui/spells/spell_ready.png"
    );
    private static final Identifier SPELl_NO_MANA = Identifier.fromNamespaceAndPath(SlashRef.MODID,
        "textures/gui/spells/no_mana.png"
    );

    private static final Identifier SPELL_ON_COOLDOWN = Identifier.fromNamespaceAndPath(SlashRef.MODID,
        "textures/gui/spells/on_cooldown.png"
    );

    private static final Identifier CHARGE = Identifier.fromNamespaceAndPath(SlashRef.MODID,
        "textures/gui/spells/charge_icon.png"
    );

    int CHARGE_SIZE = 9;

    static int WIDTH = 22;
    static int HEIGHT = 82;

    Minecraft mc = Minecraft.getInstance();

    EntitySpellCap.ISpellsCap data;

    public void onHudRender(PoseStack matrix, float v) {

        try {
            if (mc.player == null) {
                return;
            }
            if (mc.getDebugOverlay().showDebugScreen() || mc.player.isSpectator()) {
                return;
            }
            data = Load.spells(mc.player);
            if (ChatUtils.isChatOpen()) {
                return;
            }
            if (data == null) {
                return;
            }

            RenderSystem.enableBlend(); // enables transparency

            int x = 0;
            int y = mc.getWindow()
                .getGuiScaledHeight() / 2 - HEIGHT / 2;

            renderHotbar(matrix, x, y);

            for (int i = 0; i < 4; i++) {

                int place = i;
                if (Screen.hasShiftDown()) {
                    place += 4;
                }

                RenderSystem.enableBlend(); // enables transparency
                renderCurrentSpell(place, i, matrix);
                RenderSystem.disableBlend(); // enables transparency

            }
            RenderSystem.disableBlend(); // enables transparency
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void renderCurrentSpell(int place, int num, PoseStack matrix) {

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        int x = 3;
        int y = mc.getWindow()
            .getGuiScaledHeight() / 2 - HEIGHT / 2 + 3;

        y += num * 20;

        int xs = x;
        int ys = y;

        String txt = "";
        try {
            txt = CLOC.translate(KeybindsRegister.getSpellHotbar(place)
                    .getTranslatedKeyMessage())
                .toUpperCase(Locale.ROOT);
            if (txt.length() > 3) {
                txt = txt.substring(0, 2);
            }
        } catch (Exception ignored) {
        }

        Spell spell = null;
        try {
            spell = Load.spells(this.mc.player)
                .getSpellByNumber(place);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (spell == null) {
            if (!txt.isEmpty()) {
                GuiUtils.renderScaledText(matrix, xs + 13, ys + 12, 0.75D, txt, ChatFormatting.GRAY);
            }
            return;
        }

        try {
            if (Load.Unit(mc.player)
                .getCooldowns()
                .getCooldownTicks(spell.GUID()) > 1) {
                com.robertx22.forge_compat.client.AbstractGui.bindTexture(SPELL_ON_COOLDOWN);
            } else {
                com.robertx22.forge_compat.client.AbstractGui.bindTexture(SPELL_READY_TEX);
            }

            this.blit(matrix, xs, ys, 16, 16, 0, 0, 20, 20, 20, 20);

            if (spell != null) {
                com.robertx22.forge_compat.client.AbstractGui.bindTexture(spell.getIconLoc());
                this.blit(matrix, xs, ys, 0, 0, 16, 16, 16, 16);

                if (spell.config.charges > 0) {
                    int charges = data.getCastingData().charges.getCharges(spell.config.charge_name);

                    if (charges == 0) {
                        float needed = (float) spell.config.charge_regen;
                        float currentticks = (float) data.getCastingData().charges.getCurrentTicksChargingOf(spell.config.charge_name);

                        float ticksleft = needed - currentticks;

                        float percent = ticksleft / needed;
                        percent = Mth.clamp(percent, 0, 1F);
                        drawCooldown(percent, matrix, xs, ys);

                    }

                    com.robertx22.forge_compat.client.AbstractGui.bindTexture(CHARGE);
                    int chargex = x + 21;

                    for (int i = 0; i < charges; i++) {
                        this.blit(matrix, chargex, y + 5, 0, 0, CHARGE_SIZE, CHARGE_SIZE, CHARGE_SIZE, CHARGE_SIZE);
                        chargex += CHARGE_SIZE + 1;
                    }

                } else {

                    CooldownsData cds = Load.Unit(mc.player)
                        .getCooldowns();

                    float percent = (float) cds.getCooldownTicks(spell.GUID()) / (float) cds.getNeededTicks(spell.GUID());
                    if (cds.getCooldownTicks(spell.GUID()) > 1) {
                        percent = Mth.clamp(percent, 0, 1F);
                        drawCooldown(percent, matrix, xs, ys);
                    }

                    int cdsec = cds.getCooldownTicks(spell.GUID()) / 20;
                    if (cdsec > 1) {
                        String stext = cdsec + "с";
                        GuiUtils.renderScaledText(matrix, xs + 27, ys + 10, 0.75F, stext, ChatFormatting.YELLOW);
                    }
                }

                if (!txt.isEmpty()) {
                    GuiUtils.renderScaledText(matrix,
                        xs + 14, ys + 12, 1, txt, ChatFormatting.GREEN);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void drawCooldown(float percent, PoseStack matrix, int x, int y) {

        com.robertx22.forge_compat.client.AbstractGui.bindTexture(COOLDOWN_TEX);
        this.blit(matrix, x, y, 0, 0, 16, (int) (16 * percent), 16, 16);
    }

    private void renderHotbar(PoseStack matrix, int x, int y) {

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        com.robertx22.forge_compat.client.AbstractGui.bindTexture(HOTBAR_TEX);

        this.blit(matrix, x, y, 0, 0, WIDTH, HEIGHT);

    }

}