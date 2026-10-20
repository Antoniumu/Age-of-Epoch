package com.robertx22.age_of_exile.mixins;

import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.entity.living.LivingEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingTickBridgeMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void ageOfEpoch$livingTick(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent((LivingEntity) (Object) this));
    }
}
