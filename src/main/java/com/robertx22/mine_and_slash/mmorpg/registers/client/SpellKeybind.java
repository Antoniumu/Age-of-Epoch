package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.forge_compat.client.settings.KeyModifier;
import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.List;

public class SpellKeybind {

    public static boolean IS_ON_SECONd_HOTBAR = false;

    public static List<SpellKeybind> ALL = new ArrayList<>();
    public static List<SpellKeybind> FIRST_HOTBAR_KEYS = new ArrayList<>();

    static {
        try {
            Class.forName(KeybindsRegister.class.getName());
        } catch (ClassNotFoundException ignored) {
        }
    }

    public KeyMapping key;
    public KeyModifier modifier;
    public int defaultKey;
    public int num = 0;
    boolean firstbar;

    public SpellKeybind(int num, int key, KeyModifier mod, boolean firstbar) {
        // On vanilla Fabric, two KeyMappings sharing the same GLFW keycode overwrite each other in KeyMapping.MAP.
        // Leave modifier-shifted slots 5..8 unbound by default so Shift + Spell 1..4 works out of the box AND players
        // can also bind dedicated standalone keys for Spells 5..8 in Controls if desired!
        int initialBind = (mod != null) ? InputConstants.UNKNOWN.getValue() : key;
        this.key = new KeyMapping(KeybindsRegister.prefix + "spell_" + num, initialBind, KeybindsRegister.CATEGORY);
        this.modifier = mod;
        this.defaultKey = key;
        this.firstbar = firstbar;
        this.num = num;

        ALL.add(this);
        if (firstbar) {
            FIRST_HOTBAR_KEYS.add(this);
        }
    }

    public int getIndex() {
        return num - 1;
    }
}
