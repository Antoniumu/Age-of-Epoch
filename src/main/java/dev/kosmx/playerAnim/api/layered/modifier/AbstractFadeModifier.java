package dev.kosmx.playerAnim.api.layered.modifier;
import dev.kosmx.playerAnim.core.util.Ease;
public class AbstractFadeModifier {
    public static AbstractFadeModifier standardFadeIn(int length, Ease ease) { return new AbstractFadeModifier(); }
}
