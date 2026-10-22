package com.robertx22.mine_and_slash.a_libraries.jei;

import com.robertx22.mine_and_slash.database.data.runewords.RunewordRecipe;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.RuneItems;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RunewordCategory implements IRecipeCategory<RunewordRecipe> {
    final IDrawable background;
    IGuiHelper guiHelper;
    RecipeType<RunewordRecipe> recipeType;
    final IDrawable icon;

    RunewordExtension recipeExtension;

    RunewordCategory(IGuiHelper guiHelper, RecipeType<RunewordRecipe> type) {
        this.guiHelper = guiHelper;
        this.recipeType = type;
        icon = guiHelper.createDrawableItemStack(RuneItems.MAP.entrySet().stream().findFirst().get().getValue().get().getDefaultInstance());

        Dimensions dimension = calculateDimension();
        int maxWidth = dimension.width;
        int maxLines = dimension.lines;

        recipeExtension = new RunewordExtension(maxWidth);
        var height = recipeExtension.calculateMaxHeight(maxLines);
        background = guiHelper.createBlankDrawable(maxWidth, height);
    }

    private Dimensions calculateDimension() {
        AtomicInteger maxWidth = new AtomicInteger();
        AtomicInteger maxLines = new AtomicInteger();
        var font = Minecraft.getInstance().font;
        ExileDB.RuneWords().getList().forEach(word -> {
            word.stats.forEach(stat -> {
                List<Component> statTooltips = stat.getEstimationTooltip(1);
                for (Component statLine : statTooltips) {
                    var lineWidth = font.width(statLine);
                    maxWidth.set(Math.max(maxWidth.get(), lineWidth));
                }
            });
            maxLines.set(Math.max(maxLines.get(), word.stats.size()));
        });
        return new Dimensions(maxWidth.get(), maxLines.get());
    }

    @Override
    public RecipeType<RunewordRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return Words.Runeword.locName();
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RunewordRecipe recipe, IFocusGroup focuses) {
        recipeExtension.setRecipe(builder, recipe);
    }

    @Override
    public void draw(RunewordRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        int recipeWidth = this.getWidth();
        int recipeHeight = this.getHeight();
        recipeExtension.drawRunewordInfo(recipe, recipeWidth, recipeHeight, guiGraphics);
    }

    @Override
    public @Nullable Identifier getRegistryName(RunewordRecipe recipe) {
        return Identifier.parse(String.valueOf(recipe.id));
    }

    private class Dimensions {
        int width;
        int lines;

        Dimensions(int width, int height) {
            this.width = width;
            this.lines = height;
        }
    }
}
