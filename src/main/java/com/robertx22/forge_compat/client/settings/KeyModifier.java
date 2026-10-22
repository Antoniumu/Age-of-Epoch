package com.robertx22.forge_compat.client.settings;

import com.robertx22.forge_compat.client.Screen;

public enum KeyModifier {
    CONTROL, SHIFT, ALT, NONE;

    public boolean isActive(KeyConflictContext ctx) {
        return switch (this) {
            case CONTROL -> Screen.hasControlDown();
            case SHIFT -> Screen.hasShiftDown();
            case ALT -> Screen.hasAltDown();
            case NONE -> true;
        };
    }
}
