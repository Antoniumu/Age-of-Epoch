package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.simulation.SimulationManager;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.entity.living.LivingAttackEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDamageEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.forge_compat.event.entity.living.LivingHurtEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    private float ageOfEpoch$modifyDamage(float amount, ServerLevel level, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        LivingAttackEvent atk = new LivingAttackEvent(self, source, amount);
        MinecraftForge.EVENT_BUS.post(atk);
        if (atk.isCanceled()) {
            return 0.0F;
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : self;
        LivingHurtEvent hurt = new LivingHurtEvent(self, attacker, source, amount);
        MinecraftForge.EVENT_BUS.post(hurt);
        if (hurt.isCanceled() || hurt.getAmount() <= 0.0F) {
            return 0.0F;
        }
        LivingDamageEvent dmg = new LivingDamageEvent(self, source, hurt.getAmount());
        MinecraftForge.EVENT_BUS.post(dmg);
        float finalAmount = dmg.isCanceled() ? 0.0F : Math.max(0.0F, dmg.getAmount());
        if (finalAmount > 0.0F && self instanceof ServerPlayer sp) {
            if (SimulationManager.interceptLethalDamageInSimulation(sp, finalAmount)) {
                return 0.0F;
            }
        }
        return finalAmount;
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void ageOfEpoch$onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer sp && SimulationManager.isSimulationLevel(sp.level())) {
            SimulationManager.healPlayerFull(sp);
            ci.cancel();
            return;
        }
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(self, source));
    }
}
