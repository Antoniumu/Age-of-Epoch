package com.robertx22.library_of_exile.utils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class EntityUtils {

    public static float getVanillaMaxHealth(LivingEntity entity) {

        try {

            float val = (float) entity.getAttribute(Attributes.MAX_HEALTH)
                .getBaseValue();

            return val;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void setLoc(LivingEntity entity, Vec3 p, float yaw, float pitch) {
        entity.teleportTo(p.x, p.y, p.z);
    }

    public static void setLoc(LivingEntity entity, BlockPos p) {
        entity.teleportTo(p.getX(), p.getY(), p.getZ());
    }

}
