package com.robertx22.age_of_exile.mmorpg.registers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.forge_compat.fml.client.registry.ClientRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeybindsRegister {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(SlashRef.MODID, "keys"));

    public static KeyMapping HUB_SCREEN_KEY = new KeyMapping("Main Hub Screen", InputConstants.KEY_H, CATEGORY);
    public static KeyMapping QUESTBOOK_KEY = new KeyMapping("key.mmorpg.open_questbook", InputConstants.KEY_K, CATEGORY);

    public static KeyMapping SPELL_HOTBAR_1 = new KeyMapping("Use Spell 1", InputConstants.KEY_R, CATEGORY);
    public static KeyMapping SPELL_HOTBAR_2 = new KeyMapping("Use Spell 2", InputConstants.KEY_V, CATEGORY);
    public static KeyMapping SPELL_HOTBAR_3 = new KeyMapping("Use Spell 3", InputConstants.KEY_C, CATEGORY);
    public static KeyMapping SPELL_HOTBAR_4 = new KeyMapping("Use Spell 4", InputConstants.KEY_G, CATEGORY);

    public static KeyMapping WIKI_HOLD_KEY = new KeyMapping("key.mmorpg.hold_open_wiki", InputConstants.KEY_B, CATEGORY);

    public static KeyMapping MOB_PLATE_STYLE_KEY = new KeyMapping("Cycle Mob Plate Style", InputConstants.KEY_J, CATEGORY);
    public static KeyMapping PARTY_HUD_TOGGLE_KEY = new KeyMapping("key.aoe.party_hud_toggle", InputConstants.KEY_F7, CATEGORY);
    public static KeyMapping PARTY_HUD_POSITION_KEY = new KeyMapping("key.aoe.party_hud_position", InputConstants.KEY_F8, CATEGORY);

    public static KeyMapping getSpellHotbar(int num) {
        int n = num;
        if (num > 3) {
            n -= 4;
        }
        if (n == 0) {
            return SPELL_HOTBAR_1;
        }
        if (n == 1) {
            return SPELL_HOTBAR_2;
        }
        if (n == 2) {
            return SPELL_HOTBAR_3;
        } else {
            return SPELL_HOTBAR_4;
        }

    }

    public static void register() {

        ClientRegistry.registerKeyBinding(HUB_SCREEN_KEY);
        ClientRegistry.registerKeyBinding(QUESTBOOK_KEY);

        ClientRegistry.registerKeyBinding(SPELL_HOTBAR_1);
        ClientRegistry.registerKeyBinding(SPELL_HOTBAR_2);
        ClientRegistry.registerKeyBinding(SPELL_HOTBAR_3);
        ClientRegistry.registerKeyBinding(SPELL_HOTBAR_4);
        ClientRegistry.registerKeyBinding(WIKI_HOLD_KEY);
        ClientRegistry.registerKeyBinding(MOB_PLATE_STYLE_KEY);
        ClientRegistry.registerKeyBinding(PARTY_HUD_TOGGLE_KEY);
        ClientRegistry.registerKeyBinding(PARTY_HUD_POSITION_KEY);

    }

}
