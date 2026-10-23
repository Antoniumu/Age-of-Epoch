package com.robertx22.age_of_exile.uncommon.utilityclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class MapManager {

    public static Identifier getResourceLocation(Level world) {
        Identifier loc = world.dimension().identifier();
        return loc;
    }
}
