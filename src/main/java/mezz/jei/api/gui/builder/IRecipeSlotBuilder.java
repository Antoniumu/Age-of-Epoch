package mezz.jei.api.gui.builder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;
public interface IRecipeSlotBuilder {
    IRecipeSlotBuilder addItemStack(ItemStack itemStack);
    IRecipeSlotBuilder addItemStacks(List<ItemStack> itemStacks);
    IRecipeSlotBuilder addIngredients(Ingredient ingredient);
    <I> IRecipeSlotBuilder addIngredients(Class<I> type, List<I> ingredients);
    IRecipeSlotBuilder setBackground(mezz.jei.api.gui.drawable.IDrawable background, int xOffset, int yOffset);
    IRecipeSlotBuilder setSlotName(String slotName);
}
