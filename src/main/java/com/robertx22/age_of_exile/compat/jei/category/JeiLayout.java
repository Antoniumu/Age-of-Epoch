package com.robertx22.age_of_exile.compat.jei.category;

import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Small helpers shared by the Age of Epoch JEI categories.
 */
final class JeiLayout {

    private static final int LINE_HEIGHT = 10;

    private JeiLayout() {
    }

    static void addLines(IRecipeExtrasBuilder builder, List<Component> lines, int x, int y, int maxWidth) {
        if (lines == null) {
            return;
        }
        int yy = y;
        for (Component line : lines) {
            if (line == null) {
                continue;
            }
            builder.addText(line, maxWidth, LINE_HEIGHT)
                .setPosition(x, yy);
            yy += LINE_HEIGHT;
        }
    }
}
