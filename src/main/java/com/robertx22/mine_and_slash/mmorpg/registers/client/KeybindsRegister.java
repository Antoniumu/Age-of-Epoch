package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.forge_compat.client.event.RegisterKeyMappingsEvent;
import com.robertx22.forge_compat.client.settings.KeyModifier;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeybindsRegister {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(SlashRef.MODID, "keys"));
    static String prefix = SlashRef.MODID + ".key.";

    public static KeyMapping HUB_SCREEN_KEY = new KeyMapping(prefix + "hub_screen", InputConstants.KEY_H, CATEGORY);
    public static KeyMapping QUESTBOOK_KEY = new KeyMapping("key.mmorpg.open_questbook", InputConstants.KEY_K, CATEGORY);
    public static KeyMapping MOB_PLATE_STYLE_KEY = new KeyMapping(prefix + "mob_plate_style", InputConstants.KEY_J, CATEGORY);

    public static KeyMapping UNSUMMON = new KeyMapping(prefix + "unsummon", 45, CATEGORY);
    public static KeyMapping HOTBAR_SWAP = new KeyMapping(prefix + "hotbar_swap", 296, CATEGORY);
    public static KeyMapping QUICK_DRINK_POTION = new KeyMapping(prefix + "quick_drink_potion", InputConstants.KEY_P, CATEGORY);

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
        x.register(QUESTBOOK_KEY);
        x.register(MOB_PLATE_STYLE_KEY);
        x.register(UNSUMMON);
        x.register(HOTBAR_SWAP);
        x.register(QUICK_DRINK_POTION);
        for (SpellKeybind k : SpellKeybind.ALL) {
            x.register(k.key);
        }
    }
}
