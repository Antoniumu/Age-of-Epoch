package com.robertx22.age_of_exile.player_skills.crafting_inv;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftingProcessData;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.forge_compat.common.CompatChatFormatting;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ProfCraftScreen extends AbstractContainerScreen<ProfCraftContainer> {
    private Identifier texture = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/crafting/background.png");

    public ProfCraftScreen(ProfCraftContainer handler, Inventory inventory, Component text) {
        super(handler, inventory, Component.literal(""));

        imageWidth = 176;
        imageHeight = 207;
    }

    @Override
    protected void renderBg(PoseStack matrices, float delta, int mouseX, int mouseY) {
        com.robertx22.forge_compat.client.AbstractGui.bindTexture(texture);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrices, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        Minecraft mc = Minecraft.getInstance();

        // Левая панель: причина отказа, либо (если рецепт готов) шанс успеха и множитель статов
        Component reason = menu.getFailReason();
        if (reason != null) {
            drawInfoLines(matrices, mc, mc.font.split(reason, 85), CompatChatFormatting.getColor(ChatFormatting.RED));
        } else {
            ItemStack preview = menu.getResultPreview();
            CraftingProcessData data = preview.isEmpty() ? null : StackSaving.CRAFT_PROCESS.loadFrom(preview);
            if (data != null && mc.player != null) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                lines.addAll(mc.font.split(Component.literal("Шанс успеха: " + data.getSuccessChance(mc.player) + "%"), 85));
                lines.addAll(mc.font.split(Component.literal("Множитель статов: " + Math.round(data.getStatMulti() * 100) + "%"), 85));
                drawInfoLines(matrices, mc, lines, CompatChatFormatting.getColor(ChatFormatting.GREEN));
            }
        }

        this.renderTooltip(matrices, mouseX, mouseY);
        buttons.forEach(x -> x.renderToolTip(matrices, mouseX, mouseY));
    }

    private void drawInfoLines(PoseStack matrices, Minecraft mc, List<FormattedCharSequence> lines, int color) {
        int ypos = this.topPos + 10;
        for (FormattedCharSequence txt : lines) {
            com.robertx22.forge_compat.client.AbstractGui.drawShadow(mc.font, matrices, txt,
                this.leftPos + 10, ypos, color);
            ypos += mc.font.lineHeight + 2;
        }
    }

    @Override
    protected void renderLabels(PoseStack matrices, int mouseX, int mouseY) {
        // dont draw the damn name

    }

}
