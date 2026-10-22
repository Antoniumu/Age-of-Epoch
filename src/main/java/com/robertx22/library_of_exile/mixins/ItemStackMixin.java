package com.robertx22.library_of_exile.mixins;

import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.entity.player.ItemTooltipEvent;
import com.robertx22.orbs_of_crafting.main.OrbTooltips;
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

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void getTooltip(Item.TooltipContext context, Player entity, TooltipFlag tooltipContext, CallbackInfoReturnable<List<Component>> list) {
        try {
            ItemStack stack = (ItemStack) (Object) this;
            OrbTooltips.callMethod(stack, entity, tooltipContext, list);
            MinecraftForge.EVENT_BUS.post(new ItemTooltipEvent(stack, entity, list.getReturnValue(), tooltipContext));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
