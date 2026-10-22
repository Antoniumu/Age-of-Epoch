package mezz.jei.api.recipe.category;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.forge_compat.client.GuiGraphics;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.List;

public interface IRecipeCategory<T> {
    RecipeType<T> getRecipeType();
    Component getTitle();
    IDrawable getBackground();
    IDrawable getIcon();
    default int getWidth() { return getBackground() != null ? getBackground().getWidth() : 120; }
    default int getHeight() { return getBackground() != null ? getBackground().getHeight() : 60; }
    void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);
    default void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {}
    default void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {}
    default List<Component> getTooltipStrings(T recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) { return Collections.emptyList(); }
    default boolean handleInput(T recipe, double mouseX, double mouseY, InputConstants.Key input) { return false; }
    default boolean isHandled(T recipe) { return true; }
    default Identifier getRegistryName(T recipe) { return null; }
}
