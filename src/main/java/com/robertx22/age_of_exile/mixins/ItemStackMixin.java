package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.mixin_methods.TooltipMethod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ItemStack.class, priority = Integer.MAX_VALUE)
public abstract class ItemStackMixin {

    public ItemStackMixin() {
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void getTooltip(Item.TooltipContext context, Player entity, TooltipFlag tooltipContext, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        TooltipMethod.getTooltip(stack, entity, tooltipContext, cir);
    }

}
