package mezz.jei.api;

import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

public interface IModPlugin {
    Identifier getPluginUid();
    default void registerItemSubtypes(ISubtypeRegistration registration) {}
    default void registerIngredients(IModIngredientRegistration registration) {}
    default void registerCategories(IRecipeCategoryRegistration registration) {}
    default void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {}
    default void registerRecipes(IRecipeRegistration registration) {}
    default void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {}
    default void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {}
    default void registerGuiHandlers(IGuiHandlerRegistration registration) {}
    default void onRuntimeAvailable(IJeiRuntime jeiRuntime) {}
}
