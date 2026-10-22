package com.robertx22.age_of_exile.mixins;

import com.robertx22.forge_compat.registries.PropertiesIdHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentInitializers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * MC 26.3: Item/Block constructors append entries into
 * BuiltInRegistries.DATA_COMPONENT_INITIALIZERS using the auto-minted
 * mmorpg:auto_item_N id whenever an item is constructed outside a
 * DeferredRegister supplier. DataComponentInitializers.build() (run on every
 * server resource reload, i.e. world creation) throws
 * "Missing element" for any key not present in its registry - so prune the
 * orphans right before it runs.
 */
@Mixin(DataComponentInitializers.class)
public class DataComponentInitializersMixin {

    @Inject(method = "build", at = @At("HEAD"))
    private void age_of_epoch$pruneOrphanInitializers(HolderLookup.Provider context, CallbackInfoReturnable<List<?>> cir) {
        PropertiesIdHelper.cleanupDataComponentInitializers();
    }
}
