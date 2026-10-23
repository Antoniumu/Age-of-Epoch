package com.robertx22.age_of_exile.player_skills.items.backpacks.mat_pouch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

public class MatBagScreen extends AbstractContainerScreen<MatBagContainer> {
    private Identifier texture = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/backpack/pouch.png");

    public MatBagScreen(MatBagContainer handler, Inventory inventory, Component text) {
        super(handler, inventory, Component.literal(""));

        imageWidth = 176;
        imageHeight = 222;
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
        this.renderTooltip(matrices, mouseX, mouseY);
        buttons.forEach(x -> x.renderToolTip(matrices, mouseX, mouseY));
    }

    @Override
    protected void renderLabels(PoseStack matrices, int mouseX, int mouseY) {
        // dont draw the damn name

    }

}
