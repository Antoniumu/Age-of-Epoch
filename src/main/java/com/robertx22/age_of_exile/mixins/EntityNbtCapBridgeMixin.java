package com.robertx22.age_of_exile.mixins;

import com.robertx22.forge_compat.common.capabilities.CapabilityProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityNbtCapBridgeMixin {

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void ageOfEpoch$saveCaps(ValueOutput output, CallbackInfo ci) {
        CompoundTag tag = new CompoundTag();
        CapabilityProvider.saveAll((Entity) (Object) this, tag);
        if (!tag.isEmpty()) {
            output.store("AgeOfEpochCaps", CompoundTag.CODEC, tag);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void ageOfEpoch$loadCaps(ValueInput input, CallbackInfo ci) {
        input.read("AgeOfEpochCaps", CompoundTag.CODEC)
            .ifPresent(tag -> CapabilityProvider.loadAll((Entity) (Object) this, tag));
    }
}
