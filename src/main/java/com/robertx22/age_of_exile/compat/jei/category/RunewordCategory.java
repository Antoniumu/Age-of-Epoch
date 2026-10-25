package com.robertx22.age_of_exile.compat.jei.category;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Runeword Station: base item + the exact set of runes -> unique item.
 */
public class RunewordCategory extends AbstractRecipeCategory<RunewordRecipe> {

    public static final IRecipeType<RunewordRecipe> TYPE =
        IRecipeType.create(SlashRef.MODID, "runewords", RunewordRecipe.class);

    private static final int[] RUNE_X = {1, 19, 37, 1, 19, 37};
    private static final int[] RUNE_Y = {1, 1, 1, 19, 19, 19};

    public RunewordCategory(IGuiHelper guiHelper) {
        super(
            TYPE,
            Component.translatable("mmorpg.jei.category.runewords"),
            guiHelper.createDrawableItemStack(new ItemStack(SlashItems.RUNEWORD_STATION.get())),
            170,
            140
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RunewordRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < RUNE_X.length; i++) {
            List<ItemStack> rune = i < recipe.runes().size() ? List.of(recipe.runes().get(i)) : List.of();
            JeiLayout.addSlot(builder, RecipeIngredientRole.INPUT, RUNE_X[i], RUNE_Y[i], rune);
        }

        JeiLayout.addSlot(builder, RecipeIngredientRole.INPUT, 1, 40, recipe.gear());
        JeiLayout.addSlot(builder, RecipeIngredientRole.OUTPUT, 92, 40, recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RunewordRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(66, 42);
        JeiLayout.addLines(builder, recipe.lines(), 1, 62, 168, 74);
    }

    @Override
    public Identifier getIdentifier(RunewordRecipe recipe) {
        return recipe.id();
    }
}
