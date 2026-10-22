package dev.kosmx.playerAnim.minecraftApi;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import java.util.*;
public class PlayerAnimationAccess {
    public static AssociatedData getPlayerAssociatedData(AbstractClientPlayer player) { return new AssociatedData(); }
    public static class AssociatedData {
        public IAnimation get(Identifier id) { return null; }
        public void set(Identifier id, IAnimation anim) {}
    }
}
