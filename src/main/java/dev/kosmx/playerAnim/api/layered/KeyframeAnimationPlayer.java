package dev.kosmx.playerAnim.api.layered;
import dev.kosmx.playerAnim.api.firstPerson.*;
public class KeyframeAnimationPlayer implements IAnimation {
    public KeyframeAnimationPlayer(Object anim) {}
    public KeyframeAnimationPlayer setFirstPersonMode(FirstPersonMode m) { return this; }
    public KeyframeAnimationPlayer setFirstPersonConfiguration(FirstPersonConfiguration c) { return this; }
}
