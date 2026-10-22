package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.simulation.SimulationManager;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.entity.living.LivingAttackEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDamageEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.mixin_ducks.DamageSourceDuck;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingDamageBridgeMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void ageOfEpoch$onLivingAttack(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        LivingAttackEvent attackEvent = new LivingAttackEvent(self, source, amount);
        MinecraftForge.EVENT_BUS.post(attackEvent);
        if (attackEvent.isCanceled()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ageOfEpoch$modifyDamage(float amount, ServerLevel level, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        LivingDamageEvent ev = new LivingDamageEvent(self, source, amount);
        MinecraftForge.EVENT_BUS.post(ev);
        if (ev.isCanceled()) {
            return 0.0F;
        }
        float out = Math.max(0.0F, ev.getAmount());
        if (out > 0.0F && self instanceof ServerPlayer sp) {
            if (SimulationManager.interceptLethalDamageInSimulation(sp, out)) {
                return 0.0F;
            }
        }
        try {
            if (source instanceof DamageSourceDuck duck && duck.hasMnsDamageOverride() && duck.getMnsDamage() != out) {
                if (source.getEntity() instanceof Player p
                    && Load.player(p).config.isConfigEnabled(PlayerConfigData.Config.DAMAGE_CONFLICT_MSG)
                    && !Load.Unit(p).getCooldowns().isOnCooldown("dmg_conflict")) {
                    Load.Unit(p).getCooldowns().setOnCooldown("dmg_conflict", 20);
                    p.sendSystemMessage(Component.literal("Some mod has modified Mine and Slash damage!"));
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void ageOfEpoch$onLivingDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer sp && WorldUtils.isSimulationWorld(sp.level())) {
            SimulationManager.healPlayerFull(sp);
            ci.cancel();
            return;
        }
        try {
            if (self instanceof Mob mob && WorldUtils.isMapWorldClass(self.level(), self.blockPosition())) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    mob.setDropChance(slot, 0);
                }
            }
        } catch (Exception ignored) {
        }
        LivingDeathEvent ev = new LivingDeathEvent(self, source);
        MinecraftForge.EVENT_BUS.post(ev);
        if (ev.isCanceled()) {
            ci.cancel();
        }
    }
}
