package com.robertx22.divine_missions.db_init;

import com.robertx22.divine_missions.main.DivineMissions;

public class RegistryInit {

    static boolean didInit = false;

    public static void init() {

        if (didInit) {
            return;
        }

        RegistryTypes.init();
        AddRegistryContainers.addAll();
        RegisterNonDatapackEntries.register();
        if (DivineMissions.RUN_DEV_TOOLS()) {
            AddToDataGen.addAll();
        }

        didInit = true;
    }
}
