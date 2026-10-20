package com.robertx22.age_of_exile.mixin_methods;

import com.robertx22.age_of_exile.vanilla_mc.potion_effects.IOneOfATypePotion;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class CanEntityHavePotionMixin {

    public static void hook(LivingEntity en, MobEffectInstance effect, CallbackInfoReturnable<Boolean> ci) {
        if (!canAddPotion(en, effect.getEffect().value())) {
            ci.setReturnValue(false);
        }
    }

    public static boolean canAddPotion(LivingEntity en, MobEffect effect) {

        try {

            if (effect instanceof IOneOfATypePotion one) {

                if (one.isOneOfAKind()) {
                    if (en.getActiveEffects()
                        .stream()
                        .anyMatch(x -> {
                            MobEffect eff = x.getEffect().value();
                            if (eff instanceof IOneOfATypePotion ot) {
                                if (ot.getOneOfATypeType()
                                    .equals(one.getOneOfATypeType())) {
                                    return true;
                                }
                                if (eff.equals(effect)) {
                                    return true;
                                }
                            }
                            return false;
                        })) {
                        return false;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}
