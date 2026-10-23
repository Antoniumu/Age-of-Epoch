package com.robertx22.library_of_exile.mixins;

import com.robertx22.library_of_exile.components.EntityInfoComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//copied from api cus i dont want to add it just cus of this one mixin
@Mixin(ServerLevel.class)
public class ServerWorldMixin {

    @Shadow
    boolean tickingEntities;

    // Call our load event after vanilla has loaded the entity
    @Inject(method = "add", at = @At("TAIL"))
    private void onLoadEntity(Entity entity, CallbackInfo ci) {
        try {
            if (!this.tickingEntities) { // Copy vanilla logic, we cannot load entities while the game is ticking entities
                if (entity instanceof LivingEntity) {
                    EntityInfoComponent.get((LivingEntity) entity)
                        .spawnInit(entity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
