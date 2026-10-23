package com.robertx22.divine_missions.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.forge_compat.client.RenderSystem;
import com.robertx22.divine_missions.components.PlayerMissionCap;
import com.robertx22.divine_missions.main.DivineMissions;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.resources.Identifier;

public class PickMissionButton extends ImageButton {

    public int missionNumber;

    static Identifier TEX = DivineMissions.id("textures/gui/pick.png");

    public PickMissionButton(int missionNumber, int x, int y, IPressable pressAction) {
        super(x, y, 16, 16, 0, 0, 0, DivineMissions.id("textures/gui/pick.png"), pressAction);
        this.missionNumber = missionNumber;
    }

    @Override
    public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {

        Minecraft mc = Minecraft.getInstance();

        if (PlayerMissionCap.get(mc.player).missionData.missions.containsKey(missionNumber)) {

            com.robertx22.forge_compat.client.AbstractGui.bindTexture(TEX);

            RenderSystem.enableDepthTest();
            blit(matrices, this.x, this.y, 0, 0, this.width, this.height, 16, 16);

            if (this.isHovered()) {
                this.renderToolTip(matrices, mouseX, mouseY);
            }
        }

    }
}
