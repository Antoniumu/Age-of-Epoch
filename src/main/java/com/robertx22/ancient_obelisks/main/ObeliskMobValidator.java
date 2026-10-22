package com.robertx22.ancient_obelisks.main;

import com.robertx22.ancient_obelisks.capability.ObeliskEntityCapability;
import com.robertx22.library_of_exile.dimension.MobValidator;
import net.minecraft.world.entity.LivingEntity;

public class ObeliskMobValidator extends MobValidator {
    @Override
    public boolean isValidMob(LivingEntity en) {
        return ObeliskEntityCapability.get(en).data.isObeSpawn;
    }
}
