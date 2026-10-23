package com.robertx22.library_of_exile.mixins;

import com.robertx22.forge_compat.resources.DataPackRegistries;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataPackRegistries.class)
public abstract class ServerResourceManagerMixin {

    public void onConstructRegisterAllReloaders(CallbackInfo ci) {
        DataPackRegistries m = (DataPackRegistries) (Object) this;
        ReloadableResourceManager manager = m.getResourceManager();
        if (manager != null) {
            ExileRegistryType.registerJsonListeners(manager);
        }
    }

}
