package com.robertx22.the_harvest.main;

import com.robertx22.library_of_exile.dimension.MobValidator;
import com.robertx22.the_harvest.capability.HarvestEntityCap;
import net.minecraft.world.entity.LivingEntity;

public class HarvestMobValidator extends MobValidator {
    @Override
    public boolean isValidMob(LivingEntity en) {
        if (!HarvestEntityCap.get(en).data.isHarvestSpawn) {
            return false;
        }


        return true;
    }
}
