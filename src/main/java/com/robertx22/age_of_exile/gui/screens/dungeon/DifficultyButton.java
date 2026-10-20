package com.robertx22.age_of_exile.gui.screens.dungeon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.tiers.base.Difficulty;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.utils.GuiUtils;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.ImageButton;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DifficultyButton extends ImageButton {

    public static int xSize = 32;
    public static int ySize = 32;

    static Identifier LOC = SlashRef.guiId("dungeon/difficulty_button");

    Minecraft mc = Minecraft.getInstance();

    public Difficulty tier;

    public DifficultyButton(Difficulty tier, int xPos, int yPos) {
        super(xPos + 1, yPos + 1, xSize, ySize, 0, 0, ySize + 1, LOC, (button) -> {
        });
        this.tier = tier;
    }

    @Override
    public void renderToolTip(PoseStack matrix, int x, int y) {
        if (isInside(x, y)) {

            List<Component> tooltip = new ArrayList<>();

            tooltip.addAll(tier
                .getTooltip());

            tooltip.add(Component.literal(""));

            for (GearRarity r : ExileDB.GearRarities()
                .getList()) {
                if (r.drops_after_tier > -1) {
                    if (tier.rank >= r.drops_after_tier) {
                        tooltip.add(Component.literal("").append(r.locName())
                            .append(" items can drop")
                            .withStyle(r.textFormatting()));
                    }
                }
            }

            GuiUtils.renderTooltip(matrix, tooltip, x, y);
        }
    }

    public boolean isInside(int x, int y) {
        return GuiUtils.isInRect(this.x, this.y, xSize, ySize, x, y);
    }

}
