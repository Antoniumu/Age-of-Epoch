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

/**
 * Salvaging gear, stat souls and rarity essence with the Salvage Hammer.
 */
public class SalvageCategory extends AbstractRecipeCategory<SalvageRecipe> {

    public static final IRecipeType<SalvageRecipe> TYPE =
        IRecipeType.create(SlashRef.MODID, "salvage", SalvageRecipe.class);

    private static final int[] OUT_X = {52, 70, 88, 106};

    public SalvageCategory(IGuiHelper guiHelper) {
        super(
            TYPE,
            Component.translatable("mmorpg.jei.category.salvage"),
            guiHelper.createDrawableItemStack(new ItemStack(SlashItems.SALVAGE_HAMMER.get())),
            170,
            156
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SalvageRecipe recipe, IFocusGroup focuses) {
        JeiLayout.addSlot(builder, RecipeIngredientRole.INPUT, 1, 1, recipe.input());

        for (int i = 0; i < recipe.outputs().size() && i < OUT_X.length; i++) {
            JeiLayout.addSlot(builder, RecipeIngredientRole.OUTPUT, OUT_X[i], 1, recipe.outputs().get(i));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, SalvageRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(26, 3);
        JeiLayout.addLines(builder, recipe.lines(), 1, 26, 168, 126);
    }

    @Override
    public Identifier getIdentifier(SalvageRecipe recipe) {
        return recipe.id();
    }
}
