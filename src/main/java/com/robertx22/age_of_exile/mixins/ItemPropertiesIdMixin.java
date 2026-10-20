package com.robertx22.age_of_exile.mixins;

import com.robertx22.forge_compat.registries.PropertiesIdHelper;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.Properties.class)
public class ItemPropertiesIdMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ageOfEpoch$ensureItemId(CallbackInfo ci) {
        PropertiesIdHelper.ensureItemPropertiesId((Item.Properties) (Object) this);
    }
}
