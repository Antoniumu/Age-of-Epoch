package mezz.jei.api.recipe.transfer;

import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import java.util.List;
import java.util.Optional;

public interface IRecipeTransferInfo<C extends AbstractContainerMenu, R> {
    Class<? extends C> getContainerClass();
    Optional<MenuType<C>> getMenuType();
    RecipeType<R> getRecipeType();
    boolean canHandle(C container, R recipe);
    List<Slot> getRecipeSlots(C container, R recipe);
    List<Slot> getInventorySlots(C container, R recipe);
    default boolean requireCompleteSets(C container, R recipe) { return false; }
}
