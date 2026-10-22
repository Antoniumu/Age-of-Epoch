package mezz.jei.api.registration;

import mezz.jei.api.recipe.RecipeType;
public class IGuiHandlerRegistration {
    public void addRecipeClickArea(Class<?> cls, int x, int y, int w, int h, RecipeType<?>... types) {}
}
