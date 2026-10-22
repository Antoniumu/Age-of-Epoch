package dev.kosmx.playerAnim.api.layered.modifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import java.util.Optional;
import java.util.function.Function;
public class AdjustmentModifier {
    public record PartModifier(Vec3f rotation, Vec3f offset, Vec3f scale) {
        public PartModifier(Vec3f rotation) { this(rotation, new Vec3f(0,0,0), new Vec3f(1,1,1)); }
    }
    public AdjustmentModifier(Function<String, Optional<PartModifier>> fn) {}
}
