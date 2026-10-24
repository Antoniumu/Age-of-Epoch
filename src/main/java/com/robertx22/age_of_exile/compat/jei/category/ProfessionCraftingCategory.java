package com.robertx22.age_of_exile.compat.jei.category;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftToolItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The profession crafting grid (3x2, opened by right-clicking a profession tool).
 *
 * Layout mirrors the real container: 3 columns x 2 rows, one slot holds the gear being
 * enchanted, the rest hold ingredients.
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
            96
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ProfessionCraftRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < SLOT_X.length; i++) {
            ItemStack content = ItemStack.EMPTY;
            if (i == 0) {
                content = recipe.gear();
            } else if (i == 2) {
                content = recipe.ingredient();
            }

            if (content.isEmpty()) {
                continue;
            }

            builder.addInputSlot(SLOT_X[i], SLOT_Y[i])
                .setStandardSlotBackground()
                .addItemStack(content);
        }

        if (!recipe.result().isEmpty()) {
            builder.addOutputSlot(92, 1)
                .setStandardSlotBackground()
                .addItemStack(recipe.result());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ProfessionCraftRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(66, 3);
        JeiLayout.addLines(builder, recipe.lines(), 1, 42, 168);
    }

    @Override
    public Identifier getIdentifier(ProfessionCraftRecipe recipe) {
        return recipe.id();
    }
}
