package com.robertx22.age_of_exile.mixins;

import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.entity.living.LivingDamageEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.forge_compat.event.entity.living.LivingHurtEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingDamageBridgeMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ageOfEpoch$modifyDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : self;
        LivingHurtEvent hurt = new LivingHurtEvent(self, attacker, source, amount);
        MinecraftForge.EVENT_BUS.post(hurt);
        LivingDamageEvent dmg = new LivingDamageEvent(self, source, hurt.getAmount());
        MinecraftForge.EVENT_BUS.post(dmg);
        return dmg.getAmount();
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void ageOfEpoch$onDeath(DamageSource source, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent((LivingEntity) (Object) this, source));
    }
}
