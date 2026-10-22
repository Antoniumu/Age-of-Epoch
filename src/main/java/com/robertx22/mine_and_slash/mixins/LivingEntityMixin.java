package com.robertx22.mine_and_slash.mixins;

import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.entity.living.LivingEntityUseItemEvent;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.LivingHurtUtils;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.util.DmgSourceUtils;
import com.robertx22.mine_and_slash.mixin_ducks.LivingEntityAccesor;
import com.robertx22.mine_and_slash.mixin_methods.CanEntityHavePotionMixin;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements LivingEntityAccesor {

    @Accessor("lastHurt")
    public abstract void setMyLastHurt(float lastHurt);

    @Accessor("lastHurt")
    public abstract float getMyLastHurt();

    @Override
    public void myknockback(LivingEntity target) {
        LivingEntity self = (LivingEntity) (Object) this;
        target.knockback(0.5D, self.getX() - target.getX(), self.getZ() - target.getZ());
    }

    @Override
    @Invoker("getHurtSound")
    public abstract SoundEvent myGetHurtSound(DamageSource source);

    @Override
    @Invoker("getSoundVolume")
    public abstract float myGetHurtVolume();

    @Override
    public float myGetHurtPitch() {
        return ((LivingEntity) (Object) this).getVoicePitch();
    }

    @ModifyVariable(method = "heal(F)V", at = @At(value = "HEAD"), argsOnly = true, ordinal = 0)
    public float reduceHealPerLevel(float amount) {
        LivingEntity en = (LivingEntity) (Object) this;
        if (en instanceof Player) {
            return HealthUtils.realToVanilla(en, amount);
        }
        return amount;
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At(value = "HEAD"), cancellable = true)
    public void hookench(DamageSource source, float amount, CallbackInfoReturnable<Float> ci) {
        if (DmgSourceUtils.isMyDmgSource(source)) {
            ci.setReturnValue(amount);
        }
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At(value = "RETURN"), cancellable = true)
    public void hookenchreturn(DamageSource source, float amount, CallbackInfoReturnable<Float> ci) {
        LivingEntity en = (LivingEntity) (Object) this;
        if (!source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            LivingHurtUtils.damageCurioItems(en, amount);
        }
        if (DmgSourceUtils.isMyDmgSource(source)) {
            ci.setReturnValue(amount);
        }
    }

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At(value = "HEAD"), cancellable = true)
    public void hookarmortodmg(DamageSource source, float amount, CallbackInfoReturnable<Float> ci) {
        if (DmgSourceUtils.isMyDmgSource(source)) {
            ci.setReturnValue(amount);
        }
    }

    @Inject(method = "canBeAffected", at = @At(value = "HEAD"), cancellable = true)
    public void hook(MobEffectInstance effect, CallbackInfoReturnable<Boolean> ci) {
        try {
            LivingEntity en = (LivingEntity) (Object) this;
            CanEntityHavePotionMixin.hook(en, effect, ci);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "releaseUsingItem", at = @At("HEAD"))
    public void hookReleaseUsingItem(CallbackInfo ci) {
        try {
            LivingEntity en = (LivingEntity) (Object) this;
            if (!en.level().isClientSide()) {
                MinecraftForge.EVENT_BUS.post(
                    new LivingEntityUseItemEvent.Stop(en, en.getUseItem(), en.getUseItemRemainingTicks())
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
