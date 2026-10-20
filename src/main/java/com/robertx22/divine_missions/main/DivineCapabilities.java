package com.robertx22.divine_missions.main;

import com.robertx22.divine_missions.components.PlayerMissionCap;
import com.robertx22.library_of_exile.components.PlayerCapabilities;
import com.robertx22.forge_compat.common.capabilities.CapabilityManager;

public class DivineCapabilities {

    public static void reg() {

        CapabilityManager.INSTANCE.register(
            PlayerMissionCap.class,
            new PlayerMissionCap.Storage(), () -> {
                return new PlayerMissionCap(null);
            });
        PlayerCapabilities.register(PlayerMissionCap.Data, new PlayerMissionCap(null));

    }
}
