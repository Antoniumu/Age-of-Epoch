package com.robertx22.age_of_exile.uncommon.testing;

import com.robertx22.age_of_exile.aoe_data.database.spells.SpellDesc;
import com.robertx22.age_of_exile.capability.PlayerDamageChart;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.database.data.spells.components.ComponentPart;
import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.spells.components.actions.SpellAction;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.saveclasses.spells.SpellCastingData;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Проверяет КАЖДОЕ заклинание в живом мире: применяет его настоящим путём
 * (тот же {@code setToCast} → тик каста → {@code Spell.cast}, что и у хотбара) и
 * смотрит, что реально произошло.
 *
 * <p>Зачем: структурная проверка ({@code tools/verify/check_spells.py}) видит
 * только то, что написано в билдере. Она не поймает заклинание, у которого
 * партиклы объявлены, но не срабатывают, или урон объявлен, а
 * {@code DamageAction} падает с исключением. Здесь — замер по факту.
 *
 * <p>Что измеряется на каждое заклинание:
 * <ul>
 *   <li>урон по манекенам (сумма падений здоровья + счётчик попаданий);</li>
 *   <li>лечение и щит игрока, восстановление маны, выданные предметы;</li>
 *   <li>эффекты: exile-эффекты мода, наложенные на манекенов и на игрока;</li>
 *   <li>перемещение игрока (рывки, телепорты) и появление сущностей;</li>
 *   <li>структура: объявлены ли урон/партиклы/звук, есть ли описание и
 *       входит ли заклинание в школу.</li>
 * </ul>
 *
 * <p>Если урон объявлен, но ни один манекен не пострадал, делается второй заход
 * с кольцом манекенов вокруг игрока: снаряд мог уйти выше/в сторону, а ловушка
 * требует, чтобы враг подошёл. Так «не попал» не путается с «не работает».
 *
 * <p>Запуск: {@code /aoe spellaudit all|school=fire|spells=fireball,meteor}
 * либо флаг-файл {@code aoe-spell-audit.flag} с тем же содержимым (его кладёт
 * CI-сценарий). Результат — строки {@code AOEAI ...} в логе, отчёты
 * {@code spell-audit.md}/{@code spell-audit.json} и таймлайн
 * {@code spell-audit-timeline.txt} для нарезки видео по классам.
 */
public final class SpellAuditProbe {

    public static final String TAG = "AOEAI";
    /** Флаг-файл: есть — прогон стартует сам (CI, без чата и без прав оператора). */
    public static final String FLAG_FILE = "aoe-spell-audit.flag";
    private static final String TIMELINE_FILE = "spell-audit-timeline.txt";
    private static final String REPORT_MD = "spell-audit.md";
    private static final String REPORT_JSON = "spell-audit.json";
    private static final String REPORT_LOG = "spell-audit.log";

    private static final Logger LOGGER = LoggerFactory.getLogger("AgeOfEpoch-SpellAudit");

    /** Порядок классов в прогоне и в видео. */
    private static final List<String> SCHOOL_ORDER = List.of("divine", "hunting", "nature", "fire", "water");
    /** Группа для заклинаний вне школ: их тоже проверяем, это ловит «мёртвый» контент. */
    private static final String NOBODY = "no_school";
    /** Служебные заклинания: не обязаны быть в школе и не нужны в видео. */
    private static final Set<String> SERVICE = Set.of("test_command", "test_spell", "demon");

    /** Площадка: ровный пол из кварца в небе — чтобы видео было чистым. */
    private static final int PLATFORM_Y = 99;
    private static final int PLATFORM_RADIUS = 11;
    /** Расстояния манекенов от игрока в первом заходе. */
    private static final double[] DUMMY_DISTANCES = {1.5D, 3D, 5D, 7D};

    private static Runner active;

    private SpellAuditProbe() {
    }

    // ------------------------------------------------------------------ запуск

    /** {@code what} = all | school=fire | spells=fireball,meteor. */
    public static void request(String what) {
        if (active != null) {
            LOGGER.info("{} пропуск: прогон уже идёт", TAG);
            return;
        }
        active = new Runner(what == null ? "all" : what.trim());
        LOGGER.info("{} старт прогона what={}", TAG, what);
    }

    /** Вызывается каждый тик сервера. */
    public static void tick(MinecraftServer server) {
        if (active == null) {
            String flag = readFlag();
            if (flag != null) {
                request(flag);
            }
        }
        if (active == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
        if (player == null) {
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
            writeLog(TAG + " fatal=" + error);
            active = null;
            clearFlag();
            SpellAuditState.end();
        }
    }

    // ------------------------------------------------------------------ файлы

    private static Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    private static String readFlag() {
        try {
            Path path = gameDir().resolve(FLAG_FILE);
            if (!Files.exists(path)) {
                return null;
            }
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
            return text.isEmpty() ? "all" : text;
        } catch (IOException error) {
            return null;
        }
    }

    private static void clearFlag() {
        try {
            Files.deleteIfExists(gameDir().resolve(FLAG_FILE));
        } catch (IOException error) {
            LOGGER.warn("{} не удалось удалить флаг: {}", TAG, error.toString());
        }
    }

    private static void writeLog(String line) {
        LOGGER.info(line);
        try {
            Files.write(gameDir().resolve(REPORT_LOG),
                (line + "\n").getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    /** Маркер таймлайна: по нему workflow режет видео на классы. */
    private static void marker(String kind, String name) {
        long now = System.currentTimeMillis();
        String line = String.format(Locale.ROOT, "AOEAI_MARK %s %s %d", kind, name, now);
        LOGGER.info(line);
        try {
            Files.write(gameDir().resolve(TIMELINE_FILE),
                (line + "\n").getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    // ------------------------------------------------------------------ прогон

    private enum Phase {
        SETUP,
        WARMUP,
        PREPARE,
        CAST,
        OBSERVE,
        JUDGE,
        CLEANUP,
        DONE
    }

    private static final class Group {
        final String name;
        final String title;
        final List<String> spells = new ArrayList<>();

        Group(String name, String title) {
            this.name = name;
            this.title = title;
        }
    }

    private static final class SpellResult {
        String id;
        String school;
        String name;
        String description;
        boolean damage;
        boolean heal;
        boolean mana;
        boolean shield;
        boolean effect;
        boolean move;
        boolean item;
        boolean particles;
        boolean sound;
        boolean summon;
        boolean canCast;
        int castTicks;
        float damageDealt;
        int hits;
        float healed;
        float manaGained;
        float shieldGained;
        int itemsGained;
        final Set<String> effects = new LinkedHashSet<>();
        float moved;
        int spawned;
        final List<String> problems = new ArrayList<>();
        final List<String> notes = new ArrayList<>();
        boolean pass = true;

        String verdict() {
            return pass ? "PASS" : "FAIL";
        }

        void problem(String text) {
            problems.add(text);
            pass = false;
        }

        void note(String text) {
            notes.add(text);
        }
    }

    private static final class Runner {
        private final String what;
        private final List<Group> groups = new ArrayList<>();
        private final List<SpellResult> results = new ArrayList<>();
        private final Set<UUID> dummies = new HashSet<>();
        private final Map<UUID, Float> lastHealth = new LinkedHashMap<>();

        private int groupIndex;
        private int spellIndex;
        private Phase phase = Phase.SETUP;
        private int phaseTicks;
        private int maxObserve = 60;
        private boolean platformDone;

        private Spell spell;
        private SpellResult current;
        private float playerHealthAtCast;
        private float manaAtCast;
        private float shieldAtCast;
        private int itemsAtCast;
        private Set<String> playerEffectsAtCast = Set.of();
        private Set<String> playerVanillaAtCast = Set.of();
        private Set<String> dummyEffectsAtCast = Set.of();
        private Set<String> dummyVanillaAtCast = Set.of();
        private double startX;
        private double startY;
        private double startZ;
        private boolean secondAttempt;

        Runner(String what) {
            this.what = what;
            buildGroups();
        }

        // ---- построение списка

        private void buildGroups() {
            Map<String, Group> bySchool = new LinkedHashMap<>();
            for (String school : SCHOOL_ORDER) {
                bySchool.put(school, new Group(school, schoolTitle(school)));
            }

            Map<String, List<String>> members = new LinkedHashMap<>();
            for (String school : SCHOOL_ORDER) {
                members.put(school, new ArrayList<>());
            }

            for (SpellSchool school : ExileDB.SpellSchools().getList()) {
                String id = school.id == null ? "" : school.id;
                Group group = bySchool.get(id);
                if (group == null || school.spells == null) {
                    continue;
                }
                List<String> sorted = new ArrayList<>(school.spells.entrySet().stream()
                    .sorted(Comparator
                        .comparingInt((Map.Entry<String, PointData> e) -> e.getValue().y)
                        .thenComparingInt(e -> e.getValue().x))
                    .map(Map.Entry::getKey)
                    .toList());
                members.get(id).addAll(sorted);
            }

            List<String> orphans = new ArrayList<>();
            for (Spell known : ExileDB.Spells().getList()) {
                if (known == null) {
                    continue;
                }
                String id = known.GUID();
                if (SERVICE.contains(id)) {
                    continue;
                }
                boolean inSchool = members.values().stream().anyMatch(list -> list.contains(id));
                if (!inSchool) {
                    orphans.add(id);
                }
            }
            orphans.sort(String::compareTo);

            if (what.startsWith("school=")) {
                String only = what.substring("school=".length()).trim();
                if (only.equals(NOBODY)) {
                    Group group = new Group(NOBODY, schoolTitle(NOBODY));
                    group.spells.addAll(orphans);
                    groups.add(group);
                } else {
                    Group group = bySchool.get(only);
                    if (group != null) {
                        group.spells.addAll(members.getOrDefault(only, List.of()));
                        groups.add(group);
                    }
                }
            } else if (what.startsWith("spells=")) {
                Group group = new Group("custom", "Выбранные заклинания");
                for (String id : what.substring("spells=".length()).split(",")) {
                    String trimmed = id.trim();
                    if (!trimmed.isEmpty() && ExileDB.Spells().isRegistered(trimmed)) {
                        group.spells.add(trimmed);
                    }
                }
                groups.add(group);
            } else {
                for (String school : SCHOOL_ORDER) {
                    Group group = bySchool.get(school);
                    group.spells.addAll(members.getOrDefault(school, List.of()));
                    groups.add(group);
                }
                Group nobody = new Group(NOBODY, schoolTitle(NOBODY));
                nobody.spells.addAll(orphans);
                groups.add(nobody);
            }

            groups.removeIf(g -> g.spells.isEmpty());
        }

        private static String schoolTitle(String id) {
            return switch (id) {
                case "divine" -> "Divine — свет, щиты и ближний бой";
                case "hunting" -> "Hunting — луки, ловушки и мобильность";
                case "nature" -> "Nature — яды, тотемы и поддержка";
                case "fire" -> "Fire — огонь и урон по площади";
                case "water" -> "Water — лёд, контроль и лечение";
                default -> "Вне школ — мёртвый контент";
            };
        }

        // ---- шаг машины состояний

        boolean step(ServerPlayer player) {
            if (!(player.level() instanceof ServerLevel level)) {
                return false;
            }
            phaseTicks++;
            switch (phase) {
                case SETUP -> {
                    buildPlatform(level, player);
                    enterCreative(player);
                    marker("audit_start", sanitize(what));
                    writeLog(TAG + " start what=" + sanitize(what) + " groups=" + groups.size());
                    // Пауза на старте: запись видео (ffmpeg/x11grab) должна успеть
                    // начать писать, иначе первое умение в кадр не попадёт.
                    SpellAuditState.begin("разминка", 0, groups.size());
                    SpellAuditState.spell("Запись видео и подготовка площадки",
                        "Сейчас начнётся проверка всех умений: урон, лечение, эффекты и партиклы.");
                    phase = Phase.WARMUP;
                    phaseTicks = 0;
                }
                case WARMUP -> {
                    if (phaseTicks > 70) {
                        phase = Phase.PREPARE;
                        phaseTicks = 0;
                    }
                }
                case PREPARE -> {
                    if (!prepareNext(player)) {
                        return false;
                    }
                    if (phase == Phase.DONE) {
                        return finish(player);
                    }
                    resetPlayer(player, level);
                    spawnDummies(player, level);
                    snapshot(player, level);
                    SpellAuditState.setPhase("ПОДГОТОВКА");
                    phase = Phase.CAST;
                    phaseTicks = 0;
                }
                case CAST -> {
                    doCast(player);
                    phase = Phase.OBSERVE;
                    phaseTicks = 0;
                }
                case OBSERVE -> {
                    if (observe(player, level)) {
                        phase = Phase.JUDGE;
                        phaseTicks = 0;
                    }
                }
                case JUDGE -> {
                    boolean retry = judge(level, player);
                    phase = retry ? Phase.CAST : Phase.CLEANUP;
                    phaseTicks = 0;
                }
                case CLEANUP -> {
                    cleanup(level, player);
                    phase = Phase.PREPARE;
                    phaseTicks = 0;
                }
                case DONE -> {
                    return finish(player);
                }
            }
            return false;
        }

        /** @return false — ждём следующего тика */
        private boolean prepareNext(ServerPlayer player) {
            while (true) {
                if (groupIndex >= groups.size()) {
                    phase = Phase.DONE;
                    return true;
                }
                Group group = groups.get(groupIndex);
                if (spellIndex >= group.spells.size()) {
                    marker("school_end", group.name);
                    writeLog(TAG + " school_end " + group.name);
                    groupIndex++;
                    spellIndex = 0;
                    continue;
                }
                String id = group.spells.get(spellIndex);
                Spell next = ExileDB.Spells().isRegistered(id) ? ExileDB.Spells().get(id) : null;
                if (next == null) {
                    writeLog(TAG + " spell=" + id + " verdict=FAIL problem=not_registered");
                    spellIndex++;
                    continue;
                }
                if (spellIndex == 0) {
                    marker("school_start", group.name);
                    writeLog(TAG + " school_start " + group.name + " spells=" + group.spells.size());
                }
                marker("spell_start", id);
                spell = next;
                secondAttempt = false;
                current = new SpellResult();
                current.id = id;
                current.school = group.name;
                current.name = safeName(spell);
                current.description = describe(player, spell);
                annotate(current, spell);
                SpellAuditState.begin(group.title, spellIndex + 1, group.spells.size());
                SpellAuditState.spell(current.name, current.description);
                return true;
            }
        }

        private void annotate(SpellResult result, Spell spell) {
            try {
                for (ComponentPart part : spell.getAttached().getAllComponents()) {
                    if (part == null || part.acts == null) {
                        continue;
                    }
                    for (MapHolder act : part.acts) {
                        if (act == null || act.type == null) {
                            continue;
                        }
                        String type = act.type;
                        if (matches(type, SpellAction.DEAL_DAMAGE)) {
                            result.damage = true;
                        } else if (matches(type, SpellAction.RESTORE_HEALTH)) {
                            result.heal = true;
                        } else if (matches(type, SpellAction.RESTORE_MANA)) {
                            result.mana = true;
                        } else if (matches(type, SpellAction.GIVE_SHIELD)) {
                            result.shield = true;
                            result.effect = true;
                        } else if (matches(type, SpellAction.EXILE_EFFECT)
                            || matches(type, SpellAction.POTION)) {
                            result.effect = true;
                        } else if (matches(type, SpellAction.SET_ADD_MOTION)
                            || matches(type, SpellAction.PUSH)
                            || matches(type, SpellAction.TP_CASTER_IN_DIRECTION)
                            || matches(type, SpellAction.TP_TARGET_TO_SELF)) {
                            result.move = true;
                        } else if (matches(type, SpellAction.GIVE_ITEM)) {
                            result.item = true;
                        } else if (matches(type, SpellAction.AGGRO)
                            || matches(type, SpellAction.OPEN_ENDER_CHEST)
                            || matches(type, SpellAction.REFRESH_COOLDOWNS_BY_X_TICKS)) {
                            // Аггро, сундук Края и сброс кулдаунов не двигают
                            // цифры, но это ровно то, ради чего умение берут.
                            result.effect = true;
                        } else if (matches(type, SpellAction.PARTICLES_IN_RADIUS)
                            || matches(type, SpellAction.SWORD_SWEEP_PARTICLES)) {
                            result.particles = true;
                        } else if (matches(type, SpellAction.PLAY_SOUND)
                            || matches(type, SpellAction.PLAY_SOUND_PER_TARGET)) {
                            result.sound = true;
                        } else if (matches(type, SpellAction.SUMMON_BLOCK)
                            || matches(type, SpellAction.SUMMON_PROJECTILE)
                            || matches(type, SpellAction.SUMMON_AT_SIGHT)
                            || matches(type, SpellAction.SUMMON_AT_FEET)
                            || matches(type, SpellAction.SUMMON_EVOKER_FANG_AT_TARGET)
                            || matches(type, SpellAction.SUMMON_LIGHTNING_STRIKE)) {
                            result.summon = true;
                        }
                    }
                }
            } catch (Throwable error) {
                result.note("не удалось разобрать компоненты: " + error);
            }

            if (result.description == null || result.description.isBlank()) {
                result.problem("нет описания (в тултипе пусто)");
            }
            if (!result.particles) {
                result.problem("нет партиклов");
            }
            if (!result.sound) {
                result.note("нет звука");
            }
            if (!result.damage && !result.heal && !result.mana && !result.effect
                && !result.move && !result.item && !result.summon) {
                result.problem("ничего не делает (нет урона/лечения/эффекта/движения)");
            }
        }

        private boolean matches(String type, SpellAction action) {
            try {
                return type.equals(action.GUID());
            } catch (Throwable error) {
                return false;
            }
        }

        // ---- площадка и манекены

        private void buildPlatform(ServerLevel level, ServerPlayer player) {
            BlockPos center = new BlockPos(0, PLATFORM_Y, 0);
            int radius = PLATFORM_RADIUS;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    level.setBlock(center.offset(x, 0, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                }
            }
            for (int x = -radius; x <= radius; x += 4) {
                for (int z = -radius; z <= radius; z += 4) {
                    level.setBlock(center.offset(x, 4, z), Blocks.GLASS.defaultBlockState(), 2);
                }
            }
            teleport(player, level, 0.5D, PLATFORM_Y + 1, 0.5D);
            player.setYRot(0F);
            player.setXRot(0F);
            writeLog(TAG + " platform ready at y=" + (PLATFORM_Y + 1));
        }

        /**
         * На время прогона игрок — в креативе, и это не «читерство ради зелёного
         * отчёта»: {@code SpellCastingData.canCast} пускает креатив дальше
         * проверок оружия и маны. Без этого из 74 умений применялась едва
         * четверть — остальные требуют посоха/лука/меча и полной маны, то есть
         * замерялось не «работает ли умение», а «есть ли у бота нужный меч».
         * Урон, лечение и эффекты при этом считаются настоящие — их делает само
         * заклинание.
         */
        private void enterCreative(ServerPlayer player) {
            try {
                player.setGameMode(GameType.CREATIVE);
                writeLog(TAG + " режим: creative (иначе требования оружия и маны не дают применить умение)");
            } catch (Throwable error) {
                writeLog(TAG + " creative не включился: " + error);
            }
        }

        private void restoreGameMode(ServerPlayer player) {
            try {
                player.setGameMode(GameType.SURVIVAL);
            } catch (Throwable ignored) {
            }
        }

        private void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z) {
            try {
                player.teleportTo(level, x, y, z, Set.of(), player.getYRot(), player.getXRot(), false);
            } catch (Throwable error) {
                player.snapTo(x, y, z, player.getYRot(), player.getXRot());
            }
        }

        private void resetPlayer(ServerPlayer player, ServerLevel level) {
            try {
                Load.spells(player).getCastingData().cancelCast(player);
            } catch (Throwable ignored) {
            }
            // conjure_ender_chest (и любой будущий «открыть меню») открывает
            // клиенту экран: он заслонил бы подпись и все следующие умения в
            // видео, поэтому закрываем его перед каждым замером.
            try {
                player.closeContainer();
            } catch (Throwable ignored) {
            }
            // Здоровье — не полное: иначе лечение нечего измерять (лечить
            // полного игрока бессмысленно, и заклинание честно даёт ноль).
            player.setHealth(Math.max(1F, player.getMaxHealth() * 0.6F));
            player.setDeltaMovement(0, 0, 0);
            player.setRemainingFireTicks(0);
            player.clearFire();
            teleport(player, level, 0.5D, PLATFORM_Y + 1, 0.5D);
            player.setYRot(0F);
            player.setXRot(0F);
            try {
                EntityData data = Load.Unit(player);
                // Ману и щит обнуляем: восстановление маны и выдача щита видны
                // только «от нуля». В креативе стоимость каста всё равно не
                // списывается, поэтому пустой пул ничего не ломает.
                float mana = data.getResources().getMana();
                if (mana > 0) {
                    data.getResources().spend(player, ResourceType.mana, mana);
                }
                data.getResources().shields.shields.clear();
                if (data.getCooldowns() != null) {
                    data.getCooldowns().onTicksPass(1_000_000);
                }
                Load.spells(player).getCastingData().charges.onTicks(player, 1_000_000);
            } catch (Throwable error) {
                LOGGER.warn("{} не удалось сбросить ресурсы: {}", TAG, error.toString());
            }
        }

        private void spawnDummies(ServerPlayer player, ServerLevel level) {
            clearDummies(level);
            double yaw = Math.toRadians(player.getYRot());
            double dirX = -Math.sin(yaw);
            double dirZ = Math.cos(yaw);
            for (double distance : DUMMY_DISTANCES) {
                spawnDummy(level, player, player.getX() + dirX * distance, player.getZ() + dirZ * distance,
                    distance < 2 ? "§7[Манекен]" : "§7[Манекен " + (int) distance + "]");
            }
        }

        private void spawnDummyRing(ServerPlayer player, ServerLevel level, double radius) {
            clearDummies(level);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2D * i / 8D;
                spawnDummy(level, player, player.getX() + Math.cos(angle) * radius,
                    player.getZ() + Math.sin(angle) * radius, "§7[Манекен]");
            }
            spawnDummy(level, player, player.getX(), player.getZ() + 1.0D, "§7[Манекен]");
        }

        private void spawnDummy(ServerLevel level, ServerPlayer player, double x, double z, String name) {
            try {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("husk"));
                if (type == null) {
                    type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("zombie"));
                }
                Entity created = type != null ? type.create(level, EntitySpawnReason.COMMAND) : null;
                if (!(created instanceof Mob mob)) {
                    return;
                }
                mob.snapTo(x, player.getY(), z, 180F, 0F);
                mob.setNoAi(true);
                mob.setSilent(true);
                mob.setPersistenceRequired();
                if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
                    mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10_000_000D);
                }
                mob.setHealth(mob.getMaxHealth());
                mob.setCustomName(Component.literal(name));
                mob.setCustomNameVisible(true);
                level.addFreshEntity(mob);
                dummies.add(mob.getUUID());
                lastHealth.put(mob.getUUID(), mob.getHealth());
            } catch (Throwable error) {
                LOGGER.warn("{} манекен не создан: {}", TAG, error.toString());
            }
        }

        private void clearDummies(ServerLevel level) {
            for (UUID id : new ArrayList<>(dummies)) {
                Entity entity = level.getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
            }
            dummies.clear();
            lastHealth.clear();
        }

        // ---- каст и замер

        private void snapshot(ServerPlayer player, ServerLevel level) {
            current.canCast = false;
            try {
                current.canCast = Load.spells(player).getCastingData().canCast(spell, player);
            } catch (Throwable error) {
                current.note("canCast упал: " + error);
            }
            if (!current.canCast) {
                try {
                    current.note("предпроверка: "
                        + spell.getConfig().castingWeapon.predicate.text.getString());
                } catch (Throwable ignored) {
                }
            }
            playerHealthAtCast = player.getHealth();
            startX = player.getX();
            startY = player.getY();
            startZ = player.getZ();
            try {
                EntityData data = Load.Unit(player);
                manaAtCast = data.getResources().getMana();
                shieldAtCast = data.getResources().getShield();
                playerEffectsAtCast = new LinkedHashSet<>(data.getStatusEffectsData().exileMap.keySet());
            } catch (Throwable ignored) {
            }
            playerVanillaAtCast = vanillaEffects(player);
            Set<String> dummyEffects = new LinkedHashSet<>();
            Set<String> dummyVanilla = new LinkedHashSet<>();
            for (UUID id : dummies) {
                Entity entity = level.getEntity(id);
                if (entity instanceof LivingEntity living) {
                    dummyVanilla.addAll(vanillaEffects(living));
                    try {
                        dummyEffects.addAll(Load.Unit(living).getStatusEffectsData().exileMap.keySet());
                    } catch (Throwable ignored) {
                    }
                }
            }
            dummyEffectsAtCast = dummyEffects;
            dummyVanillaAtCast = dummyVanilla;
            itemsAtCast = countItems(player);
            SpellAuditState.setMetrics("");
        }

        private void doCast(ServerPlayer player) {
            try {
                SpellCastingData data = Load.spells(player).getCastingData();
                data.setToCast(spell, player);
                current.castTicks = Math.max(0, data.castingTicksLeft);
            } catch (Throwable error) {
                current.problem("каст упал с ошибкой: " + error);
            }
            maxObserve = Math.min(220, Math.max(45, current.castTicks + 45));
            SpellAuditState.setPhase("ПРИМЕНЕНИЕ");
            writeLog(TAG + " cast spell=" + current.id + " can_cast=" + current.canCast
                + " cast_ticks=" + current.castTicks);
        }

        /** @return true — наблюдение закончено */
        private boolean observe(ServerPlayer player, ServerLevel level) {
            for (Map.Entry<UUID, Float> entry : lastHealth.entrySet()) {
                Entity entity = level.getEntity(entry.getKey());
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                float now = living.getHealth();
                float before = entry.getValue();
                if (now < before - 0.001F) {
                    current.damageDealt += before - now;
                    current.hits++;
                }
                entry.setValue(now);
            }
            current.healed = Math.max(current.healed, player.getHealth() - playerHealthAtCast);
            try {
                EntityData data = Load.Unit(player);
                current.manaGained = Math.max(current.manaGained, data.getResources().getMana() - manaAtCast);
                current.shieldGained = Math.max(current.shieldGained,
                    data.getResources().getShield() - shieldAtCast);
                for (String effect : data.getStatusEffectsData().exileMap.keySet()) {
                    if (!playerEffectsAtCast.contains(effect)) {
                        current.effects.add(effect);
                    }
                }
                for (String effect : vanillaEffects(player)) {
                    if (!playerVanillaAtCast.contains(effect)) {
                        current.effects.add(effect);
                    }
                }
            } catch (Throwable ignored) {
            }
            for (UUID id : dummies) {
                Entity entity = level.getEntity(id);
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                try {
                    for (String effect : Load.Unit(living).getStatusEffectsData().exileMap.keySet()) {
                        if (!dummyEffectsAtCast.contains(effect)) {
                            current.effects.add(effect);
                        }
                    }
                } catch (Throwable ignored) {
                }
                for (String effect : vanillaEffects(living)) {
                    if (!dummyVanillaAtCast.contains(effect)) {
                        current.effects.add(effect);
                    }
                }
            }
            double dx = player.getX() - startX;
            double dy = player.getY() - startY;
            double dz = player.getZ() - startZ;
            current.moved = (float) Math.max(current.moved, Math.sqrt(dx * dx + dy * dy + dz * dz));
            current.itemsGained = Math.max(current.itemsGained, countItems(player) - itemsAtCast);
            current.spawned = Math.max(current.spawned, countSpawned(level, player));
            SpellAuditState.setMetrics(metricsLine());
            SpellAuditState.setPhase(current.castTicks > 0 && phaseTicks <= current.castTicks
                ? "ПРИМЕНЕНИЕ" : "НАБЛЮДЕНИЕ");

            boolean castDone = current.castTicks == 0 || phaseTicks > current.castTicks + 8;
            boolean worthWaiting = current.damageDealt > 0 || current.healed > 0
                || !current.effects.isEmpty() || current.shieldGained > 0 || current.itemsGained > 0
                || current.spawned > 0;
            if (phaseTicks >= maxObserve) {
                return true;
            }
            return castDone && worthWaiting && phaseTicks > 30;
        }

        /** Имена ванильных эффектов (без префикса effect.minecraft.). */
        private Set<String> vanillaEffects(LivingEntity entity) {
            Set<String> names = new LinkedHashSet<>();
            for (MobEffectInstance instance : entity.getActiveEffects()) {
                try {
                    Identifier key = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
                    if (key != null) {
                        names.add(key.getPath());
                    }
                } catch (Throwable ignored) {
                }
            }
            return names;
        }

        private int countSpawned(ServerLevel level, ServerPlayer player) {
            AABB box = new AABB(player.getX() - 14, player.getY() - 3, player.getZ() - 14,
                player.getX() + 14, player.getY() + 12, player.getZ() + 14);
            int count = 0;
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
                if (dummies.contains(entity.getUUID())) {
                    continue;
                }
                count++;
            }
            return count;
        }

        private int countItems(ServerPlayer player) {
            try {
                int total = 0;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    total += player.getInventory().getItem(i).getCount();
                }
                return total;
            } catch (Throwable error) {
                return 0;
            }
        }

        private String metricsLine() {
            StringBuilder text = new StringBuilder();
            text.append("урон: ").append(fmt(current.damageDealt)).append(" · попаданий: ").append(current.hits);
            if (current.healed > 0) {
                text.append(" · лечение: +").append(fmt(current.healed));
            }
            if (current.manaGained > 0.5F) {
                text.append(" · мана: +").append(fmt(current.manaGained));
            }
            if (current.shieldGained > 0.5F) {
                text.append(" · щит: +").append(fmt(current.shieldGained));
            }
            if (!current.effects.isEmpty()) {
                text.append(" · эффекты: ").append(String.join(", ", current.effects));
            }
            if (current.moved > 0.6F) {
                text.append(" · смещение: ").append(fmt(current.moved));
            }
            if (current.itemsGained > 0) {
                text.append(" · предметов: +").append(current.itemsGained);
            }
            return text.toString();
        }

        private static String fmt(float value) {
            return String.format(Locale.ROOT, "%.1f", value);
        }

        // ---- приговор

        /** @return true — нужен второй заход по этому же заклинанию */
        private boolean judge(ServerLevel level, ServerPlayer player) {
            if (current.damage && current.damageDealt <= 0.1F && !secondAttempt) {
                secondAttempt = true;
                writeLog(TAG + " retry spell=" + current.id + " с кольцом манекенов");
                spawnDummyRing(player, level, 2.5D);
                snapshot(player, level);
                phaseTicks = 0;
                return true;
            }
            if (!current.canCast) {
                current.problem("каст невозможен (canCast=false: требования оружия/маны/уровня)");
            }
            if (current.damage && current.damageDealt <= 0.1F) {
                current.problem("урон не нанесён (нет попаданий)");
            }
            if (current.heal && current.healed <= 0.5F) {
                current.problem("лечение не сработало");
            }
            if (current.mana && current.manaGained <= 0.5F) {
                current.problem("мана не восстановилась");
            }
            if (current.shield && current.shieldGained <= 0.5F) {
                current.problem("щит не выдан");
            }
            if (current.item && current.itemsGained <= 0) {
                current.problem("предмет не выдан");
            }
            if (current.effect && current.effects.isEmpty()) {
                current.problem("эффекты не наложены");
            }
            if (current.move && current.moved <= 0.3F) {
                current.note("перемещение не замечено");
            }
            if (current.spawned == 0 && !current.particles) {
                current.note("в мире ничего не появилось");
            }

            SpellAuditState.setPhase("ИТОГ");
            SpellAuditState.setVerdict(current.pass ? "PASS"
                : "FAIL: " + String.join("; ", current.problems));

            results.add(current);
            writeLog(TAG + " spell=" + current.id
                + " school=" + current.school
                + " verdict=" + current.verdict()
                + " dmg=" + fmt(current.damageDealt)
                + " hits=" + current.hits
                + " heal=" + fmt(current.healed)
                + " mana=" + fmt(current.manaGained)
                + " shield=" + fmt(current.shieldGained)
                + " moved=" + fmt(current.moved)
                + " items=" + current.itemsGained
                + " effects=" + (current.effects.isEmpty() ? "-" : String.join("|", current.effects))
                + " particles=" + current.particles
                + " sound=" + current.sound
                + " can_cast=" + current.canCast
                + (current.problems.isEmpty() ? "" : " problems=" + String.join("|", current.problems)));
            marker("spell_end", current.id);
            writeReports();
            return false;
        }

        private void cleanup(ServerLevel level, ServerPlayer player) {
            clearDummies(level);
            try {
                AABB box = new AABB(player.getX() - 16, player.getY() - 4, player.getZ() - 16,
                    player.getX() + 16, player.getY() + 16, player.getZ() + 16);
                for (Entity entity : level.getEntitiesOfClass(Entity.class, box,
                    e -> !(e instanceof Player))) {
                    entity.discard();
                }
            } catch (Throwable ignored) {
            }
            if (current != null) {
                spellIndex++;
                current = null;
            }
            spell = null;
        }

        private boolean finish(ServerPlayer player) {
            long passed = results.stream().filter(r -> r.pass).count();
            long failed = results.size() - passed;
            writeLog(String.format(Locale.ROOT, "%s done spells=%d passed=%d failed=%d",
                TAG, results.size(), passed, failed));
            marker("audit_end", "spells_" + results.size() + "_failed_" + failed);
            writeReports();
            SpellAuditState.end();
            restoreGameMode(player);
            return true;
        }

        // ---- отчёты

        private void writeReports() {
            try {
                Files.write(gameDir().resolve(REPORT_MD),
                    markdown().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException error) {
                LOGGER.warn("{} не удалось записать {}: {}", TAG, REPORT_MD, error.toString());
            }
            try {
                Files.write(gameDir().resolve(REPORT_JSON),
                    json().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException error) {
                LOGGER.warn("{} не удалось записать {}: {}", TAG, REPORT_JSON, error.toString());
            }
        }

        private String markdown() {
            StringBuilder md = new StringBuilder();
            long passed = results.stream().filter(r -> r.pass).count();
            md.append("# Живой аудит заклинаний\n\n");
            md.append(String.format(Locale.ROOT,
                "Проверено: **%d**, прошло: **%d**, провалилось: **%d**\n\n",
                results.size(), passed, results.size() - passed));
            md.append("Каждое заклинание применялось тем же путём, что и с хотбара "
                + "(`setToCast` → тик каста → `Spell.cast`), по манекенам впереди игрока. "
                + "Если урон не прошёл, делался второй заход с кольцом манекенов вокруг игрока.\n\n");
            md.append("| школа | заклинание | итог | урон | попаданий | лечение | эффекты | партиклы | проблемы |\n");
            md.append("|---|---|---|---|---|---|---|---|---|\n");
            for (SpellResult result : results) {
                md.append("| ").append(escape(result.school))
                    .append(" | `").append(escape(result.id)).append("`")
                    .append(" | ").append(result.verdict())
                    .append(" | ").append(fmt(result.damageDealt))
                    .append(" | ").append(result.hits)
                    .append(" | ").append(fmt(result.healed))
                    .append(" | ").append(result.effects.isEmpty() ? "—" : escape(String.join(", ", result.effects)))
                    .append(" | ").append(result.particles ? "да" : "нет")
                    .append(" | ").append(result.problems.isEmpty() ? "—" : escape(String.join("; ", result.problems)))
                    .append(" |\n");
            }
            return md.toString();
        }

        private String json() {
            StringBuilder out = new StringBuilder();
            out.append("{\n  \"spells\": [\n");
            for (int i = 0; i < results.size(); i++) {
                SpellResult result = results.get(i);
                out.append("    {");
                out.append("\"id\": \"").append(jsonEscape(result.id)).append("\", ");
                out.append("\"school\": \"").append(jsonEscape(result.school)).append("\", ");
                out.append("\"name\": \"").append(jsonEscape(result.name)).append("\", ");
                out.append("\"verdict\": \"").append(result.verdict()).append("\", ");
                out.append("\"damage\": ").append(fmt(result.damageDealt)).append(", ");
                out.append("\"hits\": ").append(result.hits).append(", ");
                out.append("\"heal\": ").append(fmt(result.healed)).append(", ");
                out.append("\"mana\": ").append(fmt(result.manaGained)).append(", ");
                out.append("\"shield\": ").append(fmt(result.shieldGained)).append(", ");
                out.append("\"items\": ").append(result.itemsGained).append(", ");
                out.append("\"moved\": ").append(fmt(result.moved)).append(", ");
                out.append("\"spawned\": ").append(result.spawned).append(", ");
                out.append("\"can_cast\": ").append(result.canCast).append(", ");
                out.append("\"particles\": ").append(result.particles).append(", ");
                out.append("\"sound\": ").append(result.sound).append(", ");
                out.append("\"effects\": [");
                int e = 0;
                for (String effect : result.effects) {
                    if (e++ > 0) {
                        out.append(", ");
                    }
                    out.append("\"").append(jsonEscape(effect)).append("\"");
                }
                out.append("], ");
                out.append("\"problems\": [");
                int p = 0;
                for (String problem : result.problems) {
                    if (p++ > 0) {
                        out.append(", ");
                    }
                    out.append("\"").append(jsonEscape(problem)).append("\"");
                }
                out.append("]");
                out.append("}").append(i + 1 < results.size() ? "," : "").append("\n");
            }
            out.append("  ]\n}\n");
            return out.toString();
        }

        private static String escape(String text) {
            return text == null ? "" : text.replace("|", "\\|").replace("\n", " ");
        }

        private static String jsonEscape(String text) {
            if (text == null) {
                return "";
            }
            return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        }
    }

    // ------------------------------------------------------------------ подписи

    private static String sanitize(String text) {
        return text == null ? "" : text.replace(' ', '_');
    }

    private static String safeName(Spell spell) {
        try {
            String name = spell.locName() == null ? "" : stripCodes(spell.locName().getString());
            if (!name.isBlank() && !name.startsWith("spell.")) {
                return name;
            }
        } catch (Throwable ignored) {
        }
        if (spell.locName != null && !spell.locName.isBlank()) {
            return stripCodes(spell.locName);
        }
        return spell.GUID();
    }

    /** Описание в том виде, в каком его увидит игрок (числа уже подставлены). */
    private static String describe(ServerPlayer player, Spell spell) {
        try {
            List<String> lines = SpellDesc.getTooltip(player, spell);
            if (lines != null) {
                StringBuilder text = new StringBuilder();
                for (String line : lines) {
                    if (line == null || line.isBlank()) {
                        continue;
                    }
                    if (text.length() > 0) {
                        text.append(' ');
                    }
                    text.append(stripCodes(line).trim());
                }
                if (text.length() > 0) {
                    return text.toString().trim();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            return stripCodes(spell.locDesc).replaceAll("\\[/?calc:[^\\]]*\\]", "").trim();
        } catch (Throwable error) {
            return "";
        }
    }

    private static String stripCodes(String text) {
        return text == null ? "" : text.replaceAll("§.", "");
    }
}
