package com.robertx22.age_of_exile.mixin_methods;

import com.robertx22.age_of_exile.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.forge_compat.event.entity.living.LivingEntityUseItemEvent;
import com.robertx22.library_of_exile.main.ForgeEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class OnItemStoppedUsingCastImbuedSpell {

    public static void register() {

        ForgeEvents.registerForgeEvent(LivingEntityUseItemEvent.Stop.class, event -> {
            if (!event.getEntityLiving().level().isClientSide()) {
                onStoppedUsing(event.getItem(), event.getEntityLiving(), event.getDuration());
            }
        });

    }

    public static boolean isBowItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof BowItem) {
            return true;
        }
        GearSlot slot = GearSlot.getSlotOf(stack.getItem());
        return slot != null && GearSlots.BOW.equals(slot.GUID());
    }

    public static boolean isCrossbowItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return true;
        }
        GearSlot slot = GearSlot.getSlotOf(stack.getItem());
        return slot != null && GearSlots.CROSBOW.equals(slot.GUID());
    }

    public static boolean canCastImbuedSpell(LivingEntity user) {
        return canCastImbuedSpell(user.getUseItem(), user, user.getUseItemRemainingTicks());
    }

    public static boolean canCastImbuedSpell(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        if (!isBowItem(stack)) {
            return false;
        }

        int useTicks = stack.getItem()
            .getUseDuration(stack, user) - remainingUseTicks;
        float power = BowItem.getPowerForTime(useTicks);
        return power >= 0.1F;
    }

    public static void onStoppedUsing(ItemStack stack, LivingEntity user, int remainingUseTicks) {

        if (canCastImbuedSpell(stack, user, remainingUseTicks)) {
            Load.spells(user)
                .getCastingData()
                .tryCastImbuedSpell(user);
        }

    }

    public static void crossbow(ItemStack stack, Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {

        try {
            if (user.level().isClientSide()) {
                return;
            }
            if (isCrossbowItem(stack)) {
                if (CrossbowItem.isCharged(stack)) {
                    Load.spells(user)
                        .getCastingData()
                        .tryCastImbuedSpell(user);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
