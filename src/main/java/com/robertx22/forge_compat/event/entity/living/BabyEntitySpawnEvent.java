package com.robertx22.forge_compat.event.entity.living;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class BabyEntitySpawnEvent extends Event {
    private final Mob parentA, parentB;
    private final Player causedByPlayer;
    private AgeableMob child;

    public BabyEntitySpawnEvent(Mob parentA, Mob parentB, AgeableMob child, Player causedByPlayer) {
        this.parentA = parentA;
        this.parentB = parentB;
        this.child = child;
        this.causedByPlayer = causedByPlayer;
    }

    public Mob getParentA() { return parentA; }
    public Mob getParentB() { return parentB; }
    public AgeableMob getChild() { return child; }
    public void setChild(AgeableMob child) { this.child = child; }
    public Player getCausedByPlayer() { return causedByPlayer; }
}
