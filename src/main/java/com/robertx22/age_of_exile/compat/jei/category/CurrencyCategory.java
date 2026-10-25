package com.robertx22.age_of_exile.compat.jei.category;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CurrencyItems;
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
 * Currency orbs applied by dragging them onto generated gear in the inventory.
 * Gems have their own category because socketing, extraction and rank-up are separate
 * mechanics with exact inputs and outputs.
 */
public class CurrencyCategory extends AbstractRecipeCategory<CurrencyRecipe> {

    public static final IRecipeType<CurrencyRecipe> TYPE =
        IRecipeType.create(SlashRef.MODID, "currency_on_gear", CurrencyRecipe.class);

    public CurrencyCategory(IGuiHelper guiHelper) {
        super(
            TYPE,
            Component.translatable("mmorpg.jei.category.currency"),
            guiHelper.createDrawableItemStack(new ItemStack(CurrencyItems.ORB_OF_TRANSMUTATION.get())),
            170,
            132
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CurrencyRecipe recipe, IFocusGroup focuses) {
        JeiLayout.addSlot(builder, RecipeIngredientRole.INPUT, 1, 1, recipe.gear());
        if (recipe.currency() != null && !recipe.currency().isEmpty()) {
            JeiLayout.addSlot(builder, RecipeIngredientRole.INPUT, 52, 1, List.of(recipe.currency()));
        }
        JeiLayout.addSlot(builder, RecipeIngredientRole.OUTPUT, 104, 1, recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CurrencyRecipe recipe, IFocusGroup focuses) {
        builder.addRecipePlusSignWidget().setPosition(26, 3);
        builder.addRecipeArrowWidget().setPosition(76, 3);
        JeiLayout.addLines(builder, recipe.lines(), 1, 26, 168, 102);
    }

    @Override
    public Identifier getIdentifier(CurrencyRecipe recipe) {
        return recipe.id();
    }
}
