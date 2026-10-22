package mezz.jei.api.recipe;
import net.minecraft.resources.Identifier;
public record RecipeType<T>(Identifier getUid, Class<? extends T> getRecipeClass) {
    public static <T> RecipeType<T> create(String namespace, String path, Class<? extends T> clazz) {
        return new RecipeType<>(Identifier.fromNamespaceAndPath(namespace, path), clazz);
    }
}
