package mezz.jei.api.recipe.category.extensions.vanilla.crafting;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryExtension;
import net.minecraft.resources.Identifier;

public interface ICraftingCategoryExtension extends IRecipeCategoryExtension {
    void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses);
    default Identifier getRegistryName() { return null; }
    default int getWidth() { return 0; }
    default int getHeight() { return 0; }
}
