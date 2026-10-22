package com.robertx22.mine_and_slash.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "setRemoved", at = @At(value = "HEAD"), cancellable = true)
    public void hookLoot(Entity.RemovalReason reason, CallbackInfo ci) {

        try {
            Entity en = (Entity) (Object) this;

            if (en instanceof LivingEntity e) {
                if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
                    //    MobUnloading.onUnloadMob(e);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   
}
