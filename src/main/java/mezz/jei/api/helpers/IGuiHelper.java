package mezz.jei.api.helpers;
import mezz.jei.api.gui.drawable.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
public interface IGuiHelper {
    IDrawableStatic createDrawable(Identifier resource, int u, int v, int width, int height);
    IDrawable createDrawableItemStack(ItemStack ingredient);
    IDrawableStatic createBlankDrawable(int width, int height);
    IDrawableStatic getSlotDrawable();

    default mezz.jei.api.gui.ingredient.ICraftingGridHelper createCraftingGridHelper() { return new mezz.jei.api.gui.ingredient.ICraftingGridHelper() {}; }
}