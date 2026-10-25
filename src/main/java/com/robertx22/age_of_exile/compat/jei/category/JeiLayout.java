package com.robertx22.age_of_exile.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers shared by the Age of Epoch JEI categories.
 */
final class DeiLayout {

    private JeiLayout() {
    }

    static void addLines(IRecipeExtrasBuilder builder, List<Component> lines, int x, int y, int maxWidth) {
        addLines(builder, lines, x, y, maxWidth, 1000);
    }

    static void addLines(IRecipeExtrasBuilder builder, List<Component> lines, int x, int y, int maxWidth, int maxHeight) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        List<FormattedText> text = new ArrayList<>();
        for (Component line : lines) {
            if (line != null) {
                text.add(line);
            }
        }
        if (text.isEmpty()) {
            return;
        }
        // One wrapped text widget for the whole explanation: JEI measures the real
        // wrapped lines instead of us reserving a fixed 10px per source line.
        builder.addText(text, maxWidth, Math.max(1, maxHeight))
            .setPosition(x, y);
    }

    static List<ItemStack> clean(List<ItemStack> stacks) {
        List<ItemStack> list = new ArrayList<>();
        if (stacks == null) {
            return list;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    static void addSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, List<ItemStack> stacks) {
        var slot = builder.addSlot(role, x, y)
            .setStandardSlotBackground();
        List<ItemStack> clean = clean(stacks);
        if (!clean.isEmpty()) {
            slot.addItemStacks(clean);
        }
    }
}
