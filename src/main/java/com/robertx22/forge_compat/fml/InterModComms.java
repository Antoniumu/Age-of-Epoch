package com.robertx22.forge_compat.fml;

import java.util.function.Supplier;

/** No-op on Fabric: inter-mod comms (curios slots etc.) are optional integrations. */
public class InterModComms {
    public static boolean sendTo(String modid, String method, Supplier<?> supplier) {
        return true;
    }
}
