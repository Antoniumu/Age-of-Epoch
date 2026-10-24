package com.robertx22.age_of_exile.compat.jei.category;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

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
            100
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RunewordRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < RUNE_X.length; i++) {
            IRecipeSlotBuilder slot = builder.addInputSlot(RUNE_X[i], RUNE_Y[i])
                .setStandardSlotBackground();
            if (i < recipe.runes().size()) {
                slot.addItemStack(recipe.runes().get(i));
            }
        }

        if (!recipe.gear().isEmpty()) {
            builder.addInputSlot(1, 40)
                .setStandardSlotBackground()
                .addItemStack(recipe.gear());
        }

        if (!recipe.result().isEmpty()) {
            builder.addOutputSlot(92, 40)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RunewordRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(66, 42);
        JeiLayout.addLines(builder, recipe.lines(), 1, 62, 168);
    }

    @Override
    public Identifier getIdentifier(RunewordRecipe recipe) {
        return recipe.id();
    }
}
