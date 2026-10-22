package mezz.jei.api.gui.ingredient;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.world.item.ItemStack;
import java.util.List;
public interface ICraftingGridHelper {
    default List<IRecipeSlotBuilder> createAndSetInputs(IRecipeLayoutBuilder builder, List<List<ItemStack>> inputs, int width, int height) { return List.of(); }
    default <T> List<IRecipeSlotBuilder> createAndSetInputs(IRecipeLayoutBuilder builder, Class<T> type, List<List<T>> inputs, int width, int height) { return List.of(); }
    default IRecipeSlotBuilder createAndSetOutputs(IRecipeLayoutBuilder builder, List<ItemStack> outputs) { return null; }
    default <T> IRecipeSlotBuilder createAndSetOutputs(IRecipeLayoutBuilder builder, Class<T> type, List<T> outputs) { return null; }
}
