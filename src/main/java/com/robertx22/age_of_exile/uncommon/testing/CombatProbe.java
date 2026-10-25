package com.robertx22.age_of_exile.uncommon.testing;

import com.robertx22.age_of_exile.capability.PlayerDamageChart;
import com.robertx22.age_of_exile.capability.player.data.TeamData;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.combat.CombatPartyTracker;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.effectdatas.DamageEvent;
import com.robertx22.age_of_exile.uncommon.effectdatas.EventBuilder;
import com.robertx22.age_of_exile.uncommon.enumclasses.AttackType;
import com.robertx22.age_of_exile.uncommon.enumclasses.PlayStyle;
import com.robertx22.age_of_exile.uncommon.enumclasses.WeaponTypes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Замеры боевых чисел в живом мире: урон/DPS и лечение.
 *
 * <p>Зачем зонд, а не юнит-тест: DPS и хил в моде — это сквозной путь
 * {@code DamageEvent} → {@code PlayerDamageChart} → {@code CombatPartyTracker},
 * а лечение идёт через {@code ResourcesData} → {@code HealthUtils}. Такой путь
 * проверяется только в запущенной игре.
 *
 * <p>Почему по тикам, а не одним заходом: удары наносятся по одному за тик
 * сервера. Никаких {@code Thread.sleep} на серверном потоке — иначе клиент
 * замирает и скриншоты снимаются «в стол».
 *
 * <p>Запуск: команда {@code /aoe devtest all} либо автоматически, если в каталоге
 * игры лежит флаг-файл {@code aoe-devtest.flag} (его кладёт CI-сценарий
 * {@code tools/e2e/scenarios/combat.txt}). Результат печатается в лог строками
 * {@code AOETEST ...} и дописывается в {@code aoe-devtest.txt}.
 */
public final class CombatProbe {

    public static final String TAG = "AOETEST";
    /** Флаг-файл: есть в каталоге игры — значит зонд надо прогнать самому. */
    public static final String FLAG_FILE = "aoe-devtest.flag";
    private static final String RESULT_FILE = "aoe-devtest.txt";

    private static final Logger LOGGER = LoggerFactory.getLogger("AgeOfEpoch-DevTest");

    /** Ударов наносим столько: заполняет окно DPS и даёт устойчивое среднее. */
    private static final int HITS = 10;
    /** «Бумажный» урон удара — как у обычного оружия начального уровня. */
    private static final float HIT_DAMAGE = 40F;

    private static Runner active;

    private CombatProbe() {
    }

    // ------------------------------------------------------------------ запуск

    /** Ставит прогон в очередь: {@code what} = dps | heal | all. */
    public static void request(String what) {
        if (active != null) {
            LOGGER.info("{} пропуск: прогон уже идёт", TAG);
            return;
        }
        active = new Runner(what);
        LOGGER.info("{} старт прогона what={}", TAG, what);
    }

    /** Вызывается каждый тик сервера: двигает прогон на один шаг. */
    public static void tick(MinecraftServer server) {
        if (active == null && flagPresent()) {
            request("all");
        }
        if (active == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
        if (player == null) {
            // Игрок ещё не зашёл в мир — ждём следующего тика.
            return;
        }
        Runner runner = active;
        try {
            if (runner.step(player)) {
                active = null;
                clearFlag();
            }
        } catch (Throwable error) {
            LOGGER.error("{} прогон упал", TAG, error);
            write(List.of(TAG + " " + runner.what + " fatal=" + error));
            active = null;
            clearFlag();
        }
    }

    private static Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    private static boolean flagPresent() {
        return Files.exists(gameDir().resolve(FLAG_FILE));
    }

    private static void clearFlag() {
        try {
            Files.deleteIfExists(gameDir().resolve(FLAG_FILE));
        } catch (IOException error) {
            LOGGER.warn("{} не удалось удалить флаг: {}", TAG, error.toString());
        }
    }

    private static void write(List<String> lines) {
        for (String line : lines) {
            LOGGER.info(line);
        }
        try {
            Path path = gameDir().resolve(RESULT_FILE);
            Files.write(path, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException error) {
            LOGGER.warn("{} не удалось записать {}: {}", TAG, RESULT_FILE, error.toString());
        }
    }

    // ------------------------------------------------------------------ прогон

    /** Пошаговая машина: каждый вызов {@link #step} — один тик сервера. */
    private static final class Runner {
        private final String what;
        private final List<String> lines = new ArrayList<>();

        private Mob dummy;
        private int hitsDone;
        private float damageBefore;
        private float requested;
        private long startedAt;

        private int healStage;
        private float healBefore;
        private float healExpected;
        private float manaMax;

        private int settle;
        private boolean dpsReported;
        private boolean healReported;
        private boolean teamReported;

        Runner(String what) {
            this.what = what;
        }

        /** @return true, когда прогон полностью завершён */
        boolean step(ServerPlayer player) {
            if (what.equals("dps") || what.equals("all")) {
                if (!dpsReported) {
                    dpsStep(player);
                    return false;
                }
            }
            if (what.equals("heal") || what.equals("all")) {
                if (!healReported) {
                    healStep(player);
                    return false;
                }
            }
            if (what.equals("team") || what.equals("all")) {
                if (!teamReported) {
                    teamStep(player);
                    return false;
                }
            }
            if (settle++ < 5) {
                return false;
            }
            lines.add(String.format(Locale.ROOT, "%s summary checks=%d failed=%d", TAG, checks, failed));
            write(lines);
            return true;
        }

        private int checks;
        private int failed;

        // ---- DPS

        private void dpsStep(ServerPlayer player) {
            if (dummy == null) {
                if (!spawnDummy(player)) {
                    lines.add(TAG + " dps fatal=dummy_not_spawned");
                    failed++;
                    checks++;
                    dpsReported = true;
                    return;
                }
                damageBefore = PlayerDamageChart.getDamage(player);
                startedAt = System.currentTimeMillis();
                return;
            }
            if (hitsDone < HITS) {
                hitsDone++;
                // Идём тем же путём, что и обычный удар оружием: DamageEvent
                // считает урон, применяет его и пишет в PlayerDamageChart и
                // CombatPartyTracker. Напрямую mob.hurt() это не проверяет.
                DamageEvent event = EventBuilder.ofDamage(player, dummy, HIT_DAMAGE)
                    .setupDamage(AttackType.attack, WeaponTypes.sword, PlayStyle.melee)
                    .setIsBasicAttack()
                    .build();
                event.Activate();
                requested += HIT_DAMAGE;
                return;
            }

            // Все удары нанесены — снимаем показания.
            long elapsed = Math.max(1L, System.currentTimeMillis() - startedAt);
            float recorded = PlayerDamageChart.getDamage(player) - damageBefore;
            CombatPartyTracker.Snapshot snapshot = CombatPartyTracker.snapshot(player);
            double hudDps = snapshot.members().stream()
                .mapToDouble(CombatPartyTracker.Member::dps)
                .max()
                .orElse(0D);
            double expectedDps = recorded * 1000.0 / elapsed;
            double ratio = hudDps / Math.max(1.0D, expectedDps);

            Report report = new Report("dps");
            report.put("hits", hitsDone);
            report.put("requested", requested);
            report.put("recorded", recorded);
            report.put("elapsed_ms", elapsed);
            report.put("dps_chart", String.format(Locale.ROOT, "%.1f", expectedDps));
            report.put("dps_hud", String.format(Locale.ROOT, "%.1f", hudDps));
            report.put("fight_active", snapshot.active());
            report.check("damage_landed", recorded > 0);
            report.check("damage_matches", recorded > requested * 0.1F);
            report.check("fight_detected", snapshot.active());
            // DPS в панели считается по окну 5 с, поэтому он ниже «мгновенного»
            // значения из PlayerDamageChart — сравниваем с допуском, а не точно.
            report.check("dps_ratio", ratio > 0.05D && ratio < 20.0D);

            lines.add(report.line());
            checks += report.checks;
            failed += report.failed;
            dpsReported = true;
            if (dummy != null) {
                dummy.discard();
                dummy = null;
            }
        }

        private boolean spawnDummy(ServerPlayer player) {
            if (!(player.level() instanceof ServerLevel level)) {
                return false;
            }
            // Статические константы EntityType (EntityType.ZOMBIE) в этой версии
            // недоступны, поэтому берём тип из реестра — так же, как это делает
            // SimulationManager.spawnTargetDummy.
            EntityType<?> zombie = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("zombie"));
            Entity created = zombie != null ? zombie.create(level, EntitySpawnReason.COMMAND) : null;
            if (!(created instanceof Mob mob)) {
                LOGGER.warn("{} не удалось создать манекен: тип zombie не найден", TAG);
                return false;
            }
            mob.snapTo(player.getX() + 2.0D, player.getY(), player.getZ(), 0F, 0F);
            mob.setNoAi(true);
            mob.setSilent(true);
            mob.setPersistenceRequired();
            // Запас здоровья с большим запасом: моб не должен умереть посреди
            // замера, иначе часть ударов уйдёт в труп и DPS занизится.
            if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
                mob.getAttribute(Attributes.MAX_HEALTH)
                    .setBaseValue(1_000_000D);
            }
            mob.setHealth(mob.getMaxHealth());
            mob.setCustomName(net.minecraft.network.chat.Component.literal("§7[Манекен замера]"));
            level.addFreshEntity(mob);
            dummy = mob;
            return true;
        }

        // ---- лечение

        private void healStep(ServerPlayer player) {
            EntityData data = Load.Unit(player);
            if (healStage == 0) {
                float max = player.getMaxHealth();
                if (max <= 4F) {
                    lines.add(TAG + " heal fatal=max_health_too_small=" + max);
                    failed++;
                    checks++;
                    healReported = true;
                    return;
                }
                player.setHealth(Math.max(1F, max * 0.25F));
                healBefore = player.getHealth();
                float amount = max * 0.5F;
                healExpected = Math.min(max, healBefore + amount);
                // Единственный настоящий путь лечения в моде:
                // ResourcesData.modify(Use.RESTORE, health) -> HealthUtils.heal.
                data.getResources()
                    .restore(player, ResourceType.health, amount);
                healStage = 1;
                return;
            }
            if (healStage == 1) {
                float after = player.getHealth();
                // Перелечивание не должно выводить за максимум.
                data.getResources()
                    .restore(player, ResourceType.health, player.getMaxHealth() * 4F);
                float overheal = player.getHealth();

                Report report = new Report("heal");
                report.put("max", player.getMaxHealth());
                report.put("before", healBefore);
                report.put("after", after);
                report.put("expected", healExpected);
                report.put("overheal", overheal);
                report.check("heal_applied", Math.abs(after - healExpected) < 0.01F);
                report.check("heal_positive", after > healBefore);
                report.check("overheal_clamped", Math.abs(overheal - player.getMaxHealth()) < 0.01F);
                lines.add(report.line());
                checks += report.checks;
                failed += report.failed;

                manaMax = data.getMaximumResource(ResourceType.mana);
                healStage = 2;
                return;
            }
            if (healStage == 2) {
                if (manaMax <= 0F) {
                    healStage = 3;
                    return;
                }
                // Мана: восстанавливаем сверх максимума — cap должен срезать.
                data.getResources()
                    .spend(player, ResourceType.mana, manaMax);
                float spent = data.getResources()
                    .getMana();
                data.getResources()
                    .restore(player, ResourceType.mana, manaMax * 3F);
                float restored = data.getResources()
                    .getMana();

                Report report = new Report("mana");
                report.put("max", manaMax);
                report.put("after_spend", spent);
                report.put("after_restore", restored);
                report.check("spend_works", spent < manaMax);
                report.check("restore_capped", Math.abs(restored - manaMax) < 0.5F);
                lines.add(report.line());
                checks += report.checks;
                failed += report.failed;
                healStage = 3;
                return;
            }
            healReported = true;
        }

        // ---- название команды

        /**
         * Раньше команда создавалась со случайным именем из UUID, и в HUD
         * показывались первые 8 символов. Здесь проверяем, что заданное имя
         * переживает чистку (§-коды, пробелы, длина) и попадает в HUD.
         */
        private void teamStep(ServerPlayer player) {
            TeamData team = Load.playerRPGData(player).team;
            String raw = "§cТихий   авангард";
            team.createTeam(raw);
            String shown = team.displayName();
            String longName = TeamData.sanitizeName("А".repeat(40));

            Report report = new Report("team");
            report.put("raw", raw.replace(' ', '_').replace('§', 'S'));
            report.put("shown", shown.replace(' ', '_'));
            report.put("long_len", longName.length());
            report.put("max_len", TeamData.MAX_NAME_LENGTH);
            report.check("name_kept", shown.equals("Тихий авангард"));
            report.check("no_format_codes", !shown.contains("§"));
            report.check("name_trimmed", longName.length() <= TeamData.MAX_NAME_LENGTH);
            lines.add(report.line());
            checks += report.checks;
            failed += report.failed;
            teamReported = true;
        }
    }

    /** Плоский отчёт: {@code AOETEST <вид> ключ=значение ... check_имя=true}. */
    private static final class Report {
        private final String kind;
        private final List<String> parts = new ArrayList<>();
        private int checks;
        private int failed;

        Report(String kind) {
            this.kind = kind;
        }

        void put(String key, Object value) {
            parts.add(key + "=" + value);
        }

        void check(String name, boolean ok) {
            checks++;
            if (!ok) {
                failed++;
            }
            parts.add("check_" + name + "=" + ok);
        }

        String line() {
            return TAG + " " + kind + " " + String.join(" ", parts);
        }
    }
}
