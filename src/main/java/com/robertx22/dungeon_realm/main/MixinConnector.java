package com.robertx22.dungeon_realm.main;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MixinConnector implements IMixinConnector {

    @Override
    public void connect() {
        Mixins.addConfiguration(
                "dungeon_realm-mixins.json"
        );
    }
}