package com.robertx22.age_of_exile.vanilla_mc.packets;

import com.robertx22.age_of_exile.combat.CombatPartyTracker;
import com.robertx22.age_of_exile.gamemaster.GameMaster;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Lightweight client poll for the private, local-player party HUD. */
public class PartyHudRequestPacket extends MyPacket<PartyHudRequestPacket> {
    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "party_hud_request");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) { }

    @Override
    public void saveToData(FriendlyByteBuf buf) { }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
        var teamData = Load.playerRPGData(player).team;
        String team = teamData.isOnTeam()
            ? "Команда · " + teamData.displayName()
            : "Без команды";
        String daily = "Ежедневная цель ещё не назначена";
        String weekly = "Общая цель недели отключена";
        if (GameMaster.isActive()) {
            try {
                daily = GameMaster.get().dailyHudSummary(player);
                weekly = GameMaster.get().weeklyHudSummary();
            } catch (Exception ignored) {
                // The HUD should never interfere with gameplay if optional progression is unavailable.
            }
        }
        Packets.sendToClient(player, new PartyHudSyncPacket(team, daily, weekly, CombatPartyTracker.snapshot(player)));
    }

    @Override
    public MyPacket<PartyHudRequestPacket> newInstance() {
        return new PartyHudRequestPacket();
    }
}
