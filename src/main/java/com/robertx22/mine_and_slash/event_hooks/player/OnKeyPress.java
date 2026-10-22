package com.robertx22.mine_and_slash.event_hooks.player;

import com.robertx22.age_of_exile.a_libraries.mob_plates.MobPlateStyleHud;
import com.robertx22.age_of_exile.questbook.QuestBookScreen;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.gui.screens.character_screen.MainHubScreen;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ChatUtils;
import com.robertx22.mine_and_slash.vanilla_mc.packets.QuickUsePotionPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.UnsummonPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.spells.TellServerToCastSpellPacket;
import net.minecraft.client.Minecraft;

public class OnKeyPress {

    public static int cooldown = 0;
    private static int lastSpellNumber = -1;
    private static int spellPacketResendTimer = 0;

    public static void onEndTick(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        if (ChatUtils.wasChatOpenRecently()) {
            return;
        }

        updateSpellInputs(mc);

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (KeybindsRegister.UNSUMMON.isDown()) {
            Packets.sendToServer(new UnsummonPacket());
            cooldown = 3;
        } else if (KeybindsRegister.HUB_SCREEN_KEY.isDown()) {
            mc.gui.setScreen(new MainHubScreen());
            cooldown = 10;
        } else if (KeybindsRegister.QUESTBOOK_KEY.isDown()) {
            mc.gui.setScreen(new QuestBookScreen());
            cooldown = 10;
        } else if (KeybindsRegister.MOB_PLATE_STYLE_KEY.isDown()) {
            MobPlateStyleHud.cycle();
            cooldown = 12;
        } else if (KeybindsRegister.HOTBAR_SWAP.isDown()) {
            SpellKeybind.IS_ON_SECONd_HOTBAR = !SpellKeybind.IS_ON_SECONd_HOTBAR;
            cooldown = 5;
        } else if (KeybindsRegister.QUICK_DRINK_POTION.consumeClick()) {
            Packets.sendToServer(new QuickUsePotionPacket());
        }
    }

    private static void updateSpellInputs(Minecraft mc) {
        int number = -1;

        // 1. Check explicitly bound standalone keys on slots 5..8 first
        for (int i = 4; i < 8; i++) {
            SpellKeybind sk = SpellKeybind.ALL.get(i);
            if (!sk.key.isUnbound() && sk.key.isDown()) {
                number = i;
                break;
            }
        }

        // 2. Check primary spell keys 1..4 (with Shift or Hotbar Swap shifting to slots 5..8)
        if (number == -1) {
            for (int i = 0; i < 4; i++) {
                SpellKeybind sk = SpellKeybind.ALL.get(i);
                if (sk.key.isDown()) {
                    number = (mc.hasShiftDown() || SpellKeybind.IS_ON_SECONd_HOTBAR) ? (i + 4) : i;
                    break;
                }
            }
        }

        if (number == lastSpellNumber) {
            if (number == -1) {
                return;
            }
            if (spellPacketResendTimer > 0) {
                spellPacketResendTimer--;
                return;
            }
        }

        Packets.sendToServer(new TellServerToCastSpellPacket(number));
        lastSpellNumber = number;
        spellPacketResendTimer = 2;
    }
}
