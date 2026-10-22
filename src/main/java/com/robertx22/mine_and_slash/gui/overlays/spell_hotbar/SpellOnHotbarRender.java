package com.robertx22.mine_and_slash.gui.overlays.spell_hotbar;

import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.library_of_exile.utils.CLOC;
import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.capability.entity.SummonedData;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.robertx22.forge_compat.client.settings.KeyModifier;

import java.util.Locale;

public class SpellOnHotbarRender {
    static int CHARGE_SIZE = 20;

    private static final Identifier CHARGE = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/charges/full_charges.png");
    private static final Identifier LOW_CHARGE = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/charges/low_charges.png");
    private static final Identifier NO_CHARGE = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/charges/no_charges.png");
    private static final Identifier KEY_BG = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/keybind_bg.png");
    private static final Identifier MOD_BG = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/modbg.png");
    private static final Identifier COOLDOWN_TEX = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/spells/cooldown.png");


    public int place;
    public GuiGraphicsExtractor gui;
    public int x;
    public int y;
    public Spell spell;

    int keyNum = 0;

    boolean disableKeyRender = false;

    Minecraft mc = Minecraft.getInstance();

    boolean horizontal;

    public SpellOnHotbarRender(boolean horizontal, int place, GuiGraphicsExtractor gui, int x, int y) {
        this.place = place;
        this.gui = gui;
        this.horizontal = horizontal;
        this.x = x;
        this.y = y;

        if (ClientConfigs.getConfig().HOTBAR_SWAPPING.get()) {
            if (place > 3) {
                if (!SpellKeybind.IS_ON_SECONd_HOTBAR) {
                    disableKeyRender = true;
                }
            } else {
                if (SpellKeybind.IS_ON_SECONd_HOTBAR) {
                    disableKeyRender = true;
                }
            }

        }

        this.keyNum = place;

        if (ClientConfigs.getConfig().HOTBAR_SWAPPING.get()) {
            if (SpellKeybind.IS_ON_SECONd_HOTBAR) {
                keyNum -= 4;
            }
        }

        this.spell = Load.player(ClientOnly.getPlayer()).getSkillGemInventory().getHotbarGem(place).getSpell();
    }


    public void render() {
        var mc = Minecraft.getInstance();


        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (horizontal) {
            x += place * 20;
        } else {
            y += place * 20;
        }

        try {
            int xs = (int) (x);
            int ys = (int) (y);


            if (spell != null) {
                com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, spell.getIconLoc(), xs, ys, 0, 0, 16, 16, 16, 16);

                if (spell.config.charges > 0) {
                    drawCharge(xs, ys);
                } else {

                    CooldownsData cds = Load.Unit(mc.player).getCooldowns();
                    float percent = (float) cds.getCooldownTicks(spell.GUID()) / (float) cds.getNeededTicks(spell.GUID());
                    drawCooldown(gui, percent, xs, ys);

                }

                SummonedData summonedData = Load.player(mc.player).getSummonedData();
                if (summonedData.getSummonedAmount(spell.GUID()) > 0) {
                    drawSummoned(xs + 3, ys + 3, summonedData.getSummonedAmount(spell.GUID()));
                }


                int xkey = xs + 15;
                int ykey = y + 14;

                drawKey(xkey, ykey);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawCharge(int xs, int ys) {

        int charges = Load.player(ClientOnly.getPlayer()).spellCastingData.charges.getCharges(spell.config.charge_name);

        Identifier chargeTex = CHARGE;

        if (charges == 0) {
            chargeTex = NO_CHARGE;

        } else {
            if (charges != spell.config.charges) {
                chargeTex = LOW_CHARGE;
            }
        }

        if (charges == 0) {
            float needed = (float) spell.config.charge_regen;
            float currentticks = (float) Load.player(mc.player)
                    .spellCastingData.charges.getCurrentTicksChargingOf(spell.config.charge_name);

            float ticksleft = needed - currentticks;

            float percent = ticksleft / needed;
            percent = Mth.clamp(percent, 0, 1F);
            drawCooldown(gui, percent, xs, ys);
        }

        int chargex = x - 2;

        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, chargeTex, chargex, y - 2, 0, 0, CHARGE_SIZE, CHARGE_SIZE, CHARGE_SIZE, CHARGE_SIZE);
    }

    private void drawKey(int xkey, int ykey) {
        if (disableKeyRender) {
            return;
        }
        int bgsize = 10;

        RenderSystem.enableBlend(); // enables transparency

        float alpha = 0.75f;
        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(alpha, alpha, alpha, alpha);
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, KEY_BG, xkey - 6, ykey - 6, 0, 0, bgsize, bgsize, bgsize, bgsize);

        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1);


        String txt = CLOC.translate(KeybindsRegister.getSpellHotbar(keyNum).key.getTranslatedKeyMessage()).toUpperCase(Locale.ROOT);
        txt = txt.substring(0, 1);
        if (KeybindsRegister.getSpellHotbar(keyNum).key.isUnbound()) {
            if (disableKeyRender) {
                txt = "";
            } else {
                //txt = "UNBOUND KEY";
                txt = "";
            }
        }
        // todo renderScaledText doesnt do push and pop but does antiscale.. FIX THIS
        GuiUtils.renderScaledText(gui, xkey - 1, ykey, 1, txt, ChatFormatting.GREEN);

        var key = KeybindsRegister.getSpellHotbar(keyNum);
        if (false) {

            RenderSystem.enableBlend(); // enables transparency
            com.robertx22.forge_compat.client.RenderSystem.setShaderColor(alpha, alpha, alpha, alpha);
            com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, MOD_BG, xkey - 18, ykey - 6, 0, 0, 13, bgsize, 13, bgsize);
            com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1F);

            String modtext = "";
            GuiUtils.renderScaledText(gui, xkey - 11, ykey, 0.6F, modtext, ChatFormatting.YELLOW);
        }


        RenderSystem.disableBlend(); // enables transparency
    }

    private void drawCooldown(GuiGraphicsExtractor gui, float percent, int xs, int ys) {
        percent = Mth.clamp(percent, 0, 1F);

        CooldownsData cds = Load.Unit(mc.player).getCooldowns();

        if (cds.getCooldownTicks(spell.GUID()) > 1) {
            com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, COOLDOWN_TEX, this.x, this.y, 0, 0, 16, (int) (16 * percent), 16, 16);
        } else {
            return;
        }

        int cdsec = cds.getCooldownTicks(spell.GUID()) / 20;
        if (cdsec > 1) {
            String stext = cdsec + "s";
            //  GuiUtils.renderScaledText(gui, xs + 27, ys + 10, 0.75F, stext, ChatFormatting.YELLOW);
        }
    }

    private void drawSummoned(int x, int y, int summonedAmount) {
        int bgsize = 10;
        float alpha = 0.75f;
        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(alpha, alpha, alpha, alpha);
        com.robertx22.forge_compat.client.GuiBlitHelper.blit(gui, KEY_BG, x - 6, y - 6, 0, 0, bgsize, bgsize, bgsize, bgsize);

        com.robertx22.forge_compat.client.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1);


        String txt = String.valueOf(summonedAmount);
        // todo renderScaledText doesnt do push and pop but does antiscale.. FIX THIS
        GuiUtils.renderScaledText(gui, x - 1, y, 1, txt, ChatFormatting.RED);

        RenderSystem.disableBlend(); // enables transparency
    }
}
