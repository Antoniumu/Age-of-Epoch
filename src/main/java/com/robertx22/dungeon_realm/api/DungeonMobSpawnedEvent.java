package com.robertx22.dungeon_realm.api;

import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.entity.LivingEntity;

public class DungeonMobSpawnedEvent extends ExileEvent {

    public LivingEntity mob;

    public DungeonMobSpawnedEvent(LivingEntity mob) {
        this.mob = mob;
    }
}
