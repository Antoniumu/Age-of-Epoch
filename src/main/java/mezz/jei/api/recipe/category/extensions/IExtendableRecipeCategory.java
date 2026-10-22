package mezz.jei.api.recipe.category.extensions;

import mezz.jei.api.recipe.category.IRecipeCategory;
import java.util.function.Function;
import java.util.function.Predicate;

public interface IExtendableRecipeCategory<T, W extends IRecipeCategoryExtension> extends IRecipeCategory<T> {
    default <R extends T> void addCategoryExtension(Class<? extends R> recipeClass, Function<R, ? extends W> extensionFactory) {}
    default <R extends T> void addCategoryExtension(Class<? extends R> recipeClass, Predicate<R> extensionFilter, Function<R, ? extends W> extensionFactory) {}
}
