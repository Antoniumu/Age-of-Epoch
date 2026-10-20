package com.robertx22.age_of_exile.gui.screens.dungeon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.dimension.dungeon_data.TeamSize;
import com.robertx22.age_of_exile.dimension.packets.StartDungeonPacket;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.CLOC;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class StartDungeonButton extends ImageButton {

    public static int SIZE_X = 49;
    public static int SIZE_Y = 16;

    static Identifier ID = SlashRef.guiId("dungeon/start_dungeon_button");

    Minecraft mc = Minecraft.getInstance();

    TeamSize teamSize;

    public StartDungeonButton(TeamSize teamSize, DungeonInfoScreen screen, int xPos, int yPos) {
        super(xPos, yPos, SIZE_X, SIZE_Y, 0, 0, SIZE_Y, ID, (button) -> {
            Packets.sendToServer(new StartDungeonPacket(teamSize, screen.teleporterPos));

            screen.onClose();
        });
        this.teamSize = teamSize;
    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (isInside(x, y)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Выберите это подземелье для начала."));

            if (teamSize.requiredMemberAmount > 1) {
                tooltip.add(Component.literal("Запускает подземелье в командном режиме."));
                tooltip.add(Component.literal("Для игры требуется команда (/age_of_exile teams)."));
                tooltip.add(Component.literal("Враги намного сильнее, но и награды выше!"));

                if (teamSize.requiredMemberAmount > 2) {
                    tooltip.add(Component.literal("Рекомендуются Танк, Целитель и Боец."));
                }
            }

            GuiUtils.renderTooltip(matrix, tooltip, x, y);

        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, SIZE_X, SIZE_Y, x, y);
    }

    @Override
    public void renderButton(PoseStack matrix, int x, int y, float f) {
        super.renderButton(matrix, x, y, f);

        String txt = CLOC.translate(teamSize.word.locName());

        int width = mc.font.width(txt);

        com.robertx22.forge_compat.client.AbstractGui.drawShadow(mc.font, matrix, txt, this.x + (SIZE_X / 2F - (width / 2F)), this.y + 3, com.robertx22.forge_compat.common.CompatChatFormatting.getColor(ChatFormatting.WHITE));
    }

}
