package top.theillusivec4.curios.api;

import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;

/**
 * Compile-time stub of the Curios API (Curios is a Forge mod). On Fabric the
 * optional accessory integration is disabled; calls return empty.
 */
public class CuriosApi {

    private static final ICurioHelper HELPER = new ICurioHelper();

    public interface ICurioHelper {
        default Optional<ICuriosItemHandler> getCuriosHandler(Player player) {
            return Optional.empty();
        }
    }

    public static ICurioHelper getCuriosHelper() {
        return HELPER;
    }
}
