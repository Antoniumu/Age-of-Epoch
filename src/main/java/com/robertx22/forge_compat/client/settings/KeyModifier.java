package com.robertx22.forge_compat.client.settings;

import com.mojang.blaze3d.platform.InputConstants;

import com.robertx22.forge_compat.client.Screen;

public enum KeyModifier {
    CONTROL, SHIFT, ALT, NONE;

    public boolean isActive(KeyConflictContext ctx) {
        return switch (this) {
            case CONTROL -> InputConstants.isKeyDown(InputConstants.KEY_LCONTROL);
            case SHIFT -> InputConstants.isKeyDown(InputConstants.KEY_LSHIFT);
            case ALT -> InputConstants.isKeyDown(InputConstants.KEY_LALT);
            case NONE -> true;
        };
    }
}
