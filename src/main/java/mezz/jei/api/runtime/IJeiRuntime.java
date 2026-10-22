package mezz.jei.api.runtime;

import java.util.Collection;
public interface IJeiRuntime {
    default IIngredientManager getIngredientManager() { return new IIngredientManager() {}; }
    interface IIngredientManager {
        default <V> void removeIngredientsAtRuntime(Object type, Collection<V> ingredients) {}
    }
}
