package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.mixin_methods.OnItemInteract;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class ItemInteractionHook {

    @Inject(method = "doClick", cancellable = true, at = @At(value = "HEAD"))
    public void method_30010(int i, int j, ContainerInput slotActionType, Player player, CallbackInfo ci) {
        AbstractContainerMenu screen = (AbstractContainerMenu) (Object) this;
        try {
            CallbackInfoReturnable<ItemStack> cir = new CallbackInfoReturnable<>("doClick", true, ItemStack.EMPTY);
            OnItemInteract.on(screen, i, j, slotActionType, player, cir);
            if (cir.isCancelled()) {
                ci.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
