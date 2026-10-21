package com.robertx22.age_of_exile.gui.screens.spell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.age_of_exile.database.data.synergy.Synergy;
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

            GuiUtils.renderTooltip(matrix, tooltip, x, y);

        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, BUTTON_SIZE_X, BUTTON_SIZE_Y, x, y);
    }

}

