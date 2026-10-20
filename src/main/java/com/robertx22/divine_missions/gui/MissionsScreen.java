package com.robertx22.divine_missions.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.divine_missions.components.PlayerMissionCap;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.packets.PickMissionPacket;
import com.robertx22.library_of_exile.gui.ItemSlotButton;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

import java.util.Map;

import net.minecraft.ChatFormatting;
public class MissionsScreen extends BaseScreen {

    static int X = 146;
    static int Y = 180;

    public MissionsScreen() {
        super(146, 180);
    }

    static Identifier BACKGROUND = DivineMissions.id("textures/gui/background.png");

    @Override
    public void init() {
        super.init();

        PlayerMissionCap missions = PlayerMissionCap.get(mc.player);

        int startX = 25;

        int x = guiLeft + startX;
        int y = guiTop + startX;

        int i = 0;

        for (Map.Entry<Integer, ItemStack> entry : missions.missionData.missions.entrySet()) {

            ItemSlotButton itemslot = new ItemSlotButton(entry.getValue(), x, y);
            itemslot.renderFancyBorder = true;
            this.addButton(itemslot);

            this.addButton(new PickMissionButton(entry.getKey(), x + 1, y + 20, action -> {
                Packets.sendToServer(new PickMissionPacket(entry.getKey()));
            }));

            x += 40;

            i++;

            if (i % 3 == 0) {
                y += 40;
                x = guiLeft + startX;
            }
        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack matrix, int x, int y, float ticks) {

        renderBackground(matrix, BACKGROUND);
        super.render(matrix, x, y, ticks);

        /*
        int picks = PlayerMissionCap.get(mc.player).missionData.picks;
        super.render(matrix, x, y, ticks);
        String txt = CLOC.translate(DivineMissions.ofTranslation("picks"))
            + ": " + picks;

        if (picks == 0) {
            txt = CLOC.translate(DivineMissions.ofTranslation("come_back_later"));
        }

        GuiUtils.renderScaledText(matrix, guiLeft + X / 2, guiTop + Y + 10, 1F, txt, ChatFormatting.GREEN);

         */

        buttons.forEach(b -> b.renderToolTip(matrix, x, y));

    }
}
