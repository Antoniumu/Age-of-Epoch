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
 * Salvaging: drag the Salvage Hammer onto a piece of gear to get essence back.
 */
public class SalvageCategory extends AbstractRecipeCategory<SalvageRecipe> {

    public static final IRecipeType<SalvageRecipe> TYPE =
        IRecipeType.create(SlashRef.MODID, "salvage", SalvageRecipe.class);

    private static final int[] OUT_X = {52, 70, 88};

    public SalvageCategory(IGuiHelper guiHelper) {
        super(
            TYPE,
            Component.translatable("mmorpg.jei.category.salvage"),
            guiHelper.createDrawableItemStack(new ItemStack(SlashItems.SALVAGE_HAMMER.get())),
            170,
            76
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SalvageRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 1)
            .setStandardSlotBackground()
            .addItemStack(recipe.gear());

        for (int i = 0; i < recipe.outputs().size() && i < OUT_X.length; i++) {
            ItemStack out = recipe.outputs().get(i);
            if (out.isEmpty()) {
                continue;
            }
            IRecipeSlotBuilder slot = builder.addOutputSlot(OUT_X[i], 1)
                .setStandardSlotBackground();
            slot.addItemStack(out);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, SalvageRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(26, 3);
        JeiLayout.addLines(builder, recipe.lines(), 1, 26, 168);
    }

    @Override
    public Identifier getIdentifier(SalvageRecipe recipe) {
        return recipe.id();
    }
}
