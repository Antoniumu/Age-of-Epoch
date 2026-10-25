package com.robertx22.age_of_exile.uncommon.testing;

import com.robertx22.age_of_exile.aoe_data.database.spells.SpellDesc;
import com.robertx22.age_of_exile.capability.PlayerDamageChart;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.database.data.spells.components.ComponentPart;
import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.age_of_exile.database.data.spells.components.actions.SpellAction;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.spells.SpellCastingData;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.mixin_methods.OnItemStoppedUsingCastImbuedSpell;
import com.robertx22.age_of_exile.vanilla_mc.packets.spells.TellClientToCastSpellPacket;
import com.robertx22.library_of_exile.main.Packets;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    /** Наклон взгляда бота: снаряды должны падать на площадку с манекенами. */
    private static final float AIM_PITCH = 30F;

    /**
     * Порог «эффект есть»: столько единиц лечения/маны считается фоновым шумом
     * (регенерация вида «+2 здоровья при тике» случается и без умения).
     */
    private static final float EFFECT_NOISE = 2.5F;

    /**
     * Прогрев перед кастом: столько тиков прогон просто стоит и меряет
     * естественный прирост маны и здоровья.
     */
    private static final int WARMUP_TICKS = 10;

    /** Сколько строк трассировки снарядов писать на одно умение. */
    private static final int PROJECTILE_TRACES = 12;

    /** Уровень, на котором проверяются умения: к этому моменту открыто всё. */
    private static final int AUDIT_LEVEL = 40;

    /** Площадка: ровный пол из кварца в небе — чтобы видео было чистым. */
    private static final int PLATFORM_Y = 99;
    /**
     * Радиус арены. Больше, чем нужно манекенам (6.5 блока), потому что луч
     * «в точку взгляда» у части умений тянется на 32 блока: склон горы в 18
     * блоках от центра перехватывал его, и умение выглядело нерабочим.
     */
    private static final int PLATFORM_RADIUS = 18;
    /** Сколько блоков воздуха расчищается над площадкой (рельеф заранее неизвестен). */
    private static final int PLATFORM_CLEAR_HEIGHT = 32;
    /**
     * Манекены в первом заходе: расстояние и боковой сдвиг.
     *
     * <p>Раньше они стояли в упор по центру (1.5 блока), и в видео был виден
     * только зомби: ни партиклов, ни полёта снаряда за ним. Теперь это широкая
     * дуга — ближние умения достают, а картинка остаётся читаемой.
     */
    private static final double[][] DUMMY_PLACEMENT = {
        {1.3D, 0D}, {3.5D, -2.2D}, {4.5D, 0D}, {4.0D, 2.2D}, {6.5D, 0D}
    };

    private static Runner active;

    /**
     * Счётчик настоящих событий урона: заполняет {@code DamageEvent} прямо перед
     * тем, как снять здоровье. Нужен, чтобы различать две разные поломки:
     * «заклинание не нашло цели» и «урон посчитался, но замер его не увидел».
     */
    public static void noteDamageEvent(LivingEntity target, float vanillaAmount, String spellId) {
        Runner runner = active;
        if (runner == null || runner.current == null || target == null) {
            return;
        }
        if (vanillaAmount <= 0F) {
            return;
        }
        // Урон засчитывается умению, которое его вызвало. Если событие не несёт
        // id (урон от блока, ловушки, доты) — виднее по источнику, а не по id:
        // такой урон засчитывается только пока активно окно наблюдения после
        // каста, и не другому умению. Снаряды прошлого умения к этому моменту
        // уже убраны (clearStrayEntities перед спавном манекенов).
        if (spellId != null && !spellId.isEmpty() && !spellId.equals(runner.current.id)) {
            return;
        }
        runner.current.damageEvents++;
        runner.current.damageEventsTotal += vanillaAmount;

        // Урон считаем по событию, а не по падению полоски здоровья манекена:
        // здоровье ванильной сущности мод периодически пересчитывает из своих
        // единиц, и такие пересчёты выглядели как урон от умения (у проклятий и
        // тотемов «урон» был у всех подряд, в том числе у лечебных).
        if (runner.dummies.contains(target.getUUID())) {
            float real = runner.toRealDamage(target, vanillaAmount);
            if (real > 0.01F) {
                runner.current.damageDealt += real;
                runner.current.hits++;
            }
        }
    }

    /**
     * Действие лечения реально сработало: видно и «до/после» здоровья цели.
     * Считать лечение по прибавке здоровья игрока нельзя — у него есть своя
     * регенерация, которая перекрывает небольшую лечебную ауру (тотем).
     */
    public static void noteHealApplied(LivingEntity target, float before, float after) {
        Runner runner = active;
        if (runner == null || runner.current == null || target == null) {
            return;
        }
        if (after > before + 0.01F || before >= target.getMaxHealth() - 0.01F) {
            runner.current.healsApplied++;
        }
    }

    /**
     * Действие восстановления маны реально сработало. Как и с лечением: прибавка
     * к мане игрока сама по себе ничего не доказывает — мана восстанавливается и
     * без умения.
     */
    public static void noteManaApplied(LivingEntity target, float value) {
        Runner runner = active;
        if (runner == null || runner.current == null || target == null) {
            return;
        }
        if (value > 0F) {
            runner.current.manaApplied++;
        }
    }

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

    /**
     * Диагностическая строка аудита из любого места мода. Пишется только во время
     * прогона: в обычной игре такие сообщения не нужны.
     */
    public static void diag(String what) {
        if (!SpellAuditState.active) {
            return;
        }
        writeLog(TAG + " " + what);
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
        boolean utility;
        boolean particles;
        boolean sound;
        boolean summon;
        boolean canCast;
        boolean serverCast;
        int castTicks;
        float damageDealt;
        int hits;
        float healed;
        float manaGained;
        float shieldGained;
        int itemsGained;
        /** Бафф действует только внутри зоны — прогону нужно в неё зайти. */
        boolean zone;

        /** Эффект, который умение требует на цели (детонатор холода и т.п.). */
        String requiredEffect;

        final Set<String> effects = new LinkedHashSet<>();
        float moved;
        int spawned;
        int damageEvents;
        /** Сколько раз падала полоска здоровья манекенов (справочно). */
        int hpDrops;
        /** Сколько раз реально сработало лечение (цель стала здоровее). */
        int healsApplied;
        /** Сколько раз реально сработало восстановление маны. */
        int manaApplied;
        float damageEventsTotal;
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
        /** Сколько раз пробуем вернуть игрока на арену, прежде чем строить её под ним. */
        private static final int TELEPORT_RETRIES = 40;
        private final String what;
        private final List<Group> groups = new ArrayList<>();
        private final List<SpellResult> results = new ArrayList<>();
        private final Set<UUID> dummies = new HashSet<>();
        private final Map<UUID, Float> lastHealth = new LinkedHashMap<>();
        /** Модовое максимальное здоровье манекена: нужно, чтобы вернуть урон в «настоящие» единицы. */
        private final Map<UUID, Float> dummyModMaxHealth = new LinkedHashMap<>();
        /** Сами манекены: искать их по UUID через игру ненадёжно (см. {@link #dummy}). */
        private final Map<UUID, Mob> dummyRefs = new LinkedHashMap<>();

        private int arenaX;
        private int arenaY;
        private int arenaZ;
        private int teleportAttempts;
        private boolean fallbackBuilt;

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
        private int projectileTraces;
        /** Естественный прирост за тик, до каста. Вычитается из эффекта умения. */
        private float manaBaseRate;
        private float healBaseRate;
        private float baselineMana;
        private float baselineHealth;
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
                    prepareCharacter(player);
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
                    keepPlayerOnPlatform(player, level);
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
                    holdWeapon(player, spell);
                    clearStrayEntities(level, player);
                    spawnDummies(player, level);
                    writeLog(TAG + " dummies " + dummiesReport(level, player));
                    applyRequiredEffect(level);
                    snapshot(player, level);
                    SpellAuditState.setPhase("ПОДГОТОВКА");
                    phase = Phase.CAST;
                    phaseTicks = 0;
                }
                case CAST -> {
                    // Сначала прогрев без заклинания. Мана и здоровье в моде
                    // восстанавливаются сами; без этой паузы регенерация за время
                    // наблюдения выдавала себя за эффект умения, и тотем, который
                    // ничего не делает, «проходил» проверку.
                    if (phaseTicks <= WARMUP_TICKS) {
                        sampleBaseline(player);
                        return false;
                    }
                    aimAtNearestDummy(player, level);
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

        /**
         * Все части умения, включая вложенные «на попадание».
         *
         * <p>Партиклы и звуки попадания объявлены не в самой части, а в её
         * дочерних частях ({@code addPerEntityHit}), поэтому обход только верхнего
         * уровня выдавал ложное «нет партиклов» (например, у проклятий).
         */
        private List<ComponentPart> allParts(Spell spell) {
            List<ComponentPart> parts = new ArrayList<>();
            for (ComponentPart part : spell.getAttached()
                .getAllComponents()) {
                collectParts(part, parts);
            }
            return parts;
        }

        private void collectParts(ComponentPart part, List<ComponentPart> parts) {
            if (part == null) {
                return;
            }
            parts.add(part);
            for (ComponentPart child : part.getPerEntityHit()) {
                collectParts(child, parts);
            }
        }

        private void annotate(SpellResult result, Spell spell) {
            try {
                for (ComponentPart part : allParts(spell)) {
                    if (part != null && part.en_preds != null) {
                        for (MapHolder pred : part.en_preds) {
                            if (pred != null && "has_effect".equals(pred.type)) {
                                try {
                                    // Умение-детонатор (например «взрыв холода») бьёт
                                    // только цели с эффектом. Прогон обязан сначала
                                    // наложить этот эффект, иначе проверяется не
                                    // умение, а отсутствие подготовки.
                                    result.requiredEffect = pred.get(com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.POTION_ID);
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    }
                    if (part != null && part.ifs != null) {
                        for (MapHolder condition : part.ifs) {
                            if (condition != null && condition.type != null
                                && condition.type.equals("is_en_in_radius")) {
                                // Бафф работает только пока стоишь в зоне: прогон
                                // обязан в неё зайти, иначе «эффекта нет» — это
                                // не поломка умения, а незаход в зону.
                                result.zone = true;
                            }
                        }
                    }
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
                            // Щит меряется отдельным счётчиком (shieldGained), а
                            // не «наложенными эффектами»: иначе умение, которое
                            // честно выдало щит, считалось проваленным.
                            result.shield = true;
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
                            // измеримые цифры, но это ровно то, ради чего умение
                            // берут: отдельный канал, чтобы не требовать с них
                            // наложенных эффектов.
                            result.utility = true;
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
                && !result.move && !result.item && !result.summon && !result.utility
                && !result.shield) {
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
            arenaX = 0;
            arenaY = PLATFORM_Y;
            arenaZ = 0;
            teleportAttempts = 0;
            fallbackBuilt = false;
            try {
                // Чанк арены форсируется заранее: иначе площадка может оказаться в
                // «пограничном» чанке, и сущности, поставленные в неё, не попадают в
                // индекс `level.getEntity(uuid)` — манекен есть, а найти его нельзя.
                level.setChunkForced(0, 0, true);
                level.getChunk(0, 0);
            } catch (Throwable error) {
                writeLog(TAG + " platform_chunk_failed " + error);
            }
            buildArena(level, arenaX, arenaY, arenaZ);
            teleport(player, level, arenaX + 0.5D, arenaY + 1, arenaZ + 0.5D);
            player.setYRot(0F);
            player.setXRot(0F);
            writeLog(String.format(Locale.ROOT, "%s platform ready at y=%d player=%.1f/%.1f/%.1f",
                TAG, PLATFORM_Y + 1, player.getX(), player.getY(), player.getZ()));
        }

        /** Пол и столб воздуха. Годится и для небесной арены, и для запасной площадки. */
        private void buildArena(ServerLevel level, int x0, int y0, int z0) {
            BlockPos center = new BlockPos(x0, y0, z0);
            int radius = PLATFORM_RADIUS;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    level.setBlock(center.offset(x, 0, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                }
            }
            // Над площадкой — воздух: мир создаётся случайным, и площадка может
            // оказаться внутри горы. Тогда «эффект в точку взгляда» упирается в
            // камень и уезжает вверх на десятки блоков, а манекены стоят в
            // монолите — умения выглядят нерабочими по причинам, не связанным с
            // умениями. Сквозной столб воздуха делает прогон воспроизводимым на
            // любом рельефе.
            for (int y = 1; y <= PLATFORM_CLEAR_HEIGHT; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = center.offset(x, y, z);
                        if (!level.getBlockState(pos)
                            .isAir()) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        /**
         * Игрок обязан оказаться на арене.
         *
         * <p>Мир создаётся прямо перед прогоном, и первый же телепорт может не
         * сработать: сервер ещё не подтвердил позицию клиента, клиент «выигрывает»
         * и оставляет игрока на точке спавна. Тогда арена остаётся в стороне,
         * манекены появляются далеко от игрока, и весь отчёт превращается в «нет
         * попаданий» — так прогон однажды уже соврал (33/72 при полностью живых
         * умениях). Поэтому позиция проверяется на каждом тике прогрева, телепорт
         * повторяется, а если он так и не сработал — арена строится прямо под
         * игроком, где бы он ни стоял.
         */
        private void keepPlayerOnPlatform(ServerPlayer player, ServerLevel level) {
            if (onArena(player)) {
                return;
            }
            if (teleportAttempts < TELEPORT_RETRIES) {
                teleportAttempts++;
                teleport(player, level, arenaX + 0.5D, arenaY + 1, arenaZ + 0.5D);
                return;
            }
            if (!fallbackBuilt) {
                fallbackBuilt = true;
                int x = (int) Math.floor(player.getX());
                int z = (int) Math.floor(player.getZ());
                int y = (int) Math.floor(player.getY()) - 1;
                buildArena(level, x, y, z);
                arenaX = x;
                arenaY = y;
                arenaZ = z;
                writeLog(TAG + " platform_fallback построена под игроком " + x + "/" + y + "/" + z);
            }
        }

        private boolean onArena(ServerPlayer player) {
            return Math.abs(player.getX() - (arenaX + 0.5D)) < 4D
                && Math.abs(player.getZ() - (arenaZ + 0.5D)) < 4D
                && Math.abs(player.getY() - (arenaY + 1)) < 4D;
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
        /**
         * Бот получает настоящий уровень и оружие под тип каста.
         *
         * <p>Без этого замер врёт дважды: у голого игрока 1-го уровня урон
         * умений, которые масштабируются от атаки оружия
         * ({@code ValueCalculation.scaleWithAttack}), выходит почти нулевым, а
         * каст с требованием «посох/лук/меч» вообще не проходит проверку. То
         * есть проверялось бы «есть ли у бота меч», а не «работает ли умение».
         *
         * <p>Оружие создаётся тем же генератором, что и дроп
         * ({@code GearBlueprint}), поэтому статы настоящие, а не подставленные.
         */
        private void prepareCharacter(ServerPlayer player) {
            try {
                Load.Unit(player).setLevel(AUDIT_LEVEL);
                writeLog(TAG + " уровень игрока выставлен на " + AUDIT_LEVEL);
            } catch (Throwable error) {
                writeLog(TAG + " не удалось выставить уровень: " + error);
            }
            equipFor(player, CastingWeapon.MELEE_WEAPON);
            equipFor(player, CastingWeapon.RANGED);
            equipFor(player, CastingWeapon.MAGE_WEAPON);
        }

        /** Кладёт в руку подходящее оружие нужного типа (посох/лук/меч). */
        private void equipFor(ServerPlayer player, CastingWeapon weapon) {
            try {
                BaseGearType.SlotTag tag = switch (weapon) {
                    case RANGED -> BaseGearType.SlotTag.ranger_casting_weapon;
                    case MAGE_WEAPON -> BaseGearType.SlotTag.mage_weapon;
                    case MELEE_WEAPON -> BaseGearType.SlotTag.melee_weapon;
                    default -> null;
                };
                if (tag == null) {
                    return;
                }
                BaseGearType type = ExileDB.GearTypes()
                    .getFilterWrapped(x -> x.getTags().contains(tag)
                        && x.getLevelRange().isLevelInRange(AUDIT_LEVEL))
                    .random();
                if (type == null) {
                    LOGGER.warn("{} нет типа снаряжения с тегом {} на уровне {}",
                        TAG, tag, AUDIT_LEVEL);
                    return;
                }
                GearBlueprint blueprint = new GearBlueprint(AUDIT_LEVEL, 0);
                blueprint.level.set(AUDIT_LEVEL);
                blueprint.gearItemSlot.set(type);
                ItemStack stack = blueprint.createStack();
                // Держим оружие в инвентаре и переставляем в руку перед замером:
                // рука может понадобиться другому типу каста.
                player.getInventory().add(stack);
            } catch (Throwable error) {
                LOGGER.warn("{} снаряжение {} не создано: {}", TAG, weapon, error.toString());
            }
        }

        /** Выбирает из инвентаря оружие под тип каста и берёт его в руку. */
        private void holdWeapon(ServerPlayer player, Spell spell) {
            try {
                BaseGearType.SlotTag tag = switch (spell.getConfig().castingWeapon) {
                    case RANGED -> BaseGearType.SlotTag.ranger_casting_weapon;
                    case MAGE_WEAPON -> BaseGearType.SlotTag.mage_weapon;
                    case MELEE_WEAPON -> BaseGearType.SlotTag.melee_weapon;
                    default -> BaseGearType.SlotTag.weapon_family;
                };
                ItemStack best = ItemStack.EMPTY;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    GearItemData gear = com.robertx22.age_of_exile.uncommon.datasaving.Gear.Load(stack);
                    if (gear == null) {
                        continue;
                    }
                    if (tag == BaseGearType.SlotTag.weapon_family
                        || gear.GetBaseGearType().getTags().contains(tag)) {
                        best = stack;
                        break;
                    }
                }
                if (!best.isEmpty()) {
                    player.setItemSlot(EquipmentSlot.MAINHAND, best);
                }
            } catch (Throwable error) {
                LOGGER.warn("{} оружие не взято в руку: {}", TAG, error.toString());
            }
        }

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
            // Эффекты снимаем: часть бафов взаимоисключающие (песни — «один из
            // типа»), и остаток от предыдущего умения не давал наложить
            // следующий. Плюс так эффект нельзя спутать с чужим.
            try {
                for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
                    player.removeEffect(instance.getEffect());
                }
                Load.Unit(player).getStatusEffectsData().exileMap.clear();
            } catch (Throwable ignored) {
            }
            player.setDeltaMovement(0, 0, 0);
            player.setRemainingFireTicks(0);
            player.clearFire();
            teleport(player, level, arenaX + 0.5D, arenaY + 1, arenaZ + 0.5D);
            player.setYRot(0F);
            // Смотрим вниз на ~30°: снаряды «в точку взгляда» падают на площадку
            // впереди (а не за спинами манекенов), стрелы летят туда же.
            player.setXRot(AIM_PITCH);
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

        /**
         * Подготовка цели: если умение срабатывает только по существам с
         * эффектом (детонаторы холода и подобное), эффект накладывается на
         * манекены до каста — как это сделал бы игрок.
         */
        private void applyRequiredEffect(ServerLevel level) {
            if (current.requiredEffect == null || current.requiredEffect.isEmpty()) {
                return;
            }
            try {
                net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getValue(
                    Identifier.parse(current.requiredEffect));
                if (effect == null) {
                    return;
                }
                for (UUID id : dummies) {
                    Entity entity = level.getEntity(id);
                    if (entity instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(
                            net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                            20 * 20, 0, false, true));
                    }
                }
                current.note("подготовка: на манекены наложен " + current.requiredEffect);
                writeLog(TAG + " prepare_effect spell=" + current.id + " effect=" + current.requiredEffect);
            } catch (Throwable error) {
                current.note("не удалось наложить эффект подготовки: " + error);
            }
        }

        /**
         * Наводит взгляд на ближайший манекен.
         *
         * <p>Раньше бот смотрел строго на север с постоянным наклоном. Снаряды и
         * «эффект в точку взгляда» уходили мимо манекенов, и в отчёте это
         * выглядело как поломка умения: урон не нанесён, эффекты не наложены.
         * Живой игрок целится в цель — вот и бот целится: считаем yaw/pitch от
         * глаз до центра ближайшего манекена.
         */
        private void aimAtNearestDummy(ServerPlayer player, ServerLevel level) {
            LivingEntity target = null;
            double best = Double.MAX_VALUE;
            for (UUID id : dummies) {
                LivingEntity living = dummy(level, id);
                if (living != null && living.isAlive()) {
                    double distance = living.distanceToSqr(player);
                    if (distance < best) {
                        best = distance;
                        target = living;
                    }
                }
            }
            if (target == null) {
                player.setYRot(0F);
                player.setYHeadRot(0F);
                player.setXRot(AIM_PITCH);
                return;
            }
            net.minecraft.world.phys.Vec3 eyes = player.getEyePosition();
            double dx = target.getX() - eyes.x;
            double dz = target.getZ() - eyes.z;
            double dy = target.getY() + target.getBbHeight() * 0.5D - eyes.y;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) (horizontal < 0.001D ? 0D
                : Math.toDegrees(Math.atan2(-dy, horizontal)));
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
            player.setXRot(pitch);
        }

        /**
         * Убирает всё, что осталось от предыдущего умения: стрелы, снаряды,
         * блоки-тотемы.
         *
         * <p>Без этого снаряд, выпущенный в конце прошлого замера, попадал в
         * манекены уже во время следующего — и урон приписывался чужому умению
         * (у «сделать стрелы» был урон 11.6 от стрел предыдущего умения).
         */
        private void clearStrayEntities(ServerLevel level, ServerPlayer player) {
            try {
                AABB box = new AABB(player.getX() - 48, player.getY() - 16, player.getZ() - 48,
                    player.getX() + 48, player.getY() + 32, player.getZ() + 48);
                for (Entity entity : level.getEntitiesOfClass(Entity.class, box,
                    e -> !(e instanceof Player) && !dummies.contains(e.getUUID()))) {
                    entity.discard();
                }
            } catch (Throwable ignored) {
            }
        }

        private void spawnDummies(ServerPlayer player, ServerLevel level) {
            clearDummies(level);
            double yaw = Math.toRadians(player.getYRot());
            double dirX = -Math.sin(yaw);
            double dirZ = Math.cos(yaw);
            // перпендикуляр к направлению взгляда — для бокового сдвига
            double sideX = -dirZ;
            double sideZ = dirX;
            int index = 0;
            for (double[] placement : DUMMY_PLACEMENT) {
                double forward = placement[0];
                double side = placement[1];
                index++;
                spawnDummy(level,
                    player,
                    player.getX() + dirX * forward + sideX * side,
                    player.getZ() + dirZ * forward + sideZ * side,
                    "§7[Манекен " + index + "]");
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
                // 100 тысяч, а не 10 миллионов: при огромном максимуме
                // HealthUtils.realToVanilla() (урон / макс. здоровья мода * макс.
                // здоровья ванилы) уводит урон в шум, и полоска здоровья
                // манекена в кадре нечитаема.
                if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
                    mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100_000D);
                }
                mob.setHealth(mob.getMaxHealth());
                mob.setCustomName(Component.literal(name));
                mob.setCustomNameVisible(true);
                level.addFreshEntity(mob);
                dummies.add(mob.getUUID());
                dummyRefs.put(mob.getUUID(), mob);
                lastHealth.put(mob.getUUID(), mob.getHealth());
                // Урон в моде считается в «настоящих» единицах здоровья, а на
                // ванильную сущность он переводится как
                // (урон / модовый максимум * ванильный максимум). Обратный
                // пересчёт нужен, чтобы в отчёте были единицы урона, а не
                // проценты от полоски манекена.
                float modMax = 1F;
                try {
                    modMax = com.robertx22.age_of_exile.uncommon.utilityclasses.HealthUtils.getMaxHealth(mob);
                } catch (Throwable ignored) {
                }
                dummyModMaxHealth.put(mob.getUUID(), modMax > 0 ? modMax : 1F);
            } catch (Throwable error) {
                LOGGER.warn("{} манекен не создан: {}", TAG, error.toString());
            }
        }

        private void clearDummies(ServerLevel level) {
            for (UUID id : new ArrayList<>(dummies)) {
                Entity entity = dummy(level, id);
                if (entity != null) {
                    entity.discard();
                }
            }
            dummies.clear();
            dummyRefs.clear();
            lastHealth.clear();
            dummyModMaxHealth.clear();
        }

        /**
         * Манекен по UUID.
         *
         * <p>Через игру искать нельзя: во время прогона {@code level.getEntity(uuid)}
         * возвращал null для только что поставленных манекенов (сущность есть,
         * попадания идут, а поиск пустой) — и тогда замер «не видел» ни целей, ни
         * их здоровья. Поэтому сначала берём ту самую сущность, которую создали.
         */
        private LivingEntity dummy(ServerLevel level, UUID id) {
            Mob known = dummyRefs.get(id);
            if (known != null && known.isAlive()) {
                return known;
            }
            Entity entity = level.getEntity(id);
            return entity instanceof LivingEntity living ? living : null;
        }

        private String dummiesReport(ServerLevel level, ServerPlayer player) {
            int ready = 0;
            for (UUID id : dummies) {
                LivingEntity living = dummy(level, id);
                if (living != null && living.isAlive()) {
                    ready++;
                }
            }
            return String.format(Locale.ROOT, "n=%d ready=%d player=%.1f/%.1f/%.1f",
                dummies.size(), ready, player.getX(), player.getY(), player.getZ());
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
            projectileTraces = 0;
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

        /**
         * Замер естественного восстановления: сколько маны и здоровья
         * прибавляется за тик, если ничего не применять.
         */
        private void sampleBaseline(ServerPlayer player) {
            try {
                EntityData data = Load.Unit(player);
                float mana = data.getResources()
                    .getMana();
                if (phaseTicks <= 1) {
                    baselineMana = mana;
                    baselineHealth = player.getHealth();
                    manaBaseRate = 0;
                    healBaseRate = 0;
                    return;
                }
                int ticks = phaseTicks - 1;
                manaBaseRate = Math.max(0F, (mana - baselineMana) / ticks);
                healBaseRate = Math.max(0F, (player.getHealth() - baselineHealth) / ticks);
            } catch (Throwable ignored) {
            }
        }

        /** Подробности площадки: без них непонятно, «не попал» или «не работает». */
        private void diagnostics(ServerLevel level, ServerPlayer player, String tag) {
            StringBuilder text = new StringBuilder();
            text.append(TAG).append(" diag_").append(tag)
                .append(" spell=").append(current.id)
                .append(" player=").append(String.format(Locale.ROOT, "%.1f/%.1f/%.1f",
                    player.getX(), player.getY(), player.getZ()))
                .append(" yaw=").append((int) player.getYRot())
                .append(" pitch=").append((int) player.getXRot())
                .append(" cast_ticks=").append(current.castTicks)
                .append(" can_cast=").append(current.canCast)
                .append(" dummies=").append(dummies.size());
            for (UUID id : dummies) {
                LivingEntity living = dummy(level, id);
                if (living != null) {
                    double distance = Math.sqrt(living.distanceToSqr(player));
                    text.append(" [").append(String.format(Locale.ROOT, "%.1fб hp=%.0f",
                        distance, living.getHealth())).append("]");
                } else {
                    text.append(" [нет сущности]");
                }
            }
            writeLog(text.toString());
        }

        /**
         * Применяет умение.
         *
         * <p>Почему не через очередь каста ({@code setToCast} → тик за тиком), как
         * это делает хотбар: в одиночной игре ту же очередь тикает ещё и клиент
         * (её состояние синхронизируется), и стороны гоняют друг друга — на
         * практике примерно у трети умений каст завершался только на клиенте,
         * где урон не считается, а эффекты живут лишь в клиентской копии. Аудит
         * тогда мерил не умение, а гонку.
         *
         * <p>Поэтому здесь два честных шага:
         * <ol>
         *   <li>на сервере — та же точка входа, что и в конце очереди каста:
         *       {@code Spell.cast(ctx, true)} — она и наносит урон, и накладывает
         *       эффекты, и выдаёт предметы;</li>
         *   <li>клиенту — штатный пакет {@code TellClientToCastSpellPacket}: он
         *       проигрывает ту же заклинательную анимацию, партиклы и звук
         *       (частицы в моде рисуются только на клиенте), поэтому в кадре
         *       видно то же, что увидел бы игрок.</li>
         * </ol>
         * Проверка требований (оружие, мана, кулдаун) остаётся отдельной и honest:
         * её результат пишется в {@code can_cast}.
         */
        private void doCast(ServerPlayer player) {
            try {
                SpellCastContext ctx = new SpellCastContext(player, 0, spell);
                current.castTicks = Math.max(0, spell.getCastTimeTicks(ctx));
            } catch (Throwable error) {
                current.castTicks = 0;
                current.note("не удалось посчитать время каста: " + error);
            }
            try {
                SpellCastContext ctx = new SpellCastContext(player, 0, spell);
                spell.cast(ctx, true);
                current.serverCast = true;
            } catch (Throwable error) {
                current.problem("каст упал с ошибкой: " + error);
            }
            // Умения-выстрелы (лук) при касте не срабатывают сразу: каст только
            // «заряжает» следующий выстрел, а сами стрелы и урон появляются,
            // когда игрок отпускает тетиву. Раньше прогон этого не делал — и все
            // такие умения выглядели сломанными («нет попаданий»), хотя урон
            // наносится на выстреле. Здесь прогон стреляет ровно тем же путём,
            // каким это делает игра при отпускании лука.
            fireImbuedShot(player);
            try {
                // Клиенту — та же команда, что мод шлёт для отрисовки чужого каста.
                Packets.sendToClient(player, new TellClientToCastSpellPacket(player, spell));
            } catch (Throwable error) {
                current.note("клиент не получил команду на отрисовку: " + error);
            }
            // 130 тиков (6.5 с) — нижняя граница: часть умений сначала ставит
            // тотем/сферу, а урон или лечение идёт раз в секунду или при её
            // истечении (чёрная дыра бьёт через 5 с).
            maxObserve = Math.min(300, Math.max(130, current.castTicks + 80));
            if (current.damage) {
                try {
                    diagnostics((ServerLevel) player.level(), player, "cast");
                } catch (Throwable ignored) {
                }
            }
            SpellAuditState.setPhase("ПРИМЕНЕНИЕ");
            writeLog(TAG + " cast spell=" + current.id + " can_cast=" + current.canCast
                + " cast_ticks=" + current.castTicks + " server_cast=" + current.serverCast);
        }

        /** Выстрел заряженным умением: тот же путь, что при отпускании тетивы. */
        private void fireImbuedShot(ServerPlayer player) {
            try {
                if (Load.spells(player)
                    .getCastingData().imbued_spell_stacks <= 0) {
                    return;
                }
                current.note("умение накладывается на выстрел из лука — прогон стреляет");
                loadBow(player);
                OnItemStoppedUsingCastImbuedSpell.onStoppedUsing(
                    new ItemStack(net.minecraft.world.item.Items.BOW), player, 10);
                writeLog(TAG + " imbued_shot spell=" + current.id);
            } catch (Throwable error) {
                current.note("не удалось выстрелить заряженным умением: " + error);
            }
        }

        /** Кладёт лук в руку: требования оружия у стрелковых умений — лук. */
        private void loadBow(ServerPlayer player) {
            try {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new ItemStack(net.minecraft.world.item.Items.BOW));
            } catch (Throwable ignored) {
            }
        }

        /** @return true — наблюдение закончено */
        private boolean observe(ServerPlayer player, ServerLevel level) {
            for (Map.Entry<UUID, Float> entry : lastHealth.entrySet()) {
                LivingEntity living = dummy(level, entry.getKey());
                if (living == null) {
                    continue;
                }
                float now = living.getHealth();
                float before = entry.getValue();
                if (now < before - 0.0001F) {
                    // Падение полоски — только справочная цифра: мод сам
                    // пересчитывает ванильное здоровье из своих единиц, и такие
                    // пересчёты нельзя путать с уроном заклинания.
                    current.hpDrops++;
                }
                entry.setValue(now);
            }
            // Из прироста вычитаем то, что набежало бы само по себе.
            int elapsed = phaseTicks + WARMUP_TICKS;
            float healedRaw = player.getHealth() - playerHealthAtCast;
            current.healed = Math.max(current.healed, healedRaw - healBaseRate * elapsed);
            try {
                EntityData data = Load.Unit(player);
                float manaRaw = data.getResources()
                    .getMana() - manaAtCast;
                current.manaGained = Math.max(current.manaGained, manaRaw - manaBaseRate * elapsed);
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
                LivingEntity living = dummy(level, id);
                if (living == null) {
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
            traceProjectiles(level, player);
            stepIntoZone(level, player);
            SpellAuditState.setMetrics(metricsLine());
            SpellAuditState.setPhase(current.castTicks > 0 && phaseTicks <= current.castTicks
                ? "ПРИМЕНЕНИЕ" : "НАБЛЮДЕНИЕ");

            boolean castDone = current.castTicks == 0 || phaseTicks > current.castTicks + 8;
            // Появление сущности (тотем, сфера) само по себе не повод закончить
            // наблюдение: эффект у таких умений идёт позже, по тикам.
            boolean worthWaiting = current.damageDealt > 0 || current.healed > EFFECT_NOISE
                || !current.effects.isEmpty() || current.shieldGained > 0
                || current.itemsGained > 0 || current.manaGained > EFFECT_NOISE;
            // Умение что-то создало (тотем, зону, сферу) — досматриваем до конца
            // окна: у таких умений эффект идёт по тикам и на истечении. Раньше
            // окно закрывалось на 30-м тике, и урон «чёрной дыры» на 100-м тике
            // просто не попадал в кадр, а в отчёте оставалось «нет попаданий».
            if (current.spawned > 0) {
                worthWaiting = false;
            }
            if (phaseTicks >= maxObserve) {
                return true;
            }
            return castDone && worthWaiting && phaseTicks > 30;
        }

        /**
         * Заход в зону.
         *
         * <p>Часть умений работает только пока стоишь внутри созданной зоны
         * (магический круг). Живой игрок в неё заходит; прогон делает то же самое —
         * один раз, на 15-м тике наблюдения, чтобы в кадре было видно и зону,
         * и наложенный бафф.
         */
        private void stepIntoZone(ServerLevel level, ServerPlayer player) {
            if (!current.zone || phaseTicks != 15) {
                return;
            }
            Entity zone = null;
            double best = Double.MAX_VALUE;
            for (Entity entity : level.getEntitiesOfClass(Entity.class,
                player.getBoundingBox()
                    .inflate(16D),
                e -> !(e instanceof Player) && !dummies.contains(e.getUUID()))) {
                double distance = entity.distanceToSqr(player);
                if (distance < best) {
                    best = distance;
                    zone = entity;
                }
            }
            if (zone == null || best < 4D) {
                return;
            }
            double x = zone.getX();
            double z = zone.getZ();
            double y = zone.getY();
            teleport(player, level, x, Math.max(y, player.getY()), z);
            writeLog(String.format(Locale.ROOT,
                "%s zone_step spell=%s pos=%.1f/%.1f/%.1f", TAG, current.id, x, y, z));
        }

        /**
         * Трассировка снарядов.
         *
         * <p>«Снаряд не попал» — это два совершенно разных случая: снаряд вообще
         * не появился (или появился не там), либо он честно летит мимо цели.
         * Строка {@code diag_proj} показывает позицию и скорость снаряда
         * относительно игрока каждый второй тик наблюдения, поэтому видно и
         * точку старта, и траекторию.
         */
        private void traceProjectiles(ServerLevel level, ServerPlayer player) {
            if (projectileTraces >= PROJECTILE_TRACES || phaseTicks % 2 != 0) {
                return;
            }
            projectileTraces++;
            List<net.minecraft.world.entity.projectile.arrow.AbstractArrow> arrows = level.getEntitiesOfClass(
                net.minecraft.world.entity.projectile.arrow.AbstractArrow.class,
                player.getBoundingBox()
                    .inflate(48D));
            StringBuilder text = new StringBuilder();
            text.append(TAG).append(" diag_proj spell=").append(current.id)
                .append(" t=").append(phaseTicks)
                .append(" n=").append(arrows.size());
            for (net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow : arrows) {
                net.minecraft.world.phys.Vec3 pos = arrow.position()
                    .subtract(player.position());
                net.minecraft.world.phys.Vec3 delta = arrow.getDeltaMovement();
                text.append(" [")
                    .append(arrow.getClass()
                        .getSimpleName())
                    .append(String.format(Locale.ROOT, " pos=%.2f/%.2f/%.2f d=%.2f/%.2f/%.2f t=%d",
                        pos.x, pos.y, pos.z, delta.x, delta.y, delta.z, arrow.tickCount))
                    .append("]");
            }
            writeLog(text.toString());
        }

        /** Обратный пересчёт: падение ванильного здоровья → урон в единицах мода. */
        private float toRealDamage(LivingEntity dummy, float vanillaDrop) {
            float vanillaMax = Math.max(0.001F, dummy.getMaxHealth());
            float modMax = dummyModMaxHealth.getOrDefault(dummy.getUUID(), vanillaMax);
            return vanillaDrop / vanillaMax * modMax;
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

        /** Короткая сводка того, что реально произошло: для подписи в видео и в отчёте. */
        private String evidenceSummary() {
            return evidence(current);
        }

        /**
         * Чем подтверждено срабатывание: только измеренные факты.
         *
         * <p>Если измеримого нет — так и написано. «PASS» без единой цифры читался
         * бы как «умение проверено и работает», хотя у части умений эффект просто
         * не наблюдаем снаружи (агро, сброс кулдаунов, открытие сундука).
         */
        private static String evidence(SpellResult result) {
            List<String> parts = new ArrayList<>();
            if (result.hits > 0) {
                parts.add("урон " + fmt(result.damageDealt) + " (" + result.hits + " поп.)");
            }
            if (result.healsApplied > 0) {
                parts.add("лечение +" + fmt(result.healed)
                    + (result.healsApplied > 1 ? " ×" + result.healsApplied : ""));
            }
            if (result.manaApplied > 0) {
                parts.add("мана +" + fmt(result.manaGained)
                    + (result.manaApplied > 1 ? " ×" + result.manaApplied : ""));
            }
            if (result.shieldGained > 0.5F) {
                parts.add("щит +" + fmt(result.shieldGained));
            }
            if (!result.effects.isEmpty()) {
                parts.add("эффекты: " + String.join(", ", result.effects));
            }
            if (result.moved > 0.3F) {
                parts.add("перемещение " + fmt(result.moved) + " бл.");
            }
            if (result.itemsGained > 0) {
                parts.add("предметы +" + result.itemsGained);
            }
            if (parts.isEmpty()) {
                parts.add(result.spawned > 0
                    ? "создано сущностей: " + result.spawned
                    : "только каст: измеримых эффектов нет (агро/кулдаун/меню снаружи не видны)");
            }
            if (!result.notes.isEmpty()) {
                parts.add("прим.: " + String.join("; ", result.notes));
            }
            return String.join(" · ", parts);
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
                spawnDummyRing(player, level, 1.2D);
                snapshot(player, level);
                phaseTicks = 0;
                return true;
            }
            if (!current.canCast) {
                current.problem("каст невозможен (canCast=false: требования оружия/маны/уровня)");
            }
            // В отчёте — только подтверждённые цифры. Прибавка здоровья и маны
            // «сама по себе» бывает фоновой регенерацией, а не эффектом умения.
            if (current.healsApplied == 0 && current.healed <= EFFECT_NOISE) {
                current.healed = 0;
            }
            if (current.manaApplied == 0 && current.manaGained <= EFFECT_NOISE) {
                current.manaGained = 0;
            }
            if (current.damage && current.damageDealt <= 0.1F) {
                current.problem(current.damageEvents > 0
                    ? "урон посчитан (" + current.damageEvents
                        + " соб.), но здоровье манекенов не изменилось"
                    : "урон не нанесён (нет попаданий)");
                try {
                    diagnostics(level, player, "nodmg");
                } catch (Throwable ignored) {
                }
            }
            if (current.heal && current.healsApplied == 0 && current.healed <= EFFECT_NOISE) {
                current.problem("лечение не сработало");
            }
            if (current.mana && current.manaApplied == 0 && current.manaGained <= EFFECT_NOISE) {
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
            // В подписи к видео — не только «PASS», но и чем именно это
            // подтверждено: так ролик годится и разработчику, и игроку.
            SpellAuditState.setVerdict(current.pass ? "PASS · " + evidenceSummary()
                : "FAIL: " + String.join("; ", current.problems));

            results.add(current);
            writeLog(TAG + " spell=" + current.id
                + " school=" + current.school
                + " verdict=" + current.verdict()
                + " dmg=" + fmt(current.damageDealt)
                + " hits=" + current.hits
                + " dmg_events=" + current.damageEvents
                + " heal=" + fmt(current.healed)
                + " mana=" + fmt(current.manaGained)
                + " shield=" + fmt(current.shieldGained)
                + " moved=" + fmt(current.moved)
                + " items=" + current.itemsGained
                + " effects=" + (current.effects.isEmpty() ? "-" : String.join("|", current.effects))
                + " particles=" + current.particles
                + " sound=" + current.sound
                + " can_cast=" + current.canCast
                + " server_cast=" + current.serverCast
                + (current.problems.isEmpty() ? "" : " problems=" + String.join("|", current.problems)));
            marker("spell_end", current.id);
            writeReports();
            return false;
        }

        private void cleanup(ServerLevel level, ServerPlayer player) {
            clearDummies(level);
            try {
                AABB box = new AABB(player.getX() - 48, player.getY() - 16, player.getZ() - 48,
                    player.getX() + 48, player.getY() + 32, player.getZ() + 48);
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

        /** «×N» — сколько раз действие реально сработало (если больше одного). */
        private static String countSuffix(int times) {
            return times > 1 ? " (×" + times + ")" : "";
        }

        private String markdown() {
            StringBuilder md = new StringBuilder();
            long passed = results.stream().filter(r -> r.pass).count();
            md.append("# Живой аудит заклинаний\n\n");
            md.append(String.format(Locale.ROOT,
                "Проверено: **%d**, прошло: **%d**, провалилось: **%d**\n\n",
                results.size(), passed, results.size() - passed));
            md.append("Колонка «чем подтверждено» — только измеренные факты: урон из "
                + "событий урона самого умения, лечение и мана из сработавших действий, "
                + "щит из ресурсов игрока, эффекты и перемещение — по изменению состояния. "
                + "Умения, у которых снаружи нечего измерить (агро, сброс кулдаунов, меню), "
                + "помечены явно.\n\n");
            md.append("Каждое заклинание применялось тем же путём, что и с хотбара "
                + "(`setToCast` → тик каста → `Spell.cast`), по манекенам впереди игрока. "
                + "Если урон не прошёл, делался второй заход с кольцом манекенов вокруг игрока.\n\n");
            md.append("| школа | заклинание | итог | урон | попаданий | лечение | мана |"
                + " чем подтверждено | партиклы | проблемы |\n");
            md.append("|---|---|---|---|---|---|---|---|---|---|\n");
            for (SpellResult result : results) {
                md.append("| ").append(escape(result.school))
                    .append(" | `").append(escape(result.id)).append("`")
                    .append(" | ").append(result.verdict())
                    .append(" | ").append(fmt(result.damageDealt))
                    .append(" | ").append(result.hits)
                    .append(" | ").append(fmt(result.healed)).append(countSuffix(result.healsApplied))
                    .append(" | ").append(fmt(result.manaGained)).append(countSuffix(result.manaApplied))
                    .append(" | ").append(escape(evidence(result)))
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
                out.append("\"evidence\": \"").append(jsonEscape(evidence(result))).append("\", ");
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
