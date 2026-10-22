package com.robertx22.library_of_exile.mixins;

import com.robertx22.library_of_exile.dimension.MapDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {
    private WorldBorder mnsCachedMapBorder;

    @Inject(method = "getWorldBorder", at = @At("RETURN"), cancellable = true)
    private void hook2(CallbackInfoReturnable<WorldBorder> cir) {
        try {
            Level world = (Level) (Object) this;
            if (world != null && !world.isClientSide() && MapDimensions.isMap(world)) {
                var c = MapDimensions.getInfo(world);
                if (c == null || c.config.DISABLE_WORLDBORDER_OVERRIDE.get()) {
                    return;
                }
                if (mnsCachedMapBorder == null) {
                    mnsCachedMapBorder = new WorldBorder();
                    mnsCachedMapBorder.setCenter(0.0, 0.0);
                    mnsCachedMapBorder.setSize(59999968);
                }
                cir.setReturnValue(mnsCachedMapBorder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
