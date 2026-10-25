package com.robertx22.age_of_exile.compat.jei.category;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftToolItems;
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
 * The profession crafting grid (3x2, opened by right-clicking a profession tool).
 * All six real grid backgrounds are drawn; slot 0 is the optional gear slot and slot 2
 * shows the ingredient variants this page is about.
 */
public class ProfessionCraftingCategory extends AbstractRecipeCategory<ProfessionCraftRecipe> {

    public static final IRecipeType<ProfessionCraftRecipe> TYPE =
        IRecipeType.create(SlashRef.MODID, "profession_crafting", ProfessionCraftRecipe.class);

    private static final int[] SLOT_X = {1, 19, 37, 1, 19, 37};
    private static final int[] SLOT_Y = {1, 1, 1, 19, 19, 19};

    public ProfessionCraftingCategory(IGuiHelper guiHelper) {
        super(
            TYPE,
            Component.translatable("mmorpg.jei.category.prof_craft"),
            guiHelper.createDrawableItemStack(new ItemStack(CraftToolItems.ARMOR.get())),
            170,
            128
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProfessionCraftRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < SLOT_X.length; i++) {
            List<ItemStack> content = List.of();
            if (i == 0) {
                content = recipe.gear();
            } else if (i == 2) {
                content = recipe.ingredient();
            }
            JeiLayout.addSlot(builder, RecipeIngredientRole.INPUT, SLOT_X[i], SLOT_Y[i], content);
        }

        JeiLayout.addSlot(builder, RecipeIngredientRole.OUTPUT, 92, 1, recipe.result());
        if (recipe.tool() != null && !recipe.tool().isEmpty()) {
            JeiLayout.addSlot(builder, RecipeIngredientRole.CRAFTING_STATION, 92, 24, List.of(recipe.tool()));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ProfessionCraftRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(66, 3);
        JeiLayout.addLines(builder, recipe.lines(), 1, 50, 168, 74);
    }

    @Override
    public Identifier getIdentifier(ProfessionCraftRecipe recipe) {
        return recipe.id();
    }
}
