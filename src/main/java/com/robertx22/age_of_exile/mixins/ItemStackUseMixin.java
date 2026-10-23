package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.mixin_methods.OnItemStoppedUsingCastImbuedSpell;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackUseMixin {

    @Inject(method = "use", at = @At("HEAD"))
    public void onUseItemstackmethod(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        ItemStack stack = (ItemStack) (Object) this;
        OnItemStoppedUsingCastImbuedSpell.crossbow(stack, world, user, hand, ci);
    }

}
