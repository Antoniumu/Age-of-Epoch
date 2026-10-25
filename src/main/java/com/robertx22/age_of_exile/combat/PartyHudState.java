package com.robertx22.age_of_exile.combat;

import java.util.List;

/** Last server-authoritative HUD state received by the local client. */
public final class PartyHudState {
    public static volatile String team = "Команда не создана";
    public static volatile String daily = "Загрузка ежедневной цели…";
    public static volatile String weekly = "Загрузка цели недели…";
    public static volatile boolean combat = false;
    public static volatile String squadName = "";
    public static volatile String squadIcon = "";
    public static volatile List<CombatPartyTracker.Member> members = List.of();
    public static volatile long lastUpdate = 0;

    private PartyHudState() { }

    public static void update(String team, String daily, String weekly, CombatPartyTracker.Snapshot fight) {
        PartyHudState.team = team;
        PartyHudState.daily = daily;
        PartyHudState.weekly = weekly;
        PartyHudState.combat = fight.active();
        PartyHudState.squadName = fight.title();
        PartyHudState.squadIcon = fight.icon();
        PartyHudState.members = List.copyOf(fight.members());
        PartyHudState.lastUpdate = System.currentTimeMillis();
    }
}
