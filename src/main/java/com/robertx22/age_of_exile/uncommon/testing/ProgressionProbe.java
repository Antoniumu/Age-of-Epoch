package com.robertx22.age_of_exile.uncommon.testing;

import com.robertx22.age_of_exile.ascension.AscensionClasses;
import com.robertx22.age_of_exile.ascension.AscensionData;
import com.robertx22.age_of_exile.ascension.AscensionLogic;
import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.stats.datapacks.stats.CoreStat;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
 * Зонд полного цикла прокачки: от 1-го уровня до Вознесённого класса.
 *
 * <p>Что показывает прогон (и что видно на видео CI):</p>
 * <ol>
 *   <li>набор опыта и level-up'ы — настоящим {@code GiveExp}, поэтому на экране
 *   те же «+N опыта» и сообщения о новом уровне, что у игрока;</li>
 *   <li>вложение очков характеристик — той же логикой, что у пакета GUI;</li>
 *   <li>изучение заклинания школы — через {@code canLearn/learnSpell};</li>
 *   <li>на 50-м — выбор пути Вознесения и вложение очков Вознесения по цепочке,
 *   назначение умения на хотбар.</li>
 * </ol>
 *
 * <p>Каждый этап обрамляется маркерами {@code AOEPROG_MARK stage_start/stage_end}
 * в {@code progression-timeline.txt} — по ним CI режет видео на ролики по этапам.
 * Итог — строка {@code AOEPROG done passed=N failed=M}, отчёт
 * {@code progression-report.md} и машинный {@code progression-report.json}.</p>
 *
 * <p>Запуск: команда {@code /aoe devtest progression [level=56] [alloc=some|none]}
 * или флаг-файл {@code aoe-progression.flag} с тем же содержимым (его кладёт
 * CI-сценарий {@code tools/e2e/scenarios/progression.txt}). {@code alloc=none}
 * не тратит очки — их потом тратит GUI-сценарий настоящими кликами.</p>
 *
 * <p>Почему по тикам, а не одним заходом: level-up'ы должны попасть в кадр
 * по одному, а не «скачком 1 → 56 за один кадр».</p>
 */
public final class ProgressionProbe {

    public static final String TAG = "AOEPROG";
    /** Флаг-файл: есть в каталоге игры — значит прогон надо начать самому. */
    public static final String FLAG_FILE = "aoe-progression.flag";
    private static final String TIMELINE_FILE = "progression-timeline.txt";
    private static final String REPORT_MD = "progression-report.md";
    private static final String REPORT_JSON = "progression-report.json";

    private static final Logger LOGGER = LoggerFactory.getLogger("AgeOfEpoch-Progression");

    /** Уровень по умолчанию: 56 = 7 очков Вознесения (хватает на 3+3+1 по цепочке). */
    private static final int DEFAULT_TARGET = 56;
    /** Один уровень за столько тиков: level-up сообщения успевают попасть в кадр. */
    private static final int TICKS_PER_LEVEL = 5;
    /** Пауза между этапами, чтобы этап читался на видео. */
    private static final int STAGE_PAUSE_TICKS = 30;

    private static Runner active;

    private ProgressionProbe() {
    }

    // ------------------------------------------------------------------ запуск

    /** Ставит прогон в очередь: {@code what} = "level=56 alloc=some" (пробелы/запятые). */
    public static void request(String what) {
        if (active != null) {
            LOGGER.info("{} пропуск: прогон уже идёт", TAG);
            return;
        }
        active = new Runner(what);
        LOGGER.info("{} старт прогона: {}", TAG, what);
    }

    /** Вызывается каждый тик сервера: двигает прогон на один шаг. */
    public static void tick(MinecraftServer server) {
        if (active == null && flagPresent()) {
            request(readFlag());
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
            writeLines(List.of(TAG + " fatal=" + error));
            ProgressionState.setVerdict("FAIL: " + error);
            ProgressionState.end();
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

    private static String readFlag() {
        try {
            return Files.readString(gameDir().resolve(FLAG_FILE), StandardCharsets.UTF_8).trim();
        } catch (IOException error) {
            return "";
        }
    }

    private static void clearFlag() {
        try {
            Files.deleteIfExists(gameDir().resolve(FLAG_FILE));
        } catch (IOException error) {
            LOGGER.warn("{} не удалось удалить флаг: {}", TAG, error.toString());
        }
    }

    private static void writeLines(List<String> lines) {
        for (String line : lines) {
            LOGGER.info(line);
        }
    }

    /** Маркер таймлайна: по нему CI режет видео на этапы. */
    private static void marker(String kind, String name) {
        long now = System.currentTimeMillis();
        String line = String.format(Locale.ROOT, "AOEPROG_MARK %s %s %d", kind, name, now);
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
        INTRO,
        LEVEL,          // качаем до ближайшей вехи
        ALLOC_STATS,    // демо-вложение очков характеристик
        ALLOC_SPELL,    // демо-изучение заклинания школы
        ASCEND_PICK,    // выбор пути Вознесения
        ASCEND_ALLOC,   // вложение очков Вознесения по цепочке
        HOTBAR,         // назначение вознесённого умения на панель
        STAGE_PAUSE,    // пауза между этапами (для видео)
        VERIFY,
        DONE
    }

    private static final class Runner {

        /** Одна веха прокачки: уровень и что сделать по достижении. */
        private static final class Milestone {
            final int level;
            final Phase then;

            Milestone(int level, Phase then) {
                this.level = level;
                this.then = then;
            }
        }

        final int targetLevel;
        /** some — вложить демо-очки (статы/заклинание); none — не тратить (их потратит GUI-сценарий). */
        final boolean allocateDemo;

        Phase phase = Phase.INTRO;
        Phase phaseAfterPause = Phase.LEVEL;
        int phaseTicks = 0;

        final List<Milestone> milestones = new ArrayList<>();
        int milestoneIndex = 0;

        boolean spellLearned = false;
        int ascendAllocsDone = 0;

        final List<String> passed = new ArrayList<>();
        final List<String> failed = new ArrayList<>();
        final List<String> lines = new ArrayList<>();

        Runner(String what) {
            int target = DEFAULT_TARGET;
            boolean alloc = true;
            if (what != null) {
                for (String token : what.trim().split("[\\s,]+")) {
                    if (token.startsWith("level=")) {
                        try {
                            target = Integer.parseInt(token.substring("level=".length()).trim());
                        } catch (NumberFormatException ignored) {
                        }
                    } else if (token.equals("alloc=none")) {
                        alloc = false;
                    } else if (token.equals("alloc=some") || token.equals("alloc=all")) {
                        alloc = true;
                    }
                }
            }
            // Вознесение открывается на 50-м: ниже него прогон не имеет смысла.
            this.targetLevel = Math.max(target, AscensionClasses.UNLOCK_LEVEL + 1);
            this.allocateDemo = alloc;

            // Вехи прокачки: 10 → вложить статы, 25 → изучить заклинание,
            // 50 → выбрать путь, цель → вложить очки Вознесения.
            milestones.add(new Milestone(10, Phase.ALLOC_STATS));
            milestones.add(new Milestone(25, Phase.ALLOC_SPELL));
            milestones.add(new Milestone(AscensionClasses.UNLOCK_LEVEL, Phase.ASCEND_PICK));
            milestones.add(new Milestone(this.targetLevel, Phase.ASCEND_ALLOC));
        }

        void ok(String what) {
            passed.add(what);
            log("ok=true " + what);
        }

        void bad(String what) {
            failed.add(what);
            log("ok=false " + what);
            ProgressionState.setVerdict("FAIL: " + what);
        }

        void log(String what) {
            String line = TAG + " " + what;
            lines.add(line);
            writeLines(List.of(line));
        }

        void stageStart(String id, String title, String detail) {
            marker("stage_start", id);
            ProgressionState.stage(title, detail);
            log("stage=" + id + " «" + title + "»");
        }

        void stageEnd(String id) {
            marker("stage_end", id);
        }

        /** Переход к следующему этапу через паузу (кадр успевает показать итог). */
        void goPause(Phase next) {
            phaseAfterPause = next;
            phase = Phase.STAGE_PAUSE;
            phaseTicks = 0;
        }

        /** Один тик прогона. Возвращает true, когда всё закончено. */
        boolean step(ServerPlayer player) {
            phaseTicks++;

            switch (phase) {
                case INTRO: {
                    ProgressionState.begin(targetLevel);
                    int lvl = Load.Unit(player).getLevel();
                    int freeStats = Load.playerRPGData(player).statPoints.getFreePoints(player);
                    stageStart("intro", "Старт персонажа", "Уровень " + lvl);
                    log(String.format(Locale.ROOT, "intro level=%d exp=%d stat_free=%d",
                        lvl, Load.Unit(player).getExp(), freeStats));
                    if (lvl != 1) {
                        // Не ошибка прогона, но на видео важно: начинаем не с нуля.
                        log("warning: стартовый уровень " + lvl + " (ожидался 1)");
                    }
                    stageEnd("intro");
                    goPause(Phase.LEVEL);
                    return false;
                }

                case LEVEL: {
                    Milestone goal = milestones.get(milestoneIndex);
                    int lvl = Load.Unit(player).getLevel();
                    if (lvl >= goal.level) {
                        stageEnd("level_" + goal.level);
                        phase = goal.then;
                        phaseTicks = 0;
                        return false;
                    }
                    if (phaseTicks == 1) {
                        stageStart("level_" + goal.level,
                            "Прокачка " + lvl + " → " + goal.level, "");
                    }
                    ProgressionState.setLevel(lvl, goal.level);
                    ProgressionState.stage("Прокачка " + lvl + " → " + goal.level,
                        "Уровень " + lvl + " / " + goal.level);
                    if (phaseTicks % TICKS_PER_LEVEL == 0) {
                        var data = Load.Unit(player);
                        int need = data.getExpRequiredForLevelUp() - data.getExp();
                        if (need < 1) {
                            need = 1;
                        }
                        int before = data.getLevel();
                        data.GiveExp(player, need);
                        int after = data.getLevel();
                        if (after > before) {
                            log(String.format(Locale.ROOT, "level_up %d -> %d", before, after));
                        }
                    }
                    return false;
                }

                case ALLOC_STATS: {
                    stageStart("alloc_stats", "Очки характеристик", "Вкладываем 3 очка");
                    if (!allocateDemo) {
                        log("skip alloc=none — очки оставлены для GUI-сценария");
                        stageEnd("alloc_stats");
                        advanceMilestone();
                        return false;
                    }
                    RPGPlayerData cap = Load.playerRPGData(player);
                    int free = cap.statPoints.getFreePoints(player);
                    List<String> coreIds = new ArrayList<>();
                    for (var stat : ExileDB.Stats().getList()) {
                        if (stat instanceof CoreStat) {
                            coreIds.add(stat.GUID());
                        }
                    }
                    coreIds.sort(String::compareTo);
                    int done = 0;
                    int want = Math.min(3, Math.min(free, coreIds.size()));
                    for (int i = 0; i < want; i++) {
                        String id = coreIds.get(i);
                        // Та же логика, что у AllocateStatPacket.onReceived.
                        cap.statPoints.map.put(id, 1 + cap.statPoints.map.getOrDefault(id, 0));
                        done++;
                        ok("alloc_stat " + id + " free_after=" + cap.statPoints.getFreePoints(player));
                    }
                    if (done > 0) {
                        cap.syncToClient(player);
                        Load.Unit(player).setEquipsChanged(true);
                        Load.Unit(player).tryRecalculateStats();
                        ProgressionState.stage("Очки характеристик", "Вложено: " + done);
                    } else {
                        bad("alloc_stat: нет свободных очков характеристик");
                    }
                    stageEnd("alloc_stats");
                    advanceMilestone();
                    return false;
                }

                case ALLOC_SPELL: {
                    stageStart("alloc_spell", "Заклинание школы", "Изучаем первое доступное");
                    if (!allocateDemo) {
                        log("skip alloc=none — очки оставлены для GUI-сценария");
                        stageEnd("alloc_spell");
                        advanceMilestone();
                        return false;
                    }
                    if (!spellLearned) {
                        EntitySpellCap.ISpellsCap spells = Load.spells(player);
                        boolean done = false;
                        if (spells != null && spells.getFreeSpellPoints() > 0) {
                            outer:
                            for (SpellSchool school : ExileDB.SpellSchools().getList()) {
                                for (String spellId : school.spells.keySet()) {
                                    Spell spell = ExileDB.Spells().get(spellId);
                                    if (spell == null) {
                                        continue;
                                    }
                                    // Та же проверка, что у AllocateSpellPacket.onReceived.
                                    if (spells.canLearn(school, spell)) {
                                        spells.getSpellsData().learnSpell(spell, school);
                                        spells.syncToClient(player);
                                        ok("learn_spell " + spellId + " school=" + school.GUID());
                                        ProgressionState.stage("Заклинание школы",
                                            "Изучено: " + spell.locName().getString());
                                        done = true;
                                        break outer;
                                    }
                                }
                            }
                        }
                        if (!done) {
                            bad("learn_spell: ни одно заклинание не изучилось (очки/уровень?)");
                        }
                        spellLearned = true;
                    }
                    stageEnd("alloc_spell");
                    advanceMilestone();
                    return false;
                }

                case ASCEND_PICK: {
                    stageStart("ascend_pick", "Вознесение: выбор пути", "Танцор Прилива");
                    if (!allocateDemo) {
                        // alloc=none: путь выберет GUI-сценарий настоящим кликом.
                        log("skip alloc=none — выбор пути оставлен для GUI-сценария");
                        stageEnd("ascend_pick");
                        advanceMilestone();
                        return false;
                    }
                    if (AscensionLogic.canPickClass(player)) {
                        boolean picked = AscensionLogic.pickClass(player, AscensionClasses.TIDE_DANCER.id);
                        if (picked) {
                            ok("ascend_pick tide_dancer");
                            ProgressionState.stage("Вознесение: выбор пути",
                                "Выбран путь: " + AscensionClasses.TIDE_DANCER.locName);
                        } else {
                            bad("ascend_pick: pickClass вернул false");
                        }
                    } else {
                        bad("ascend_pick: уровень ниже " + AscensionClasses.UNLOCK_LEVEL);
                    }
                    stageEnd("ascend_pick");
                    advanceMilestone();
                    return false;
                }

                case ASCEND_ALLOC: {
                    stageStart("ascend_alloc", "Вознесение: цепочка умений", "");
                    if (!allocateDemo) {
                        log("skip alloc=none — очки Вознесения оставлены для GUI-сценария");
                        stageEnd("ascend_alloc");
                        goPause(Phase.HOTBAR);
                        return false;
                    }
                    AscensionClasses cls = AscensionClasses.TIDE_DANCER;
                    // Цепочка линейная: идём по узлам по порядку, пока есть очки.
                    boolean moved = true;
                    while (moved) {
                        moved = false;
                        for (String spellId : cls.skillIds) {
                            if (AscensionLogic.freePoints(player) < 1) {
                                break;
                            }
                            if (AscensionLogic.canAllocate(player, spellId).isEmpty()) {
                                if (AscensionLogic.allocate(player, spellId)) {
                                    ascendAllocsDone++;
                                    AscensionData data = AscensionLogic.data(player);
                                    ok("ascend_alloc " + spellId + " rank=" + data.rankOf(spellId)
                                        + " free_after=" + AscensionLogic.freePoints(player));
                                    ProgressionState.stage("Вознесение: цепочка умений",
                                        spellId + " — ранг " + data.rankOf(spellId));
                                    moved = true;
                                }
                            }
                        }
                    }
                    if (ascendAllocsDone > 0) {
                        ok("ascend_alloc_total=" + ascendAllocsDone);
                    } else {
                        bad("ascend_alloc: не вложено ни одного очка Вознесения");
                    }
                    stageEnd("ascend_alloc");
                    goPause(Phase.HOTBAR);
                    return false;
                }

                case HOTBAR: {
                    stageStart("hotbar", "Хотбар", "Назначаем вознесённое умение");
                    if (!allocateDemo) {
                        log("skip alloc=none — хотбар назначит GUI-сценарий (ПКМ по узлу)");
                        stageEnd("hotbar");
                        goPause(Phase.VERIFY);
                        return false;
                    }
                    AscensionClasses cls = AscensionClasses.TIDE_DANCER;
                    boolean assigned = false;
                    for (String spellId : cls.skillIds) {
                        if (AscensionLogic.data(player).rankOf(spellId) > 0) {
                            int slot = AscensionLogic.firstFreeHotbarSlot(player, spellId);
                            if (slot >= 0 && AscensionLogic.assignHotbar(player, spellId, slot)) {
                                ok("hotbar " + spellId + " slot=" + slot);
                                ProgressionState.stage("Хотбар",
                                    spellId + " → ячейка " + (slot + 1));
                                assigned = true;
                                break;
                            }
                        }
                    }
                    if (!assigned) {
                        bad("hotbar: не удалось назначить ни одно вознесённое умение");
                    }
                    stageEnd("hotbar");
                    goPause(Phase.VERIFY);
                    return false;
                }

                case STAGE_PAUSE: {
                    if (phaseTicks >= STAGE_PAUSE_TICKS) {
                        phase = phaseAfterPause;
                        phaseTicks = 0;
                    }
                    return false;
                }

                case VERIFY: {
                    stageStart("verify", "Проверка итогов", "");
                    int lvl = Load.Unit(player).getLevel();
                    if (lvl >= targetLevel) {
                        ok("verify level=" + lvl);
                    } else {
                        bad("verify: уровень " + lvl + " вместо " + targetLevel);
                    }
                    AscensionData data = AscensionLogic.data(player);
                    if (allocateDemo) {
                        // В режиме alloc=none выбор пути и вложения делает
                        // GUI-сценарий кликами — здесь они ожидаемо пустые.
                        if (data.hasChosenClass()) {
                            ok("verify class=" + data.chosen_class);
                        } else {
                            bad("verify: класс Вознесения не выбран");
                        }
                        if (data.spentPoints() > 0) {
                            ok("verify ascension_points=" + data.spentPoints());
                        } else {
                            bad("verify: очки Вознесения не вложены");
                        }
                    }
                    stageEnd("verify");
                    phase = Phase.DONE;
                    return false;
                }

                case DONE: {
                    String summary = String.format(Locale.ROOT,
                        "%s done passed=%d failed=%d level=%d/%d",
                        TAG, passed.size(), failed.size(),
                        Load.Unit(player).getLevel(), targetLevel);
                    writeLines(List.of(summary));
                    writeReport(player);
                    ProgressionState.stage("Готово", "passed=" + passed.size() + " failed=" + failed.size());
                    ProgressionState.setVerdict(failed.isEmpty() ? "PASS" : "FAIL");
                    // Даём оверлею 5 секунд показать итог, потом гасим.
                    ProgressionState.end();
                    return true;
                }
            }
            return false;
        }

        private void advanceMilestone() {
            milestoneIndex++;
            goPause(milestoneIndex < milestones.size() ? Phase.LEVEL : Phase.VERIFY);
        }

        // -------------------------------------------------------------- отчёт

        void writeReport(ServerPlayer player) {
            StringBuilder md = new StringBuilder();
            md.append("# Прокачка: полный цикл\n\n");
            md.append("Целевой уровень: **").append(targetLevel).append("**, ");
            md.append("фактический: **").append(Load.Unit(player).getLevel()).append("**\n\n");
            md.append("Проверок пройдено: **").append(passed.size()).append("**, провалено: **")
                .append(failed.size()).append("**\n\n");
            md.append("## Этапы\n\n");
            for (String line : lines) {
                md.append("- `").append(line).append("`\n");
            }
            if (!failed.isEmpty()) {
                md.append("\n## Провалы\n\n");
                for (String f : failed) {
                    md.append("- ").append(f).append("\n");
                }
            }
            writeFile(REPORT_MD, md.toString());

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"target_level\": ").append(targetLevel).append(",\n");
            json.append("  \"level\": ").append(Load.Unit(player).getLevel()).append(",\n");
            json.append("  \"passed\": ").append(passed.size()).append(",\n");
            json.append("  \"failed\": ").append(failed.size()).append(",\n");
            json.append("  \"failures\": [");
            for (int i = 0; i < failed.size(); i++) {
                json.append(i == 0 ? "" : ", ").append("\"").append(failed.get(i).replace("\"", "'")).append("\"");
            }
            json.append("]\n}\n");
            writeFile(REPORT_JSON, json.toString());
        }

        private void writeFile(String name, String content) {
            try {
                Files.write(gameDir().resolve(name), content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException error) {
                LOGGER.warn("{} не удалось записать {}: {}", TAG, name, error.toString());
            }
        }
    }
}
