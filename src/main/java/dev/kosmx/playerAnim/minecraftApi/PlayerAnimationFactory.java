package dev.kosmx.playerAnim.minecraftApi;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import java.util.function.Function;
public class PlayerAnimationFactory {
    public static final FactoryHolder ANIMATION_DATA_FACTORY = new FactoryHolder();
    public static class FactoryHolder {
        public void registerFactory(Identifier id, int priority, Function<AbstractClientPlayer, IAnimation> factory) {}
    }
}
