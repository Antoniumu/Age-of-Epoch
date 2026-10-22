package com.robertx22.library_of_exile.mixins;

import com.robertx22.library_of_exile.components.EntityInfoComponent;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    public void hookOnActuallyHurt(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        try {
            LivingEntity entity = (LivingEntity) (Object) this;
            ExileEvents.DAMAGE_BEFORE_APPLIED.callEvents(new ExileEvents.OnDamageEntity(source, amount, entity));
            if (!entity.level().isClientSide() && source != null && source.getEntity() instanceof Player) {
                EntityInfoComponent.IEntityInfo comp = EntityInfoComponent.get(entity);
                if (comp != null) {
                    comp.getDamageStats().onDamagedBy(source.getEntity(), Math.max(amount, 1.0F));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
