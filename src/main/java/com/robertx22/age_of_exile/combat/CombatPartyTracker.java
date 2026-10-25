package com.robertx22.age_of_exile.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-only, quiet encounter grouping. A fight exists per mob; no invites or notifications. */
public final class CombatPartyTracker {
    private static final long FIGHT_TIMEOUT_MS = 6_000L;
    private static final long DPS_WINDOW_MS = 5_000L;
    private static final Map<UUID, Encounter> ENCOUNTERS = new HashMap<>();
    private static final String[] TITLES = {"Сапфировый дозор", "Янтарный отряд", "Лунная стража", "Штормовой клинок", "Тихий авангард", "Звёздный рейд"};
    private static final String[] ICONS = {"✦", "◆", "◈", "✧", "⬟", "✺"};

    private CombatPartyTracker() {
    }

    public static void recordDamage(Player attacker, LivingEntity target, float damage) {
        if (!(attacker instanceof ServerPlayer) || !(target instanceof Mob) || damage <= 0 || target.level().isClientSide()) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID targetId = target.getUUID();
        Encounter encounter = ENCOUNTERS.get(targetId);
        if (encounter == null || now - encounter.lastHit > FIGHT_TIMEOUT_MS) {
            encounter = new Encounter(targetId, now);
            ENCOUNTERS.put(targetId, encounter);
        }
        Fighter fighter = encounter.fighters.computeIfAbsent(attacker.getUUID(), id -> new Fighter(attacker));
        fighter.name = attacker.getGameProfile().name();
        fighter.color = playerColor(attacker);
        fighter.hits.addLast(new Hit(now, damage));
        fighter.lastHit = now;
        encounter.lastHit = now;
        trim(fighter, now);
    }

    public static Snapshot snapshot(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Encounter>> iterator = ENCOUNTERS.entrySet().iterator();
        Encounter selected = null;
        long ownLastHit = Long.MIN_VALUE;
        while (iterator.hasNext()) {
            Encounter encounter = iterator.next().getValue();
            if (now - encounter.lastHit > FIGHT_TIMEOUT_MS) {
                iterator.remove();
                continue;
            }
            Fighter own = encounter.fighters.get(player.getUUID());
            long activeFighters = encounter.fighters.values().stream()
                .filter(fighter -> now - fighter.lastHit <= FIGHT_TIMEOUT_MS).count();
            if (own != null && now - own.lastHit <= FIGHT_TIMEOUT_MS && activeFighters >= 3
                && own.lastHit > ownLastHit) {
                selected = encounter;
                ownLastHit = own.lastHit;
            }
        }
        if (selected == null) {
            return Snapshot.idle();
        }
        List<Member> members = new ArrayList<>();
        for (Fighter fighter : selected.fighters.values()) {
            if (now - fighter.lastHit <= FIGHT_TIMEOUT_MS) {
                trim(fighter, now);
                members.add(new Member(fighter.name, fighter.color, dps(fighter, now)));
            }
        }
        if (members.size() < 3) {
            return Snapshot.idle();
        }
        members.sort(Comparator.comparingDouble((Member row) -> row.dps).reversed());
        return new Snapshot(true, selected.title, selected.icon, members);
    }

    private static double dps(Fighter fighter, long now) {
        trim(fighter, now);
        double damage = 0;
        for (Hit hit : fighter.hits) {
            if (now - hit.at <= DPS_WINDOW_MS) damage += hit.damage;
        }
        double elapsed = Math.min(DPS_WINDOW_MS, Math.max(500L, now - fighter.startedAt));
        return damage * 1000.0 / elapsed;
    }

    private static void trim(Fighter fighter, long now) {
        while (!fighter.hits.isEmpty() && now - fighter.hits.peekFirst().at > DPS_WINDOW_MS) {
            fighter.hits.removeFirst();
        }
    }

    private static int playerColor(Player player) {
        int teamColor = player.getTeamColor();
        if (teamColor != 0xFFFFFF) return teamColor;
        int[] palette = {0x58C7FA, 0xF2B84B, 0xB48CFF, 0x5BDBA5, 0xFF7385, 0x70A0FF};
        return palette[Math.floorMod(player.getUUID().hashCode(), palette.length)];
    }

    private static final class Encounter {
        final UUID target;
        final long startedAt;
        final Map<UUID, Fighter> fighters = new HashMap<>();
        final String title;
        final String icon;
        long lastHit;

        Encounter(UUID target, long now) {
            this.target = target;
            this.startedAt = now;
            int index = Math.floorMod(target.hashCode(), TITLES.length);
            this.title = TITLES[index];
            this.icon = ICONS[index];
            this.lastHit = now;
        }
    }

    private static final class Fighter {
        String name;
        int color;
        final long startedAt = System.currentTimeMillis();
        long lastHit;
        final ArrayDeque<Hit> hits = new ArrayDeque<>();
        Fighter(Player player) {
            this.name = player.getGameProfile().name();
            this.color = playerColor(player);
            this.lastHit = startedAt;
        }
    }

    private record Hit(long at, float damage) { }
    public record Member(String name, int color, double dps) { }
    public record Snapshot(boolean active, String title, String icon, List<Member> members) {
        static Snapshot idle() { return new Snapshot(false, "", "", List.of()); }
    }
}
