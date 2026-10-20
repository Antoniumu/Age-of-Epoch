package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.mixin_methods.OnItemStoppedUsingCastImbuedSpell;
import com.robertx22.age_of_exile.mixin_methods.TooltipMethod;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CancellationException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

@Mixin(value = ItemStack.class, priority = Integer.MAX_VALUE)
public abstract class ItemStackMixin {
    public ItemStackMixin() {
    }

    @Inject(method = {"hurt(ILjava/util/Random;Lnet/minecraft/entity/player/ServerPlayer;)Z"}, at = {@At("HEAD")}, cancellable = true)
    private void getMaxDamage(int p_96631_1_, Random p_96631_2_, @Nullable ServerPlayer p_96631_3_, CallbackInfoReturnable<Boolean> ci) {
        ItemStack stack = (ItemStack) (Object) this;

        try {
            // no idea if this will cause lag, if it does it's easy to remove or make more efficient
            GearItemData gear = Gear.Load(stack);

            if (gear != null) {
                boolean dontHurt = RandomUtils.roll(gear.getRarity().unbreaking_chance);
                if (dontHurt) {
                    ci.setReturnValue(false);
                }
            }
        } catch (CancellationException e) {
            e.printStackTrace();
        }
    }

    // copied from TooltipCallback fabric event
    @Inject(method = {"getTooltipLines"}, at = {@At("RETURN")})
    private void getTooltip(Player entity, TooltipFlag tooltipContext, CallbackInfoReturnable<List<Component>> list) {
        ItemStack stack = (ItemStack) (Object) this;
        TooltipMethod.getTooltip(stack, entity, tooltipContext, list);
    }

    /*
    @Inject(method = {"releaseUsing"}, cancellable = true, at = {@At("HEAD")})
    public void onStoppedUsing(Level world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        OnItemStoppedUsingCastImbuedSpell.onStoppedUsing(stack, world, user, remainingUseTicks, ci);
    }

     */

    @Inject(method = {"use"}, cancellable = true, at = {@At("HEAD")})
    public void onUseItemstackmethod(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult<ItemStack>> ci) {
        ItemStack stack = (ItemStack) (Object) this;

        OnItemStoppedUsingCastImbuedSpell.crossbow(stack, world, user, hand, ci);

    }
}
