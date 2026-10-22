package com.robertx22.library_of_exile.config.map_dimension;

import net.minecraft.world.level.gamerules.GameRules;

public class MapGameRules extends GameRules {
    public GameRules rules;

    public MapGameRules(GameRules rules) {
        super();
        this.rules = rules;
    }
}
