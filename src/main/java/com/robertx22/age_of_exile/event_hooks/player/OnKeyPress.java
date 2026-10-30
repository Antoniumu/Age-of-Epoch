package com.robertx22.age_of_exile.event_hooks.player;

import com.robertx22.age_of_exile.a_libraries.mob_plates.MobPlateStyleHud;
import com.robertx22.age_of_exile.gui.screens.character_screen.CharacterScreen;
import com.robertx22.age_of_exile.gui.overlays.party_hud.PartyHudOverlay;
import com.robertx22.age_of_exile.vanilla_mc.packets.PartyHudRequestPacket;

import com.robertx22.age_of_exile.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ChatUtils;
import com.robertx22.age_of_exile.vanilla_mc.packets.spells.TellServerToCastSpellPacket;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.client.Minecraft;
import com.robertx22.forge_compat.client.Screen;

public class OnKeyPress {

    public static int cooldown = 0;
    private static int partyHudSyncTicks = 0;

    public static void onEndTick(Minecraft mc) {

        // открытие вики по удержанию клавиши на предмете (WikiHoldToOpen):
        // ставится в очередь из тултипа, выполняется здесь — в тике, а не в рендере
        if (com.robertx22.age_of_exile.wiki.WikiHoldToOpen.consumePendingOpen()) {
            cooldown = 10;
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (mc.player == null) {
            partyHudSyncTicks = 0;
            return;
        }

        if (mc.level != null && ++partyHudSyncTicks >= 10) {
            partyHudSyncTicks = 0;
            Packets.sendToServer(new PartyHudRequestPacket());
        }

        if (mc.gui.screen() instanceof com.robertx22.age_of_exile.airship.AirshipDialogueScreen) return;

        if (ChatUtils.wasChatOpenRecently()) {
            return;
        }

        if (KeybindsRegister.PARTY_HUD_TOGGLE_KEY.isDown()) {
            PartyHudOverlay.toggle();
            cooldown = 10;
        } else if (KeybindsRegister.PARTY_HUD_POSITION_KEY.isDown()) {
            PartyHudOverlay.cyclePosition();
            cooldown = 10;
        } else if (KeybindsRegister.MOB_PLATE_STYLE_KEY.isDown()) {
            // Shift+J switches how entities with a vanilla nickname are treated,
            // plain J cycles the visual style.
            if (mc.hasShiftDown()) {
                MobPlateStyleHud.cycleNamedMode();
            } else {
                MobPlateStyleHud.cycle();
            }
            cooldown = 12;
        } else if (KeybindsRegister.HUB_SCREEN_KEY.isDown()) {
            mc.gui.setScreen(new CharacterScreen());
            cooldown = 10;
        } else {

            int number = -1;

            if (KeybindsRegister.SPELL_HOTBAR_1.isDown()) {
                number = 0;
            } else if (KeybindsRegister.SPELL_HOTBAR_2.isDown()) {
                number = 1;
            } else if (KeybindsRegister.SPELL_HOTBAR_3.isDown()) {
                number = 2;
            } else if (KeybindsRegister.SPELL_HOTBAR_4.isDown()) {
                number = 3;
            } else {
                number -= 500; // dont cast
            }

            if (mc.hasShiftDown()) {
                number += 4;
            }
            if (number > -1) {
                // todo make sure its not lagging servers
                Packets.sendToServer(new TellServerToCastSpellPacket(number));
            }
        }
    }
}
