package com.robertx22.mine_and_slash.compat.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;

public class ItemDamage {
    public static <T extends LivingEntity> void hurtAndBreak(ItemStack stack, int pAmount, T pEntity, Consumer<T> pOnBroken) {
        if (!pEntity.level().isClientSide() && stack.isDamageableItem() && pAmount > 0) {
            int next = stack.getDamageValue() + pAmount;
            stack.setDamageValue(next);
            if (next >= stack.getMaxDamage()) {
                pOnBroken.accept(pEntity);
                stack.shrink(1);
                stack.setDamageValue(0);
            }
        }
    }
}
