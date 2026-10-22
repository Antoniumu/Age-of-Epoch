package com.robertx22.dungeon_realm.api;

import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.entity.LivingEntity;

public class SpawnUberEvent extends ExileEvent {

    public LivingEntity uberBoss;

    public SpawnUberEvent(LivingEntity uberBoss) {
        this.uberBoss = uberBoss;
    }
}
