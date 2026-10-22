package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.robertx22.forge_compat.client.event.RegisterKeyMappingsEvent;
import com.robertx22.forge_compat.client.settings.KeyConflictContext;
import com.robertx22.forge_compat.client.settings.KeyModifier;

public class KeybindsRegister {

    // MC 26.3: keybind categories are Identifier-registered objects, not strings.
    public static KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(SlashRef.MODID, "keybind"));
    static String prefix = SlashRef.MODID + ".key.";

    public static KeyMapping HUB_SCREEN_KEY = new KeyMapping(prefix + "hub_screen", InputConstants.KEY_H, KeyMapping.Category.MISC);

    public static KeyMapping UNSUMMON = new KeyMapping(prefix + "unsummon", InputConstants.KEY_MINUS, KeyMapping.Category.MISC);

    public static KeyMapping HOTBAR_SWAP = new KeyMapping(prefix + "hotbar_swap", InputConstants.KEY_F1, KeyMapping.Category.MISC);

    public static KeyMapping QUICK_DRINK_POTION = new KeyMapping(prefix + "quick_drink_potion", InputConstants.Type.KEYBOARD, InputConstants.KEY_P, KeyMapping.Category.MISC);

    public static SpellKeybind SPELL_HOTBAR_1 = new SpellKeybind(1, InputConstants.KEY_R, null, true);
    public static SpellKeybind SPELL_HOTBAR_2 = new SpellKeybind(2, InputConstants.KEY_V, null, true);
    public static SpellKeybind SPELL_HOTBAR_3 = new SpellKeybind(3, InputConstants.KEY_C, null, true);
    public static SpellKeybind SPELL_HOTBAR_4 = new SpellKeybind(4, InputConstants.KEY_G, null, true);
    public static SpellKeybind SPELL_HOTBAR_5 = new SpellKeybind(5, InputConstants.KEY_R, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_6 = new SpellKeybind(6, InputConstants.KEY_V, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_7 = new SpellKeybind(7, InputConstants.KEY_C, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_8 = new SpellKeybind(8, InputConstants.KEY_G, KeyModifier.SHIFT, false);

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
