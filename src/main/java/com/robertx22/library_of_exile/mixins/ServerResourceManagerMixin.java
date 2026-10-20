package com.robertx22.library_of_exile.mixins;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import net.minecraft.commands.Commands;
import com.robertx22.forge_compat.resources.DataPackRegistries;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataPackRegistries.class)
public abstract class ServerResourceManagerMixin {

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onConstructRegisterAllReloaders(Commands.EnvironmentType registrationEnvironment, int i, CallbackInfo ci) {
        DataPackRegistries m = (DataPackRegistries) (Object) this;
        SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager) m.getResourceManager();
        ExileRegistryType.registerJsonListeners(manager);
    }

}
