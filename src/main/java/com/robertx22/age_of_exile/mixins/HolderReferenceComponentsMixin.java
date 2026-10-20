package com.robertx22.age_of_exile.mixins;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Holder.Reference.class)
public class HolderReferenceComponentsMixin {

    @Shadow
    @Nullable
    private DataComponentMap components;

    @Inject(method = "components", at = @At("HEAD"), cancellable = true)
    private void ageOfEpoch$fallbackComponents(CallbackInfoReturnable<DataComponentMap> cir) {
        if (this.components == null) {
            cir.setReturnValue(DataComponentMap.EMPTY);
        }
    }
}
