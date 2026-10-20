package com.robertx22.infinite_dungeons.db_init;

import com.robertx22.infinite_dungeons.main.MainID;

public class RegistryInit {

    static boolean didInit = false;

    public static void init() {

        if (didInit) {
            return;
        }

        RegistryTypes.init();
        AddRegistryContainers.addAll();
        RegisterNonDatapackEntries.register();
        if (MainID.RUN_DEV_TOOLS()) {
            AddToDataGen.addAll();
        }

        didInit = true;
    }
}
