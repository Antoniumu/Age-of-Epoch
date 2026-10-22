package com.robertx22.forge_compat.client.settings;

import net.minecraft.client.Minecraft;

import com.robertx22.forge_compat.client.Screen;

public enum KeyModifier {
    CONTROL, SHIFT, ALT, NONE;

    public boolean isActive(KeyConflictContext ctx) {
        return switch (this) {
            case CONTROL -> Minecraft.getInstance().hasControlDown();
            case SHIFT -> Minecraft.getInstance().hasShiftDown();
            case ALT -> Minecraft.getInstance().hasAltDown();
            case NONE -> true;
        };
    }
}
