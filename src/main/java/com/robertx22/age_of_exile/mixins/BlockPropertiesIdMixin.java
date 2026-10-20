package com.robertx22.age_of_exile.mixins;

import com.robertx22.forge_compat.registries.PropertiesIdHelper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.Properties.class)
public class BlockPropertiesIdMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ageOfEpoch$ensureBlockId(CallbackInfo ci) {
        PropertiesIdHelper.ensureBlockPropertiesId((BlockBehaviour.Properties) (Object) this);
    }
}
