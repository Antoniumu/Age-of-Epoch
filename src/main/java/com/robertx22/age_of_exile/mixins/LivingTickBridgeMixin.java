package com.robertx22.age_of_exile.mixins;

import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.entity.living.LivingEvent;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingTickBridgeMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void ageOfEpoch$onLivingTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(self));
        if (!self.level().isClientSide() && self instanceof Spider spider && spider.tickCount % 20 == 0) {
            if (spider.getTarget() == null && MapDimensions.isMap(spider.level())) {
                Player nearest = spider.level().getNearestPlayer(spider, 16.0);
                if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                    spider.setTarget(nearest);
                }
            }
        }
    }
}
