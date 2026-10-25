package com.robertx22.age_of_exile.vanilla_mc.packets;

import com.robertx22.age_of_exile.combat.CombatPartyTracker;
import com.robertx22.age_of_exile.combat.PartyHudState;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative party, goal and encounter data for the compact HUD. */
public class PartyHudSyncPacket extends MyPacket<PartyHudSyncPacket> {
    private String team = "";
    private String daily = "";
    private String weekly = "";
    private boolean combat;
    private String squadName = "";
    private String squadIcon = "";
    private List<CombatPartyTracker.Member> members = List.of();

    public PartyHudSyncPacket() { }

    public PartyHudSyncPacket(String team, String daily, String weekly, CombatPartyTracker.Snapshot fight) {
        this.team = team;
        this.daily = daily;
        this.weekly = weekly;
        this.combat = fight.active();
        this.squadName = fight.title();
        this.squadIcon = fight.icon();
        this.members = fight.members().stream().limit(8).toList();
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "party_hud_sync");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        team = buf.readUtf(96);
        daily = buf.readUtf(160);
        weekly = buf.readUtf(160);
        combat = buf.readBoolean();
        squadName = buf.readUtf(64);
        squadIcon = buf.readUtf(8);
        int count = buf.readVarInt();
        members = new ArrayList<>(Math.min(count, 8));
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf(32);
            int color = buf.readInt();
            double dps = buf.readDouble();
            if (i < 8) members.add(new CombatPartyTracker.Member(name, color, dps));
        }
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeUtf(team, 96);
        buf.writeUtf(daily, 160);
        buf.writeUtf(weekly, 160);
        buf.writeBoolean(combat);
        buf.writeUtf(squadName, 64);
        buf.writeUtf(squadIcon, 8);
        buf.writeVarInt(members.size());
        for (CombatPartyTracker.Member member : members) {
            buf.writeUtf(member.name(), 32);
            buf.writeInt(member.color());
            buf.writeDouble(member.dps());
        }
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        PartyHudState.update(team, daily, weekly,
            new CombatPartyTracker.Snapshot(combat, squadName, squadIcon, members));
    }

    @Override
    public MyPacket<PartyHudSyncPacket> newInstance() {
        return new PartyHudSyncPacket();
    }
}
