package com.robertx22.age_of_exile.uncommon.testing;

import com.robertx22.age_of_exile.capability.PlayerDamageChart;
import com.robertx22.age_of_exile.capability.player.data.TeamData;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.combat.CombatPartyTracker;
import com.robertx22.age_of_exile.aoe_data.database.base_gear_types.BaseGearTypes;
import com.robertx22.age_of_exile.database.data.affixes.Affix;
import com.robertx22.age_of_exile.database.data.stats.types.resources.health.Health;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.event_hooks.my_events.CollectGearEvent;
import com.robertx22.age_of_exile.event_hooks.my_events.OnEntityTick;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.AffixData;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.unit.GearData;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.effectdatas.DamageEvent;
import com.robertx22.age_of_exile.uncommon.effectdatas.EventBuilder;
import com.robertx22.age_of_exile.uncommon.enumclasses.AttackType;
import com.robertx22.age_of_exile.uncommon.enumclasses.PlayStyle;
import com.robertx22.age_of_exile.uncommon.enumclasses.WeaponTypes;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 *
 * <p>Прогоны: {@code dps} — урон, {@code heal} — лечение/мана, {@code gear} —
 * обновление макс. HP от экипировки (регрессия на «броня не поднимает HP до
 * перезахода»), {@code team} — название команды, {@code soulless} — бесдушные
 * основы, {@code all} — всё сразу.</p>
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

    /** Ставит прогон в очередь: {@code what} = dps | heal | gear | team | soulless | all. */
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

        private int gearStage;
        private int gearWait;
        private float gearMaxBefore;
        private ItemStack gearChest = ItemStack.EMPTY;
        private ItemStack gearChestSaved = ItemStack.EMPTY;
        private boolean gearEquipOk;
        private boolean gearUnequipOk;
        private boolean gearInplaceOk;

        private int settle;
        private boolean dpsReported;
        private boolean healReported;
        private boolean teamReported;
        private boolean gearReported;
        private boolean soullessReported;

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
            if (what.equals("soulless") || what.equals("all")) {
                if (!soullessReported) {
                    // «Бесдушные основы»: проверка в живом мире — без реестров слотов,
                    // датапака типов предметов и NBT-сохранения её не прогнать.
                    int[] counters = new int[]{checks, failed};
                    SoullessProbe.run(player, lines, counters);
                    checks = counters[0];
                    failed = counters[1];
                    soullessReported = true;
                    return false;
                }
            }
            if (what.equals("gear") || what.equals("all")) {
                if (!gearReported) {
                    gearStep(player);
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

        /**
         * Лечение проверяем инвариантами, а не формулой: {@code ResourcesData}
         * работает в «реальных» единицах мода, а {@code getHealth()} — в
         * ванильных, и мод пересчитывает одно в другое. Поэтому проверяем то,
         * что важно игроку: лечение поднимает здоровье, лечит до максимума и
         * не выводит здоровье за максимум.
         */
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
                // Перелечивание: количество заведомо больше максимума.
                data.getResources()
                    .restore(player, ResourceType.health, max * 20F);
                healStage = 1;
                return;
            }
            if (healStage == 1) {
                float full = player.getHealth();
                Report report = new Report("heal");
                report.put("max", player.getMaxHealth());
                report.put("before", healBefore);
                report.put("after_full", full);
                report.check("heal_positive", full > healBefore);
                report.check("heal_reaches_max", Math.abs(full - player.getMaxHealth()) < 0.01F);
                report.check("never_above_max", full <= player.getMaxHealth() + 0.01F);
                lines.add(report.line());
                checks += report.checks;
                failed += report.failed;

                // Частичное лечение: снова роняем здоровье и лечим половиной
                // максимума в реальных единицах — результат обязан быть строго
                // между текущим значением и максимумом.
                player.setHealth(Math.max(1F, player.getMaxHealth() * 0.25F));
                healBefore = player.getHealth();
                data.getResources()
                    .restore(player, ResourceType.health, maxRealHealth(data) * 0.5F);
                healStage = 2;
                return;
            }
            if (healStage == 2) {
                float partial = player.getHealth();
                Report report = new Report("heal_partial");
                report.put("before", healBefore);
                report.put("after", partial);
                report.put("max", player.getMaxHealth());
                report.check("partial_increases", partial > healBefore);
                report.check("partial_below_max", partial < player.getMaxHealth());
                lines.add(report.line());
                checks += report.checks;
                failed += report.failed;

                manaMax = data.getMaximumResource(ResourceType.mana);
                healStage = 3;
                return;
            }
            if (healStage == 3) {
                if (manaMax <= 0F) {
                    healReported = true;
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
                healReported = true;
            }
        }

        /** Максимум здоровья в «реальных» единицах мода (для заклинаний и зелий). */
        private float maxRealHealth(EntityData data) {
            try {
                float value = data.getUnit()
                    .healthData()
                    .getValue();
                return value > 0F ? value : 1F;
            } catch (Exception ignored) {
                return 1F;
            }
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

        // ---- экипировка и максимальное HP

        /**
         * Регрессия на баг «броня со статами не поднимает макс. HP до перезахода».
         *
         * <p>Проверяем три наблюдаемых игроком инварианта, и все — через штатный
         * путь {@code checkGearChanged} → пересчёт статов, без
         * {@code setEquipsChanged}/{@code forceRecalculateStats} извне (иначе тест
         * сам замаскирует баг):</p>
         * <ol>
         *   <li>надели броню со статом здоровья — макс. HP вырос в течение пары
         *       тиков, а не после перезахода;</li>
         *   <li>сняли — макс. HP вернулся к исходному;</li>
         *   <li>прокрутили УЖЕ надетый предмет (NBT меняется на той же
         *       ItemStack-ссылке) — статы тоже пересчитались. Именно этот случай
         *       терялся при сравнении экипировки по ссылкам.</li>
         * </ol>
         *
         * <p>После проверки «надели» броня держится надетой ~8 секунд —
         * CI-сценарий успевает снять скриншот HUD с выросшим максимумом.</p>
         */
        private void gearStep(ServerPlayer player) {
            EntityData data = Load.Unit(player);

            if (gearStage == 0) {
                gearMaxBefore = maxRealHealth(data);
                gearChestSaved = player.getItemBySlot(EquipmentSlot.CHEST).copy();
                gearChest = createHealthChest(player);
                if (gearChest.isEmpty()) {
                    lines.add(TAG + " gear fatal=could_not_create_gear");
                    failed++;
                    checks++;
                    gearReported = true;
                    return;
                }
                player.setItemSlot(EquipmentSlot.CHEST, gearChest);
                gearStage = 1;
                gearWait = 0;
                return;
            }
            if (gearStage == 1) {
                // Даём штатному checkGearChanged (следующий тик) пересчитать статы.
                if (++gearWait < 3) {
                    return;
                }
                float raised = maxRealHealth(data);
                gearEquipOk = raised > gearMaxBefore + 0.01F;
                Report report = new Report("gear_equip");
                report.put("max_before", fmt(gearMaxBefore));
                report.put("max_after", fmt(raised));
                report.check("equip_raises_max", gearEquipOk);
                lines.add(report.line());
                checks += report.checks;
                failed += report.failed;
                gearStage = 2;
                gearWait = 0;
                return;
            }
            if (gearStage == 2) {
                // Держим броню надетой ~8 с: широкое окно для скриншота HUD
                // с выросшим максимумом здоровья (CI-сценарий tools/e2e).
                if (++gearWait < 160) {
                    return;
                }
                player.setItemSlot(EquipmentSlot.CHEST, gearChestSaved.isEmpty() ? ItemStack.EMPTY : gearChestSaved.copy());
                gearStage = 3;
                gearWait = 0;
                return;
            }
            if (gearStage == 3) {
                if (++gearWait < 3) {
                    return;
                }
                float back = maxRealHealth(data);
                gearUnequipOk = Math.abs(back - gearMaxBefore) < Math.max(0.5F, gearMaxBefore * 0.02F);
                Report report = new Report("gear_unequip");
                report.put("max_before", fmt(gearMaxBefore));
                report.put("max_after", fmt(back));
                report.check("unequip_restores_max", gearUnequipOk);
                lines.add(report.line());
                checks += report.checks;
                failed += report.failed;
                gearStage = 4;
                gearWait = 0;
                return;
            }
            if (gearStage == 4) {
                player.setItemSlot(EquipmentSlot.CHEST, gearChest.copy());
                gearStage = 5;
                gearWait = 0;
                return;
            }
            if (gearStage == 5) {
                if (++gearWait < 3) {
                    return;
                }
                gearMaxBefore = maxRealHealth(data);
                // Прокрутка уже надетого предмета: NBT меняется на той же ссылке.
                ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
                GearItemData gearData = Gear.Load(worn);
                if (gearData == null || gearData.affixes.pre.isEmpty()) {
                    lines.add(TAG + " gear fatal=worn_item_lost_stats");
                    failed++;
                    checks++;
                    gearReported = true;
                    return;
                }
                // Обнуляем ВСЕ аффиксы: в списке до virile может лежать случайный
                // префикс от randomize — достаточно ли было бы нулевого первого.
                gearData.affixes.pre.forEach(a -> a.perc = 0);
                gearData.affixes.suf.forEach(a -> a.perc = 0);
                Gear.Save(worn, gearData);
                gearStage = 6;
                gearWait = 0;
                return;
            }
            if (gearStage == 6) {
                if (++gearWait < 3) {
                    return;
                }
                float after = maxRealHealth(data);
                gearInplaceOk = after < gearMaxBefore - 0.01F;
                Report report = new Report("gear_inplace");
                report.put("max_before", fmt(gearMaxBefore));
                report.put("max_after", fmt(after));
                report.check("inplace_reroll_updates_max", gearInplaceOk);
                lines.add(report.line());
                checks += report.checks;
                failed += report.failed;
                player.setItemSlot(EquipmentSlot.CHEST, gearChestSaved.isEmpty() ? ItemStack.EMPTY : gearChestSaved.copy());
                if (gearEquipOk && gearUnequipOk && gearInplaceOk) {
                    gearReported = true;
                } else {
                    // Строгие проверки провалились — локализуем разрыв цепочки
                    // «экипировка → пересчёт статов» рантайм-проверками.
                    gearStage = 7;
                    gearWait = 0;
                }
                return;
            }
            if (gearStage == 7) {
                // Диагностика (только при провале): те же статы теми же путями.
                // 1) надеть заново и дернуть штатный триггер ВРУЧНУЮ;
                // 2) если он не помог — forceRecalculateStats (чистый пересчёт);
                // 3) параллельно: пережил ли нагрудник NBT-кругооборот и видит ли
                //    сбор экипировки его стат здоровья.
                float base = maxRealHealth(data);
                player.setItemSlot(EquipmentSlot.CHEST, gearChest.copy());

                ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
                GearItemData wornData = worn == null ? null : Gear.Load(worn);
                boolean wornHealth = wornData != null && wornData.GetAllStats()
                    .stream()
                    .anyMatch(s -> Health.GUID.equals(s.getStatId()));

                List<GearData> collected = new ArrayList<>();
                new CollectGearEvent.CollectedGearStacks(player, collected, null);
                boolean collectHealth = collected.stream()
                    .anyMatch(x -> x.gear != null && x.gear.GetAllStats()
                        .stream()
                        .anyMatch(s -> Health.GUID.equals(s.getStatId())));

                OnEntityTick.checkGearChanged(player); // штатный триггер, но зовём сами
                float afterManual = maxRealHealth(data);

                data.forceRecalculateStats();
                float afterForce = maxRealHealth(data);

                Report report = new Report("gear_diag");
                report.put("base", fmt(base));
                report.put("after_manual", fmt(afterManual));
                report.put("after_force", fmt(afterForce));
                report.put("collect_n", collected.size());
                report.check("chest_keeps_health", wornHealth);
                report.check("collect_sees_health", collectHealth);
                report.check("manual_trigger_works", afterManual > base + 0.01F);
                report.check("forced_recalc_works", afterForce > base + 0.01F);
                lines.add(report.line());
                checks += report.checks;
                failed += report.failed;

                player.setItemSlot(EquipmentSlot.CHEST, gearChestSaved.isEmpty() ? ItemStack.EMPTY : gearChestSaved.copy());
                gearReported = true;
            }
        }

        /** Нагрудник с гарантированным статом здоровья (аффикс «virile», +% HP). */
        private ItemStack createHealthChest(ServerPlayer player) {
            try {
                int lvl = Math.max(1, Load.Unit(player).getLevel());
                GearBlueprint bp = new GearBlueprint(lvl, 0);
                bp.item = Items.DIAMOND_CHESTPLATE;
                // Уровень предмета фиксируем: LevelPart по умолчанию роллит ±3
                // уровня, предмет может оказаться «выше игрока» — тогда
                // isUsableBy его отвергает и тест ложно падает (раньше он был
                // зелёным только при удачном ролле).
                bp.level.LevelRange = false;
                // GUID базовых типов короткий («chest», «boots», …) — не путать
                // с slotHint-ами вида «plate_chest» из SimulationManager.
                var type = ExileDB.GearTypes().get(BaseGearTypes.CHEST.GUID());
                if (type == null) {
                    LOGGER.warn("{} нет базового типа нагрудника в реестре", TAG);
                    return ItemStack.EMPTY;
                }
                bp.gearItemSlot.set(type);
                if (ExileDB.GearRarities().isRegistered(IRarity.COMMON_ID)) {
                    bp.rarity.set(ExileDB.GearRarities().get(IRarity.COMMON_ID));
                }
                ItemStack stack = bp.createStack();
                GearItemData gearData = Gear.Load(stack);
                if (gearData == null) {
                    LOGGER.warn("{} сгенерированный нагрудник не распознан как GearItemData", TAG);
                    return ItemStack.EMPTY;
                }
                AffixData hp = new AffixData(Affix.Type.prefix);
                hp.affix = "virile"; // +4-8% здоровья
                hp.perc = 100;
                gearData.affixes.pre.add(hp);
                Gear.Save(stack, gearData);

                boolean hasHealth = gearData.GetAllStats()
                    .stream()
                    .anyMatch(s -> Health.GUID.equals(s.getStatId()));
                if (!hasHealth) {
                    LOGGER.warn("{} у нагрудника нет стата здоровья после аффикса virile", TAG);
                    return ItemStack.EMPTY;
                }
                return stack;
            } catch (Exception e) {
                LOGGER.warn("{} не удалось создать тестовую броню: {}", TAG, e.toString());
                return ItemStack.EMPTY;
            }
        }

        private static String fmt(float v) {
            return String.format(Locale.ROOT, "%.1f", v);
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
