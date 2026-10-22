package com.robertx22.mine_and_slash.a_libraries.jei;

import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.runewords.RunewordRecipe;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RunewordExtension implements IRecipeCategoryExtension {
    private static final int SLOT_SIZE = 18;
    private static final int LINES_GAP = 2;
    private static final int STATS_Y_GAP = 8;
    private static final int NAME_Y_POSITION = 0;

    int maximumResultItemsPerRow;
    int categoryWidth;
    int lineHeight;

    int statsY;
    int slotsY;
    int slotsTextY;
    int inputY;
    int nameY;
    int runewordsTextY;

    public RunewordExtension(int categoryWidth) {
        this.categoryWidth = categoryWidth;
        lineHeight = Minecraft.getInstance().font.lineHeight;
        nameY = NAME_Y_POSITION;
        runewordsTextY = offsetLines(nameY, 2);
        inputY = offsetLine(runewordsTextY);
        slotsTextY = offsetLine(inputY + SLOT_SIZE);
        slotsY = offsetLine(slotsTextY);
        statsY = slotsY + STATS_Y_GAP;

        maximumResultItemsPerRow = this.categoryWidth / SLOT_SIZE;
    }

    private int offsetLine(int origin) {
        return origin + LINES_GAP + lineHeight;
    }

    public void setRecipe(IRecipeLayoutBuilder builder, RunewordRecipe recipe) {
        List<ItemStack> inputs = recipe.toMaterialsStackForJei();
        List<List<ItemStack>> resultItem = recipe.toResultSlotsForJei();

        createAndSetInput(builder, inputs, categoryWidth);
        createAndSetOutputs(builder, resultItem, categoryWidth);
    }

    private void createAndSetOutputs(IRecipeLayoutBuilder builder, List<List<ItemStack>> outputs, int categoryWidth) {
        var row = 0;
        var column = 0;
        var center = categoryWidth / 2;
        var offset = calculateCenteringOffset(outputs.size());
        for (List<ItemStack> output : outputs) {
            var x = center + offset;
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, x, slotsY + row * SLOT_SIZE);
            slot.addItemStacks(output);

            column++;
            offset += SLOT_SIZE;
            if (column >= maximumResultItemsPerRow) {
                column = 0;
                row++;
                offset = calculateCenteringOffset(outputs.size() - row * maximumResultItemsPerRow);
            }
        }
    }

    private int calculateCenteringOffset(int size) {
        size = Math.min(size, maximumResultItemsPerRow);
        var offset = -(size / 2) * SLOT_SIZE;
        if (size % 2 != 0) {
            offset = -((size + 1) / 2) * SLOT_SIZE + SLOT_SIZE / 2;
        }
        return offset;
    }

    private void createAndSetInput(IRecipeLayoutBuilder builder, List<ItemStack> inputs, int categoryWidth) {
        var center = categoryWidth / 2;
        var offset = calculateCenteringOffset(inputs.size());

        for (ItemStack input : inputs) {
            var x = center + offset;
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, x, inputY);
            slot.addItemStack(input);
            offset += SLOT_SIZE;
        }
    }

    public void drawRunewordInfo(RunewordRecipe recipe, int recipeWidth, int recipeHeight, GuiGraphicsExtractor guiGraphics) {
        var font = Minecraft.getInstance().font;

        // Recipe name - yellow and bold in center
        Component nameText = recipe.locName().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        drawCenteredText(recipeWidth, guiGraphics, font, nameText, nameY);
        int x;

        // Literal "Runeword"
        Component runewordText = Words.Runeword.locName().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD);
        drawCenteredText(recipeWidth, guiGraphics, font, runewordText, runewordsTextY);

        // Literal "Slots"
        Component slotsText = Words.ON_SLOTS.locName().withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        drawCenteredText(recipeWidth, guiGraphics, font, slotsText, slotsTextY);

        int statY = statsY + slotsLines(recipe.slots.size());

        // Literal "Stats"
        Component statsHeader = Words.Stats.locName().append(":");
        drawCenteredText(recipeWidth, guiGraphics, font, statsHeader, statY);
        statY = offsetLine(statY);

        for (StatMod stat : recipe.stats) {
            List<Component> statTooltips = stat.getEstimationTooltip(1);
            for (Component statLine : statTooltips) {
                drawCenteredText(recipeWidth, guiGraphics, font, statLine, statY);
                statY = offsetLine(statY);
            }
        }
    }

    private static void drawCenteredText(int recipeWidth, GuiGraphicsExtractor guiGraphics, Font font, Component nameText, int y) {
        var x = recipeWidth / 2 - font.width(nameText) / 2;
        com.robertx22.forge_compat.client.GuiBlitHelper.drawString(guiGraphics, font, nameText, x, y, 0xFFFFFF);
    }

    private int slotsLines(int totalItems) {
        return (int) (Math.ceil((float) totalItems / maximumResultItemsPerRow) * SLOT_SIZE);
    }

    public int calculateMaxHeight(int maxLines) {
        return offsetLines(slotsTextY + slotsLines(maximumResultItemsPerRow * 2) + STATS_Y_GAP, maxLines + 2);
    }

    private int offsetLines(int nameY, int amount) {
        for (int i = 0; i < amount; i++) {
            nameY = offsetLine(nameY);
        }
        return nameY;
    }
}
