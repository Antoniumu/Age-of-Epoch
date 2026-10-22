package com.robertx22.library_of_exile.config.map_dimension;

import net.minecraft.world.level.gamerules.GameRules;

// MC 26.3: GameRules has no default constructor anymore; this was only a wrapper, keep it as a holder
public class MapGameRules {
    public GameRules rules;

    public MapGameRules(GameRules rules) {
        this.rules = rules;
    }
}
