package com.robertx22.dungeon_realm.main;

import com.robertx22.dungeon_realm.capability.DungeonEntityCapability;
import com.robertx22.library_of_exile.dimension.MobValidator;
import net.minecraft.world.entity.LivingEntity;

public class DungeonMobValidator extends MobValidator {
    @Override
    public boolean isValidMob(LivingEntity en) {
        if (!DungeonEntityCapability.get(en).data.isDungeonMob) {
            return false;
        }

        return true;
    }
}
