package com.robertx22.age_of_exile.gui.screens.spell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.age_of_exile.database.data.synergy.Synergy;
import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.guides.GuideAdvisor;
import com.robertx22.age_of_exile.database.data.value_calc.LevelProvider;
import com.robertx22.age_of_exile.gui.TextUtils;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.vanilla_mc.packets.spells.AllocateSynergyPacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.Level;
public class LearnSynergyButton extends ImageButton {

    static Identifier SPELL_SLOT = SlashRef.guiId("spells/slots/synergy");

    public static int BUTTON_SIZE_X = 18;
    public static int BUTTON_SIZE_Y = 18;

    Minecraft mc = Minecraft.getInstance();
    SpellScreen screen;

    Synergy synergy;

    public LearnSynergyButton(SpellScreen screen, Synergy syn, int xPos, int yPos) {
        super(xPos, yPos, BUTTON_SIZE_X, BUTTON_SIZE_Y, 0, 0, BUTTON_SIZE_Y, SPELL_SLOT, (button) -> {
            Packets.sendToServer(new AllocateSynergyPacket(screen.currentSchool(), syn, AllocateSynergyPacket.ACTION.ALLOCATE));

        });
        this.screen = screen;
        this.synergy = syn;

    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();

        com.robertx22.forge_compat.client.AbstractGui.bindTexture(SPELL_SLOT);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, x, y, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X);

        com.robertx22.forge_compat.client.AbstractGui.bindTexture(synergy.getIconLoc());
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, x + 1, y + 1, 16, 16, 16, 16, 16, 16);

        int currentlvl = Load.spells(mc.player)
            .getLevelOf(synergy.GUID());
        int maxlvl = synergy.getMaxLevel();
        String lvltext = currentlvl + "/" + maxlvl;
        TextUtils.renderText(matrix, 0.8F, lvltext, x + BUTTON_SIZE_X / 2, (int) (y + BUTTON_SIZE_Y * 0.85F), ChatFormatting.GREEN);

        // подсветка рекомендованной гайдом синергии (та же логика, что у заклинаний)
        SpellSchool guideSchool = GuideAdvisor.spellSchool(GuideAdvisor.activeGuide(mc.player));
        if (guideSchool != null && screen.currentSchool() != null
            && guideSchool.GUID().equals(screen.currentSchool().GUID())
            && GuideAdvisor.isRecommendedSpell(mc.player, synergy.GUID())) {
            int alpha = 130 + (int) (110 * (0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 240.0)));
            int color = (Math.min(255, alpha) << 24) | 0x0058FF8C;
            com.robertx22.forge_compat.client.AbstractGui.fill(matrix, x - 1, y - 1, x + BUTTON_SIZE_X + 1, y, color);
            com.robertx22.forge_compat.client.AbstractGui.fill(matrix, x - 1, y + BUTTON_SIZE_Y, x + BUTTON_SIZE_X + 1, y + BUTTON_SIZE_Y + 1, color);
            com.robertx22.forge_compat.client.AbstractGui.fill(matrix, x - 1, y, x, y + BUTTON_SIZE_Y, color);
            com.robertx22.forge_compat.client.AbstractGui.fill(matrix, x + BUTTON_SIZE_X, y, x + BUTTON_SIZE_X + 1, y + BUTTON_SIZE_Y, color);
        }
    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (isInside(x, y)) {

            List<Component> tooltip = new ArrayList<>();

            tooltip.add(synergy.locName()
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

            TooltipInfo info = new TooltipInfo(mc.player);

            synergy.getStats(new LevelProvider(mc.player, synergy))
                .forEach(s -> {
                    tooltip.addAll(s.GetTooltipString(info));
                });

            int reqlvl = screen.currentSchool()
                .getLevelNeededToAllocate(screen.currentSchool().synergies.get(synergy.GUID()));

            tooltip.add(Component.literal("Требуемый уровень: " + reqlvl).withStyle(ChatFormatting.RED));

            if (GuideAdvisor.isRecommendedSpell(mc.player, synergy.GUID())) {
                tooltip.add(Component.literal("★ Следующая синергия по гайду").withStyle(ChatFormatting.AQUA));
            }

            GuiUtils.renderTooltip(matrix, tooltip, x, y);

        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, BUTTON_SIZE_X, BUTTON_SIZE_Y, x, y);
    }

}

