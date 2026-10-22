package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.KeyMapping;
import com.robertx22.forge_compat.client.event.RegisterKeyMappingsEvent;
import com.robertx22.forge_compat.client.settings.KeyConflictContext;
import com.robertx22.forge_compat.client.settings.KeyModifier;

public class KeybindsRegister {

    public static KeyMapping.Category CATEGORY = KeyMapping.Category.MISC;
    static String prefix = SlashRef.MODID + ".key.";

    public static KeyMapping HUB_SCREEN_KEY = new KeyMapping(prefix + "hub_screen", 72, KeyMapping.Category.MISC);

    public static KeyMapping UNSUMMON = new KeyMapping(prefix + "unsummon", 45, KeyMapping.Category.MISC);

    public static KeyMapping HOTBAR_SWAP = new KeyMapping(prefix + "hotbar_swap", 296, KeyMapping.Category.MISC);

    public static KeyMapping QUICK_DRINK_POTION = new KeyMapping(prefix + "quick_drink_potion", 80, KeyMapping.Category.MISC);

    public static SpellKeybind SPELL_HOTBAR_1 = new SpellKeybind(1, 82, null, true);
    public static SpellKeybind SPELL_HOTBAR_2 = new SpellKeybind(2, 86, null, true);
    public static SpellKeybind SPELL_HOTBAR_3 = new SpellKeybind(3, 67, null, true);
    public static SpellKeybind SPELL_HOTBAR_4 = new SpellKeybind(4, 71, null, true);
    public static SpellKeybind SPELL_HOTBAR_5 = new SpellKeybind(5, 82, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_6 = new SpellKeybind(6, 86, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_7 = new SpellKeybind(7, 67, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_8 = new SpellKeybind(8, 71, KeyModifier.SHIFT, false);

    public static SpellKeybind getSpellHotbar(int num) {
        return SpellKeybind.ALL.stream().filter(x -> x.getIndex() == num).findAny().orElseThrow(() -> new RuntimeException(num + " isn't a valid hotbar number"));
    }

    public static void register(RegisterKeyMappingsEvent x) {

        x.register(HUB_SCREEN_KEY);
        x.register(UNSUMMON);
        x.register(HOTBAR_SWAP);
        x.register(QUICK_DRINK_POTION);
        for (SpellKeybind k : SpellKeybind.ALL) {
            x.register(k.key);
        }

    }

}
