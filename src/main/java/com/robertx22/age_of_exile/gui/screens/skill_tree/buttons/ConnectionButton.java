package com.robertx22.age_of_exile.gui.screens.skill_tree.buttons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.data.talent_tree.TalentTree;
import com.robertx22.age_of_exile.gui.screens.skill_tree.SkillTreeScreen;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public class ConnectionButton extends ImageButton {

    public static int SIZE = 6;

    public static Identifier ID = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/skill_tree/lines.png");

    TalentTree school;
    PointData one;
    PointData two;

    public int origX;
    public int origY;

    SkillTreeScreen screen;
    Minecraft mc = Minecraft.getInstance();

    public Perk.Connection connection;

    public ConnectionButton(SkillTreeScreen screen, TalentTree school, PointData one, PointData two, int x, int y) {
        super(x, y, SIZE, SIZE, 0, 0, 0, ID, (action) -> {
        });
        this.school = school;
        this.one = one;
        this.two = two;
        this.screen = screen;
        this.origX = x;
        this.origY = y;
        this.active = false;
    }

    public void refreshConnection(RPGPlayerData data) {
        if (data != null && data.talents != null) {
            this.connection = data.talents.getConnection(school, one, two);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    @Override
    public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
        // do nothing. use the custom method
    }

    // render here so the bind texture is only called once
    // because there are thousands of connector buttons
    public void renderButtonForReal(PoseStack matrices, int mouseX, int mouseY, float delta) {
        if (connection == null) {
            refreshConnection(Load.playerRPGData(mc.player));
        }

        if (connection == Perk.Connection.POSSIBLE) {
            blit(matrices, this.x, this.y, 0, 0, 6, 6);
        } else if (connection == Perk.Connection.LINKED) {
            blit(matrices, this.x, this.y, 6, 0, 6, 6);
        } else if (connection == Perk.Connection.BLOCKED) {
            blit(matrices, this.x, this.y, 12, 0, 6, 6);
        }
    }
}
