package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.simulation.SimulationManager;
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
        Entity self = (Entity) (Object) this;
        CompoundTag tag = new CompoundTag();
        CapabilityProvider.saveAll(self, tag);
        if (!tag.isEmpty()) {
            output.store("AgeOfEpochCaps", CompoundTag.CODEC, tag);
        }
        CompoundTag simSnap = SimulationManager.getSnapshotForSave(self);
        if (simSnap != null && !simSnap.isEmpty()) {
            output.store("AgeOfEpochSimSnapshot", CompoundTag.CODEC, simSnap);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void ageOfEpoch$loadCaps(ValueInput input, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        input.read("AgeOfEpochCaps", CompoundTag.CODEC)
            .ifPresent(tag -> CapabilityProvider.loadAll(self, tag));
        input.read("AgeOfEpochSimSnapshot", CompoundTag.CODEC)
            .ifPresent(simSnap -> SimulationManager.loadSnapshotFromSave(self, simSnap));
    }
}
