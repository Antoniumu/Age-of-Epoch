package com.robertx22.age_of_exile.player_skills.items.backpacks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.player_skills.items.backpacks.upgrades.BackpackUpgradeItem;
import com.robertx22.library_of_exile.gui.ItemSlotButton;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

public class BackpackScreen extends AbstractContainerScreen<BackpackContainer> {
    // The constructor replaces this with the tier-specific background. Keep a
    // real texture as the default so a partially-built screen never points at
    // a missing file.
    private Identifier texture = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/backpack/0.png");

    public BackpackScreen(BackpackContainer handler, Inventory inventory, Component text) {
        super(handler, inventory, Component.literal(""), 176, 233);


        this.texture = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/backpack/" + handler.info.tier + ".png");
    }

    @Override
    public void init() {
        super.init();

        int i = 0;
        for (BackpackUpgradeItem item : this.menu.info.data.getUpgrades()) {
            if (i < 9) {
                this.addButton(new ItemSlotButton(new ItemStack(item), this.leftPos + 7 + (i * 18), this.topPos + 11));
            }
            i++;
        }

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
