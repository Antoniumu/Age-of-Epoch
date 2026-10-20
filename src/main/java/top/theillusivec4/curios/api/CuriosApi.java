package top.theillusivec4.curios.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Compile-time stub of the Curios API (Curios is a Forge mod). On Fabric the
 * optional accessory integration is disabled; calls return empty.
 */
public class CuriosApi {

    private static final ICurioHelper HELPER = new ICurioHelper() {};

    public interface ICurioHelper {
        default Optional<ICuriosItemHandler> getCuriosHandler(Player player) {
            return Optional.empty();
        }

        default Set<String> getCurioTags(Item item) {
            return Collections.emptySet();
        }
    }

    public static ICurioHelper getCuriosHelper() {
        return HELPER;
    }
}
