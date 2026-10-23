package com.robertx22.infinite_dungeons.main;

import com.robertx22.infinite_dungeons.components.MobIDCap;
import com.robertx22.infinite_dungeons.components.PlayerIDCap;
import com.robertx22.library_of_exile.components.PlayerCapabilities;
import com.robertx22.forge_compat.common.capabilities.CapabilityManager;

public class DungeonCapabilities {

    public static void reg() {

        CapabilityManager.INSTANCE.register(
            PlayerIDCap.class,
            new PlayerIDCap.Storage(), () -> {
                return new PlayerIDCap(null);
            });
        PlayerCapabilities.register(PlayerIDCap.Data, new PlayerIDCap(null));

        CapabilityManager.INSTANCE.register(
            MobIDCap.class,
            new MobIDCap.Storage(), () -> {
                return new MobIDCap(null);
            });
    }
}
