package mezz.jei.api.gui.builder;
import mezz.jei.api.recipe.RecipeIngredientRole;
public interface IRecipeLayoutBuilder {
    IRecipeSlotBuilder addSlot(RecipeIngredientRole role, int x, int y);
    void moveRecipeTransferButton(int posX, int posY);
    void setShapeless();
}
