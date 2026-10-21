package com.robertx22.age_of_exile.gui.screens.spell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.ImageButton;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class PickHotbarButton extends ImageButton {

    static Identifier SPELL_SLOT = SlashRef.guiId("spells/pick_hotbar");

    public static int BUTTON_SIZE_X = 20;
    public static int BUTTON_SIZE_Y = 20;

    Minecraft mc = Minecraft.getInstance();
    SpellScreen screen;

    public int num;

    public PickHotbarButton(SpellScreen screen, int num, int xPos, int yPos) {
        super(xPos, yPos, BUTTON_SIZE_X, BUTTON_SIZE_Y, 0, 0, BUTTON_SIZE_Y, SPELL_SLOT, (button) -> {
            SpellScreen.IS_PICKING_HOTBAR_SPELL = true;
            SpellScreen.NUMBER_OF_HOTBAR_FOR_PICKING = num;
            screen.setStatusMessage("Выберите изученное заклинание для ячейки #" + (num + 1));
        });
        this.screen = screen;
        this.num = num;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();

        Spell spell = Load.spells(mc.player)
            .getSpellByNumber(num);

        AbstractGui.bindTexture(SPELL_SLOT);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, x, y, BUTTON_SIZE_X, BUTTON_SIZE_X, 0, 0, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X, BUTTON_SIZE_X);

        if (SpellScreen.IS_PICKING_HOTBAR_SPELL && SpellScreen.NUMBER_OF_HOTBAR_FOR_PICKING == this.num) {
            int col = 0xFF55FF55;
            AbstractGui.fill(matrix, x - 1, y - 1, x + BUTTON_SIZE_X + 1, y, col);
            AbstractGui.fill(matrix, x - 1, y + BUTTON_SIZE_Y, x + BUTTON_SIZE_X + 1, y + BUTTON_SIZE_Y + 1, col);
            AbstractGui.fill(matrix, x - 1, y, x, y + BUTTON_SIZE_Y, col);
            AbstractGui.fill(matrix, x + BUTTON_SIZE_X, y, x + BUTTON_SIZE_X + 1, y + BUTTON_SIZE_Y, col);
        }

        if (spell != null) {
            AbstractGui.bindTexture(spell.getIconLoc());
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            blit(matrix, x + 2, y + 2, 16, 16, 0, 0, 16, 16, 16, 16);
        }

    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (isInside(x, y)) {

            List<Component> tooltip = new ArrayList<>();

            TooltipInfo info = new TooltipInfo(mc.player);

            Spell spell = Load.spells(mc.player)
                .getSpellByNumber(num);

            tooltip.add(Component.literal("Ячейка панели заклинаний #" + (num + 1)).withStyle(ChatFormatting.GOLD));
            if (spell != null) {
                tooltip.addAll(spell.GetTooltipString(info));
                tooltip.add(Component.literal("Перетащите сюда другое заклинание для замены").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.literal("Перетащите сюда изученное заклинание мышью").withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("или нажмите ЛКМ по ячейке, а затем по заклинанию").withStyle(ChatFormatting.GRAY));
            }
            GuiUtils.renderTooltip(matrix, tooltip, x, y);

        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, BUTTON_SIZE_X, BUTTON_SIZE_Y, x, y);
    }

}
