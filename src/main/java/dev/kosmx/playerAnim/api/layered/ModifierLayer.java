package dev.kosmx.playerAnim.api.layered;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
public class ModifierLayer<T extends IAnimation> implements IAnimation {
    private T anim;
    public T getAnimation() { return anim; }
    public void setAnimation(T anim) { this.anim = anim; }
    public void replaceAnimationWithFade(AbstractFadeModifier fade, T anim) { this.anim = anim; }
    public void addModifierBefore(Object mod) {}
    public void addModifierLast(Object mod) {}
}
