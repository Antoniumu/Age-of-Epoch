package com.robertx22.age_of_exile.gui.screens.spell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.guides.GuideAdvisor;
import com.robertx22.age_of_exile.gui.TextUtils;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.vanilla_mc.packets.AllocateSpellPacket;
import com.robertx22.age_of_exile.vanilla_mc.packets.spells.SetupHotbarPacket;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.ImageButton;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class LearnSpellButton extends ImageButton {

    static Identifier SPELL_SLOT = SlashRef.guiId("spells/slots/spell");

    public static int BUTTON_SIZE_X = 18;
    public static int BUTTON_SIZE_Y = 18;

    Minecraft mc = Minecraft.getInstance();
    SpellScreen screen;

    public Spell spell;

    public LearnSpellButton(SpellScreen screen, Spell spell, int xPos, int yPos) {
        super(xPos, yPos, BUTTON_SIZE_X, BUTTON_SIZE_Y, 0, 0, BUTTON_SIZE_Y, SPELL_SLOT, (button) -> {
            if (SpellScreen.IS_PICKING_HOTBAR_SPELL) {
                SpellScreen.IS_PICKING_HOTBAR_SPELL = false;
                Packets.sendToServer(new SetupHotbarPacket(spell, SpellScreen.NUMBER_OF_HOTBAR_FOR_PICKING));
            } else {
                Packets.sendToServer(new AllocateSpellPacket(screen.currentSchool(), spell, AllocateSpellPacket.ACTION.ALLOCATE));
            }
        });
        this.screen = screen;
        this.spell = spell;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();

        AbstractGui.bindTexture(SPELL_SLOT);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, x, y, BUTTON_SIZE_X, BUTTON_SIZE_X, 0, 0, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X);

        AbstractGui.bindTexture(spell.getIconLoc());
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, x + 1, y + 1, 16, 16, 0, 0, 16, 16, 16, 16);

        int currentlvl = Load.spells(mc.player)
            .getLevelOf(spell.GUID());
        int maxlvl = spell.getMaxLevel();
        String lvltext = currentlvl + "/" + maxlvl;
        ChatFormatting col = currentlvl > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY;
        TextUtils.renderText(matrix, 0.75F, lvltext, x + BUTTON_SIZE_X / 2, (int) (y + BUTTON_SIZE_Y * 0.85F), col);

        SpellSchool guideSchool = GuideAdvisor.spellSchool(GuideAdvisor.activeGuide(mc.player));
        if (guideSchool != null && screen.currentSchool() != null
            && guideSchool.GUID().equals(screen.currentSchool().GUID())
            && GuideAdvisor.isRecommendedSpell(mc.player, spell.GUID())) {
            int alpha = 130 + (int) (110 * (0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 240.0)));
            int color = (Math.min(255, alpha) << 24) | 0x0058FF8C;
            AbstractGui.fill(matrix, x - 1, y - 1, x + BUTTON_SIZE_X + 1, y, color);
            AbstractGui.fill(matrix, x - 1, y + BUTTON_SIZE_Y, x + BUTTON_SIZE_X + 1, y + BUTTON_SIZE_Y + 1, color);
            AbstractGui.fill(matrix, x - 1, y, x, y + BUTTON_SIZE_Y, color);
            AbstractGui.fill(matrix, x + BUTTON_SIZE_X, y, x + BUTTON_SIZE_X + 1, y + BUTTON_SIZE_Y, color);
        }
    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (isInside(x, y)) {

            List<Component> tooltip = new ArrayList<>();

            TooltipInfo info = new TooltipInfo(mc.player);

            tooltip.addAll(spell.GetTooltipString(info));

            int reqlvl = screen.currentSchool()
                .getLevelNeededToAllocate(screen.currentSchool().spells.get(spell.GUID()));

            tooltip.add(Component.literal("Требуемый уровень: " + reqlvl).withStyle(ChatFormatting.RED));

            if (GuideAdvisor.isRecommendedSpell(mc.player, spell.GUID())) {
                tooltip.add(Component.literal("★ Следующее заклинание по гайду").withStyle(ChatFormatting.AQUA));
            }

            int currentlvl = Load.spells(mc.player).getLevelOf(spell.GUID());
            if (currentlvl > 0) {
                tooltip.add(Component.literal("ЛКМ — Повысить ур. | Зажмите и перетащите в ячейку внизу").withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.literal("ЛКМ — Изучить заклинание").withStyle(ChatFormatting.GREEN));
            }

            GuiUtils.renderTooltip(matrix, tooltip, x, y);

        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, BUTTON_SIZE_X, BUTTON_SIZE_Y, x, y);
    }

}
