package com.robertx22.age_of_exile.mixins;

import com.robertx22.forge_compat.common.capabilities.CapabilityProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityNbtCapBridgeMixin {

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void ageOfEpoch$saveCaps(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        CapabilityProvider.saveAll((Entity) (Object) this, tag);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void ageOfEpoch$loadCaps(CompoundTag tag, CallbackInfo ci) {
        CapabilityProvider.loadAll((Entity) (Object) this, tag);
    }
}
