package com.robertx22.age_of_exile.uncommon.testing;

import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.CraftToolItems;
import com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftContainer;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftingProcessData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftedConsumableData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData;
import com.robertx22.age_of_exile.player_skills.ingredient.items.CraftToolItem;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_parts.CraftedStatsData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.saveclasses.stat_soul.SoullessGear;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
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
import java.util.Set;

/**
 * Проверяет ВСЕ профессии ремесла в живом мире: открывает настоящую сетку крафта
 * 3×2, кладёт в неё ингредиенты, жмёт на результат и смотрит, что реально
 * произошло (создался ли предмет, сколько статов дал крафт, начислился ли опыт).
 *
 * <p>Зачем: структурная проверка ({@code tools/verify/check_professions.py})
 * видит только код. Она не поймает главный класс поломок ремесла — когда
 * логика крафта конфликтует с другими системами (например, автоматическая
 * выдача бесдушных основ ломала проверку «gear has stats already» и все три
 * гир-профессии не могли крафтить вообще). Здесь — замер по факту.
 *
 * <p>Прогон состоит из двух частей:
 * <ul>
 *   <li><b>Программная</b> — те же {@code canCraft}/{@code mayPlace}/
 *       {@code craftResult}, что и у экрана, на контейнере без GUI: отказы на
 *       пустой сетке, на чужих ингредиентах, на снаряжении с душой/крафт-статами
 *       и приём обычной бесдушной основы;</li>
 *   <li><b>Живая, мышкой</b> — зонд выдаёт инструмент, основу и ингредиенты,
 *       а CI-сценарий ({@code tools/e2e/scenarios/professions.txt}) настоящими
 *       кликами раскладывает их по слотам сетки и забирает результат. Зонд
 *       наблюдает за открытым {@link ProfCraftContainer} и инвентарём и пишет,
 *       что происходит на каждом шаге.</li>
 * </ul>
 *
 * <p>Запуск: {@code /aoe profaudit all|prof=alchemy,cooking} либо флаг-файл
 * {@code aoe-profession-audit.flag} с тем же содержимым (его кладёт CI-сценарий).
 * Результат — строки {@code AOEPR ...} в логе, отчёты {@code profession-audit.md}/
 * {@code profession-audit.json} и таймлайн {@code profession-audit-timeline.txt}
 * для нарезки видео по профессиям.
 */
public final class ProfessionAuditProbe {

    public static final String TAG = "AOEPR";
    /** Флаг-файл: есть — прогон стартует сам (CI, без чата и без прав оператора). */
    public static final String FLAG_FILE = "aoe-profession-audit.flag";
    /**
     * Флаг-файл передачи хода: сценарий кладёт его, когда закончил текущую
     * профессию (тултип, Escape) — только потом зонд выдаёт предметы следующей.
     * Без этой синхронизации зонд закрывал экран крафта прямо из-под мыши
     * сценария и тултип/Escape уходили в пустоту.
     */
    private static final String NEXT_FILE = "aoe-profession-next.flag";
    /** Сколько тиков ждём флаг передачи хода, прежде чем идти дальше. */
    private static final int HANDOVER_TIMEOUT_TICKS = 1800;
    private static final String TIMELINE_FILE = "profession-audit-timeline.txt";
    private static final String REPORT_MD = "profession-audit.md";
    private static final String REPORT_JSON = "profession-audit.json";
    private static final String REPORT_LOG = "profession-audit.log";

    private static final Logger LOGGER = LoggerFactory.getLogger("AgeOfEpoch-ProfessionAudit");

    /** Порядок профессий в прогоне и в видео. */
    private static final List<PlayerSkillEnum> ORDER = List.of(
        PlayerSkillEnum.ALCHEMY,
        PlayerSkillEnum.INSCRIBING,
        PlayerSkillEnum.COOKING,
        PlayerSkillEnum.ARMOR_CRAFTING,
        PlayerSkillEnum.WEAPON_CRAFTING,
        PlayerSkillEnum.JEWEL_CRAFTING
    );

    /** Уровень игрока: нужен, чтобы съедобные зелья/еда не блокировались порогом уровня. */
    private static final int AUDIT_LEVEL = 40;
    /** Площадка в небе: вокруг GUI её не видно, но между экранами кадр чистый. */
    private static final int PLATFORM_Y = 120;

    /** Сколько тиков ждём, пока сценарий откроет GUI и разложит ингредиенты. */
    private static final int GUI_TIMEOUT_TICKS = 2400;
    /** Сколько тиков ждём финализации крафта после забора результата. */
    private static final int VERIFY_TIMEOUT_TICKS = 100;

    /**
     * Тиры ингредиентов для живого прогона. Тир 0 — самый первый уровень
     * ({@code tierToLevel(0) == 1}), поэтому «съесть» результат сможет любой игрок.
     */
    private static final int LIVE_TIER = 0;

    private static Runner active;

    private ProfessionAuditProbe() {
    }

    // ------------------------------------------------------------------ запуск

    /** {@code what} = all | prof=alchemy,cooking. */
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
            ProfessionAuditState.end();
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

    /** @return true — сценарий передал ход (флаг был и удалён). */
    private static boolean consumeNextFlag() {
        try {
            Path path = gameDir().resolve(NEXT_FILE);
            if (!Files.exists(path)) {
                return false;
            }
            Files.deleteIfExists(path);
            return true;
        } catch (IOException error) {
            return false;
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

    /** Маркер таймлайна: по нему workflow режет видео на ролики по профессиям. */
    private static void marker(String kind, String name) {
        long now = System.currentTimeMillis();
        String line = String.format(Locale.ROOT, "AOEPR_MARK %s %s %d", kind, name, now);
        LOGGER.info(line);
        try {
            Files.write(gameDir().resolve(TIMELINE_FILE),
                (line + "\n").getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    // ------------------------------------------------------------------ данные

    private enum Phase {
        SETUP,
        CHECKS,
        GIVE,
        WATCH,
        VERIFY,
        NEXT,
        WAIT_READY,
        DONE
    }

    private static final class CheckResult {
        final String name;
        final boolean ok;
        final String detail;

        CheckResult(String name, boolean ok, String detail) {
            this.name = name;
            this.ok = ok;
            this.detail = detail == null ? "" : detail;
        }
    }

    private static final class ProfResult {
        final String id;
        final String name;
        final List<CheckResult> checks = new ArrayList<>();
        final List<String> problems = new ArrayList<>();
        final List<String> notes = new ArrayList<>();
        boolean guiOpened;
        int placed;
        boolean resultReady;
        boolean taken;
        boolean crafted;
        boolean craftedOk;
        String craftedItem = "";
        int craftedStats;
        boolean expGained;

        ProfResult(PlayerSkillEnum skill) {
            this.id = skill.GUID();
            this.name = skill.word.locNameForLangFile();
        }

        String verdict() {
            return problems.isEmpty() ? "PASS" : "FAIL";
        }

        void problem(String text) {
            problems.add(text);
        }

        void note(String text) {
            notes.add(text);
        }
    }

    private static final class Runner {
        private final String what;
        private final List<PlayerSkillEnum> profs = new ArrayList<>();
        private final List<ProfResult> results = new ArrayList<>();

        private int profIndex;
        private Phase phase = Phase.SETUP;
        private int phaseTicks;

        private ProfResult current;
        /** Слот инвентаря, куда ушёл результат (там крафт финализируется); -2 — слот неизвестен. */
        private int takenSlot = -1;
        private int lastFilled;
        private boolean lastResultFilled;
        private int profExpBefore;

        Runner(String what) {
            this.what = what;
            for (PlayerSkillEnum skill : ORDER) {
                if (what.equals("all") || what.startsWith("prof=")) {
                    if (what.startsWith("prof=")) {
                        List<String> only = List.of(what.substring("prof=".length()).trim().split(","));
                        if (!only.contains(skill.GUID())) {
                            continue;
                        }
                    }
                    profs.add(skill);
                }
            }
            if (profs.isEmpty()) {
                profs.addAll(ORDER);
            }
        }

        // ------------------------------------------------------------------ шаг

        boolean step(ServerPlayer player) {
            if (!(player.level() instanceof ServerLevel level)) {
                return false;
            }
            phaseTicks++;
            try {
                return dispatch(player, level);
            } catch (Throwable error) {
                // Ошибка на шаге одной профессии не должна убивать весь прогон:
                // фиксируем её как проблему профессии и идём дальше.
                LOGGER.error("{} ошибка фазы {}", TAG, phase, error);
                writeLog(TAG + " phase_error prof=" + (current == null ? "-" : current.id)
                    + " phase=" + phase + " error=" + sanitize(String.valueOf(error)));
                if (current != null) {
                    current.problem("исключение в фазе " + phase + ": " + sanitize(String.valueOf(error)));
                    if (phase == Phase.NEXT || phase == Phase.DONE) {
                        return false;
                    }
                    phase = Phase.NEXT;
                    phaseTicks = 0;
                } else {
                    phase = Phase.CHECKS;
                    phaseTicks = 0;
                }
                return false;
            }
        }

        private boolean dispatch(ServerPlayer player, ServerLevel level) {
            switch (phase) {
                case SETUP -> {
                    consumeNextFlag(); // старый флаг от прошлого прогона
                    enterCreative(player);
                    prepareCharacter(player);
                    buildPlatform(level, player);
                    marker("audit_start", sanitize(what));
                    writeLog(TAG + " start what=" + sanitize(what) + " profs=" + profs.size());
                    ProfessionAuditState.begin("Подготовка", 0, profs.size());
                    ProfessionAuditState.step("Запись видео и подготовка",
                        "Сейчас начнётся проверка всех профессий: сетка крафта 3×2, ингредиенты и результат.");
                    phase = Phase.CHECKS;
                    phaseTicks = 0;
                }
                case CHECKS -> {
                    if (profIndex >= profs.size()) {
                        phase = Phase.DONE;
                        return false;
                    }
                    PlayerSkillEnum skill = profs.get(profIndex);
                    current = new ProfResult(skill);
                    results.add(current);
                    ProfessionAuditState.begin(current.name, profIndex + 1, profs.size());
                    ProfessionAuditState.setPhase("ПРОВЕРКИ");
                    runChecks(player, skill);
                    phase = Phase.GIVE;
                    phaseTicks = 0;
                }
                case GIVE -> {
                    PlayerSkillEnum skill = profs.get(profIndex);
                    try {
                        player.closeContainer();
                    } catch (Throwable ignored) {
                    }
                    giveLiveItems(player, skill);
                    marker("prof_start", current.id);
                    writeLog(TAG + " given prof=" + current.id
                        + " tool=" + itemId(toolFor(skill))
                        + " gear=" + (skill.isGearCraftingProf() ? "yes" : "no")
                        + " ings=3");
                    ProfessionAuditState.step("Кладём ингредиенты в сетку 3×2",
                        "Сценарий мышкой раскладывает ингредиенты по слотам и забирает результат.");
                    ProfessionAuditState.setPhase("СЕТКА");
                    phase = Phase.WATCH;
                    phaseTicks = 0;
                    lastFilled = 0;
                    lastResultFilled = false;
                    takenSlot = -1;
                    profExpBefore = expSnapshot(player, skill);
                }
                case WATCH -> {
                    watchGui(player);
                    if (takenSlot != -1) {
                        phase = Phase.VERIFY;
                        phaseTicks = 0;
                    } else if (phaseTicks > GUI_TIMEOUT_TICKS) {
                        current.problem("таймаут: сценарий не собрал крафт за "
                            + (GUI_TIMEOUT_TICKS / 20) + " с");
                        phase = Phase.NEXT;
                        phaseTicks = 0;
                    }
                }
                case VERIFY -> {
                    if (verifyCraft(player, profs.get(profIndex))) {
                        phase = Phase.NEXT;
                        phaseTicks = 0;
                    } else if (phaseTicks > VERIFY_TIMEOUT_TICKS) {
                        current.problem("предмет с процессом крафта не финализировался за "
                            + (VERIFY_TIMEOUT_TICKS / 20) + " с");
                        phase = Phase.NEXT;
                        phaseTicks = 0;
                    }
                }
                case NEXT -> {
                    if (!current.taken && current.resultReady) {
                        current.problem("результат был готов, но его не забрали");
                    }
                    if (!current.resultReady && current.placed > 0) {
                        current.problem("ингредиенты разложены, но результат не появился");
                    }
                    if (current.placed == 0) {
                        current.problem("ни один предмет не был положен в сетку");
                    }
                    ProfessionAuditState.setPhase("ИТОГ");
                    ProfessionAuditState.setVerdict(current.verdict() + ": "
                        + (current.problems.isEmpty()
                        ? "создано " + current.craftedItem + ", статов " + current.craftedStats
                        : String.join("; ", current.problems)));
                    writeLog(TAG + " prof=" + current.id
                        + " verdict=" + current.verdict()
                        + " checks=" + current.checks.size()
                        + " gui=" + current.guiOpened
                        + " placed=" + current.placed
                        + " result=" + current.resultReady
                        + " taken=" + current.taken
                        + " crafted=" + current.craftedOk
                        + " item=" + (current.craftedItem.isEmpty() ? "-" : current.craftedItem)
                        + " stats=" + current.craftedStats
                        + " exp=" + current.expGained
                        + (current.problems.isEmpty() ? "" : " problems=" + String.join("|", current.problems)));
                    marker("prof_end", current.id);
                    writeReports();
                    profIndex++;
                    current = null;
                    phase = Phase.WAIT_READY;
                    phaseTicks = 0;
                }
                case WAIT_READY -> {
                    // Сценарий дописывает тултип и закрывает экран сам; флаг
                    // «next» означает, что можно выдавать предметы следующей
                    // профессии. Таймаут — подстраховка, чтобы прогон не встал.
                    if (consumeNextFlag() || phaseTicks > HANDOVER_TIMEOUT_TICKS) {
                        phase = Phase.CHECKS;
                        phaseTicks = 0;
                    }
                }
                case DONE -> {
                    return finish(player);
                }
            }
            return false;
        }

        // ------------------------------------------------------------------ проверки (без GUI)

        private void runChecks(ServerPlayer player, PlayerSkillEnum skill) {
            check("tool_registered", toolFor(skill) != ItemStack.EMPTY
                    && toolFor(skill).getItem() instanceof CraftToolItem t && t.skill == skill,
                itemId(toolFor(skill)));

            List<SlashIngredient> allowed = ingredientsFor(skill);
            check("ingredients_available", allowed.size() >= 3,
                "доступно ингредиентов: " + allowed.size());

            SlashIngredient matrix = soulMatrix();
            List<SlashIngredient> regular = regularIngredients(skill);
            check("has_regular_ingredients", matrix != null && regular.size() >= 2,
                "soul_matrix=" + (matrix != null) + ", обычных=" + regular.size());
            if (matrix == null || regular.size() < 2) {
                // Без ингредиентов остальные проверки сетки бессмысленны.
                return;
            }

            ItemStack gear = skill.isGearCraftingProf() ? baseGearFor(skill) : ItemStack.EMPTY;
            if (skill.isGearCraftingProf()) {
                check("gear_base_found", !gear.isEmpty(), itemId(gear));
                GearSlot slot = GearSlot.getSlotOf(gear.getItem());
                check("gear_slot_matches", gearMatches(skill, gear),
                    slot == null ? "нет слота" : slot.GUID());
            }

            // Контейнер без GUI: те же слоты, mayPlace и canCraft, что у экрана.
            ProfCraftContainer menu =
                new ProfCraftContainer(toolFor(skill), 1, player.getInventory(), InteractionHand.MAIN_HAND);
            int start = menu.getContainerStart();

            check("grid_empty_fails", menu.getFailReason() != null,
                menu.getFailReason() == null ? "" : plain(menu.getFailReason()));
            check("junk_rejected", !menu.slots.get(start).mayPlace(new ItemStack(Items.STICK)),
                "палка в слот не должна проходить");

            ItemStack ingOne = buildIngredient(regular.get(0), LIVE_TIER);
            check("ingredient_accepted", menu.slots.get(start).mayPlace(ingOne), regular.get(0).GUID());

            if (skill.isGearCraftingProf()) {
                check("gear_accepted", menu.slots.get(start).mayPlace(gear.copy()), itemId(gear));

                // Регрессия главного фикса: обычная бесдушная основа обязана проходить,
                // хотя у неё уже есть GEAR_ITEM_DATA (его выдаёт SoullessGear.ensure).
                ItemStack soulless = gear.copy();
                GearItemData base = SoullessGear.createFor(soulless, 10);
                if (base != null) {
                    Gear.Save(soulless, base);
                }
                menu.slots.get(start).set(soulless);
                menu.slots.get(start + 1).set(ingOne.copy());
                boolean soullessOk = menu.getFailReason() == null && !menu.getResultPreview().isEmpty();
                check("soulless_gear_accepted", soullessOk,
                    soullessOk ? "бесдушная основа проходит" : "отказ: " + safeReason(menu));
                menu.slots.get(start).set(ItemStack.EMPTY);
                menu.slots.get(start + 1).set(ItemStack.EMPTY);

                // Душа (не бесдушная основа) — отказ.
                ItemStack souled = gear.copy();
                GearItemData soul = SoullessGear.createFor(souled, 10);
                if (soul != null) {
                    soul.sl = false; // «наложенная душа»
                    Gear.Save(souled, soul);
                }
                menu.slots.get(start).set(souled);
                menu.slots.get(start + 1).set(ingOne.copy());
                check("souled_gear_rejected", menu.getFailReason() != null, safeReason(menu));
                menu.slots.get(start).set(ItemStack.EMPTY);
                menu.slots.get(start + 1).set(ItemStack.EMPTY);

                // Основа с уже созданными крафт-статами — отказ (их стёр бы перекрафт).
                ItemStack crafted = gear.copy();
                GearItemData withStats = SoullessGear.createFor(crafted, 10);
                if (withStats != null) {
                    withStats.cr = new CraftedStatsData();
                    Gear.Save(crafted, withStats);
                }
                menu.slots.get(start).set(crafted);
                menu.slots.get(start + 1).set(ingOne.copy());
                check("crafted_stats_rejected", menu.getFailReason() != null, safeReason(menu));
                menu.slots.get(start).set(ItemStack.EMPTY);
                menu.slots.get(start + 1).set(ItemStack.EMPTY);
            }

            // Чужой ингредиент обязан заворачивать canCraft (shift-click путь).
            SlashIngredient wrong = wrongIngredientFor(skill);
            if (wrong != null) {
                menu.slots.get(start).set(buildIngredient(wrong, LIVE_TIER));
                check("wrong_prof_rejected", menu.getFailReason() != null, wrong.GUID());
                menu.slots.get(start).set(ItemStack.EMPTY);
            } else {
                check("wrong_prof_rejected", false, "нет ингредиента вне профессии");
            }

            // Полный рецепт: как его соберёт сценарий мышкой.
            if (skill.isGearCraftingProf()) {
                menu.slots.get(start).set(gear.copy());
            }
            menu.slots.get(start + 1).set(buildIngredient(matrix, LIVE_TIER));
            menu.slots.get(start + 2).set(buildIngredient(regular.get(0), LIVE_TIER));
            menu.slots.get(start + 3).set(buildIngredient(regular.get(1), LIVE_TIER));

            boolean fullOk = menu.getFailReason() == null && !menu.getResultPreview().isEmpty();
            check("full_recipe_works", fullOk, fullOk ? "" : safeReason(menu));

            ItemStack preview = menu.getResultPreview();
            CraftingProcessData pdata = StackSaving.CRAFT_PROCESS.loadFrom(preview);
            if (pdata != null) {
                // В сетке 3 ингредиента (у гир-профессий 4-й предмет — основа,
                // она ингредиентом не считается).
                check("result_process_saved", pdata.getIngredientsCount() == 3,
                    "ингредиентов в процессе: " + pdata.getIngredientsCount());
                int chance = pdata.getSuccessChance(player);
                check("success_chance_100", chance == 100, "шанс успеха: " + chance + "%");
                float multi = pdata.getStatMulti();
                check("stat_multi_sane", multi > 0F && multi <= 1.001F,
                    "множитель: " + Math.round(multi * 100) + "%");
            } else {
                check("result_process_saved", false, "на результате нет данных процесса крафта");
                check("success_chance_100", false, "нет процесса крафта");
                check("stat_multi_sane", false, "нет процесса крафта");
            }

            // Выход крафта напрямую: статы должны появиться.
            try {
                if (skill.isGearCraftingProf() && pdata != null) {
                    GearItemData out = pdata.craftGear(gear.copy(), player);
                    int stats = out != null && out.cr != null ? out.cr.stats.size() : 0;
                    check("crafted_output_valid", out != null && out.cr != null && !out.cr.stats.isEmpty(),
                        "бесдушная основа со статами: " + stats);
                } else if (pdata != null) {
                    CraftedConsumableData out = pdata.craftConsumable(player);
                    check("crafted_output_valid", out != null && !out.stats.isEmpty(),
                        "статов в расходуемом: " + (out == null ? 0 : out.stats.size()));
                } else {
                    check("crafted_output_valid", false, "нет процесса крафта");
                }
            } catch (Throwable error) {
                check("crafted_output_valid", false, "исключение: " + error);
            }
        }

        private void check(String name, boolean ok, String detail) {
            current.checks.add(new CheckResult(name, ok, detail));
            if (!ok) {
                current.problem("проверка " + name + " провалилась"
                    + (detail == null || detail.isEmpty() ? "" : " (" + detail + ")"));
            }
            writeLog(TAG + " check prof=" + current.id
                + " name=" + name
                + " ok=" + ok
                + " detail=" + sanitize(detail));
        }

        // ------------------------------------------------------------------ живой прогон (мышкой)

        private void giveLiveItems(ServerPlayer player, PlayerSkillEnum skill) {
            player.getInventory().clearContent();
            PlayerSkillData data = Load.playerRPGData(player).professions.getDataFor(skill);
            data.setLvl(1); // опыт за крафт должен начисляться (иначе упирается в потолок уровня)

            // Инструмент — в руку: setItemSlot кладёт его в выбранный слот хотбара,
            // а выбор слота нам безразличен (слоты хотбара сценарий не кликает).
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, toolFor(skill));
            // Предметы — в главный инвентарь (слоты 9..12), а не в хотбар:
            // их координаты в GUI фиксированы и не зависят от выбранного слота.
            if (skill.isGearCraftingProf()) {
                player.getInventory().setItem(9, baseGearFor(skill));
            }
            player.getInventory().setItem(10, buildIngredient(soulMatrix(), LIVE_TIER));
            List<SlashIngredient> regular = regularIngredients(skill);
            player.getInventory().setItem(11, buildIngredient(regular.get(0), LIVE_TIER));
            player.getInventory().setItem(12, buildIngredient(regular.get(1), LIVE_TIER));
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();

            // Смотрим в небо: правый клик гарантированно попадает в воздух и
            // открывает менё инструмента, а не взаимодействует с блоком.
            player.setYRot(0F);
            player.setXRot(-90F);
        }

        /** Следит за открытым контейнером крафта и инвентарём. */
        private void watchGui(ServerPlayer player) {
            if (!(player.containerMenu instanceof ProfCraftContainer menu)) {
                return; // экран крафта закрыт: всё наблюдение идёт только по нему
            }
            if (!current.guiOpened) {
                current.guiOpened = true;
                writeLog(TAG + " gui_open prof=" + current.id);
            }
            int start = menu.getContainerStart();
            int filled = 0;
            for (int i = 0; i < 6; i++) {
                if (!menu.slots.get(start + i).getItem().isEmpty()) {
                    filled++;
                }
            }
            if (filled != lastFilled) {
                current.placed = Math.max(current.placed, filled);
                ProfessionAuditState.setMetrics("Занято слотов: " + filled + "/6");
                ItemStack last = ItemStack.EMPTY;
                for (int i = 0; i < 6; i++) {
                    ItemStack s = menu.slots.get(start + i).getItem();
                    if (!s.isEmpty()) {
                        last = s;
                    }
                }
                writeLog(TAG + " placed prof=" + current.id
                    + " filled=" + filled
                    + " item=" + itemId(last));
            }

            boolean resultFilled = !menu.getResultPreview().isEmpty();
            if (resultFilled && !lastResultFilled) {
                current.resultReady = true;
                CraftingProcessData pdata = StackSaving.CRAFT_PROCESS.loadFrom(menu.getResultPreview());
                int chance = pdata != null ? pdata.getSuccessChance(player) : -1;
                ProfessionAuditState.setPhase("РЕЗУЛЬТАТ");
                ProfessionAuditState.setMetrics("Результат готов, шанс успеха: " + chance + "%");
                writeLog(TAG + " result_ready prof=" + current.id
                    + " item=" + itemId(menu.getResultPreview())
                    + " chance=" + chance);
            }

            // Результат забрали. Основной признак — в инвентаре появился стак с
            // процессом крафта. Но GearSoulOnInvTick может финализировать его в
            // том же тике (его действие выполняется раньше), поэтому есть и
            // запасной признак: результат и сетка опустели одновременно — значит
            // onTake забрал результат и списал ингредиенты.
            if (takenSlot == -1 && current.resultReady) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && StackSaving.CRAFT_PROCESS.has(stack)) {
                        takenSlot = i;
                        break;
                    }
                }
                if (takenSlot == -1 && lastResultFilled && lastFilled > 0 && filled == 0
                    && !resultFilled) {
                    takenSlot = -2; // запасной признак, слот неизвестен
                }
                if (takenSlot != -1) {
                    current.taken = true;
                    PlayerSkillData data =
                        Load.playerRPGData(player).professions.getDataFor(profs.get(profIndex));
                    profExpBefore = data.getExp() + data.getLvl() * 100000;
                    ProfessionAuditState.setPhase("ПРОВЕРКА");
                    ProfessionAuditState.setMetrics("Результат забран, ждём финализацию крафта");
                    writeLog(TAG + " taken prof=" + current.id
                        + " slot=" + takenSlot);
                }
            }

            lastFilled = filled;
            lastResultFilled = resultFilled;
        }

        /** @return true — крафт разрешился (успех или провал), можно судить. */
        private boolean verifyCraft(ServerPlayer player, PlayerSkillEnum skill) {
            // Пока где-то в инвентаре есть незавершённый процесс — ждём.
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && StackSaving.CRAFT_PROCESS.has(stack)) {
                    return false;
                }
            }
            // Ищем готовый выход среди всего инвентаря (слот мог смениться).
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (skill.isGearCraftingProf()) {
                    GearItemData gear = Gear.Load(stack);
                    if (gear != null && gear.cr != null && !gear.cr.stats.isEmpty()) {
                        finishCraft(player, skill, stack, gear.cr.stats.size(), true);
                        return true;
                    }
                } else {
                    CraftedConsumableData data = StackSaving.CRAFTED_CONSUMABLE.loadFrom(stack);
                    if (data != null && !data.stats.isEmpty()) {
                        finishCraft(player, skill, stack, data.stats.size(), true);
                        return true;
                    }
                }
            }
            // Порох = крафт провалился (или упал с исключением).
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(Items.GUNPOWDER)) {
                    current.crafted = true;
                    current.craftedOk = false;
                    current.problem("крафт провалился (вернулся порох)");
                    writeLog(TAG + " crafted prof=" + current.id + " ok=false item=gunpowder");
                    return true;
                }
            }
            return false;
        }

        private void finishCraft(ServerPlayer player, PlayerSkillEnum skill,
                                 ItemStack stack, int stats, boolean ok) {
            current.crafted = true;
            current.craftedOk = ok;
            current.craftedItem = itemId(stack);
            current.craftedStats = stats;
            PlayerSkillData after = Load.playerRPGData(player).professions.getDataFor(skill);
            int expAfter = after.getExp() + after.getLvl() * 100000;
            current.expGained = expAfter != profExpBefore;
            if (!current.expGained) {
                current.note("опыт профессии не изменился");
            }
            writeLog(TAG + " crafted prof=" + current.id
                + " ok=" + current.craftedOk
                + " item=" + (current.craftedItem.isEmpty() ? "-" : current.craftedItem)
                + " stats=" + current.craftedStats
                + " exp_gain=" + current.expGained);
            ProfessionAuditState.setMetrics("Создано: " + current.craftedItem
                + ", статов: " + current.craftedStats);
        }

        // ------------------------------------------------------------------ финал

        private boolean finish(ServerPlayer player) {
            long passed = results.stream().filter(r -> r.problems.isEmpty()).count();
            long failed = results.size() - passed;
            writeLog(String.format(Locale.ROOT, "%s done profs=%d passed=%d failed=%d",
                TAG, results.size(), passed, failed));
            marker("audit_end", "profs_" + results.size() + "_failed_" + failed);
            writeReports();
            ProfessionAuditState.end();
            restoreGameMode(player);
            return true;
        }

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
            long passed = results.stream().filter(r -> r.problems.isEmpty()).count();
            md.append("# Живой аудит профессий\n\n");
            md.append(String.format(Locale.ROOT,
                "Проверено: **%d**, прошло: **%d**, провалилось: **%d**\n\n",
                results.size(), passed, results.size() - passed));
            md.append("Каждая профессия проверялась двумя путями: программно (те же "
                + "`canCraft`/`mayPlace`/`craftResult`, что и у экрана крафта) и живым "
                + "прогоном — настоящий экран 3×2, ингредиенты раскладывались мышкой "
                + "по слотам, результат забирался и финализировался в инвентаре.\n\n");
            md.append("| профессия | итог | проверок | сложено в сетку | создано | статов | опыт | проблемы |\n");
            md.append("|---|---|---|---|---|---|---|---|\n");
            for (ProfResult r : results) {
                md.append("| ").append(escape(r.name))
                    .append(" | ").append(r.verdict())
                    .append(" | ").append(r.checks.size())
                    .append(" | ").append(r.placed)
                    .append(" | ").append(escape(r.craftedItem.isEmpty() ? "—" : r.craftedItem))
                    .append(" | ").append(r.craftedStats)
                    .append(" | ").append(r.expGained ? "да" : "нет")
                    .append(" | ").append(r.problems.isEmpty() ? "—" : escape(String.join("; ", r.problems)))
                    .append(" |\n");
            }
            md.append("\n## Проверки по профессиям\n\n");
            for (ProfResult r : results) {
                md.append("### ").append(escape(r.name)).append(" — ").append(r.verdict()).append("\n\n");
                md.append("| проверка | итог | детали |\n|---|---|---|\n");
                for (CheckResult c : r.checks) {
                    md.append("| `").append(escape(c.name)).append("` | ")
                        .append(c.ok ? "OK" : "**FAIL**")
                        .append(" | ").append(escape(c.detail)).append(" |\n");
                }
                md.append("\n");
            }
            return md.toString();
        }

        private String json() {
            StringBuilder out = new StringBuilder();
            out.append("{\n  \"professions\": [\n");
            for (int i = 0; i < results.size(); i++) {
                ProfResult r = results.get(i);
                out.append("    {");
                out.append("\"id\": \"").append(jsonEscape(r.id)).append("\", ");
                out.append("\"name\": \"").append(jsonEscape(r.name)).append("\", ");
                out.append("\"verdict\": \"").append(r.verdict()).append("\", ");
                out.append("\"gui\": ").append(r.guiOpened).append(", ");
                out.append("\"placed\": ").append(r.placed).append(", ");
                out.append("\"result\": ").append(r.resultReady).append(", ");
                out.append("\"taken\": ").append(r.taken).append(", ");
                out.append("\"crafted\": ").append(r.craftedOk).append(", ");
                out.append("\"item\": \"").append(jsonEscape(r.craftedItem)).append("\", ");
                out.append("\"stats\": ").append(r.craftedStats).append(", ");
                out.append("\"exp\": ").append(r.expGained).append(", ");
                out.append("\"checks\": [");
                for (int j = 0; j < r.checks.size(); j++) {
                    CheckResult c = r.checks.get(j);
                    out.append("{\"name\": \"").append(jsonEscape(c.name))
                        .append("\", \"ok\": ").append(c.ok)
                        .append(", \"detail\": \"").append(jsonEscape(c.detail)).append("\"}");
                    if (j < r.checks.size() - 1) {
                        out.append(", ");
                    }
                }
                out.append("], ");
                out.append("\"problems\": [");
                for (int j = 0; j < r.problems.size(); j++) {
                    out.append("\"").append(jsonEscape(r.problems.get(j))).append("\"");
                    if (j < r.problems.size() - 1) {
                        out.append(", ");
                    }
                }
                out.append("]}");
                if (i < results.size() - 1) {
                    out.append(",");
                }
                out.append("\n");
            }
            out.append("  ]\n}\n");
            return out.toString();
        }

        // ------------------------------------------------------------------ окружение

        private void enterCreative(ServerPlayer player) {
            try {
                player.setGameMode(GameType.CREATIVE);
                writeLog(TAG + " режим: creative (инструмент и ингредиенты выдаются без выживания)");
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

        private void prepareCharacter(ServerPlayer player) {
            try {
                Load.Unit(player).setLevel(AUDIT_LEVEL);
                writeLog(TAG + " уровень игрока выставлен на " + AUDIT_LEVEL);
            } catch (Throwable error) {
                writeLog(TAG + " не удалось выставить уровень: " + error);
            }
        }

        /** Ровная площадка в небе: между открытиями GUI в кадре чистый мир. */
        private void buildPlatform(ServerLevel level, ServerPlayer player) {
            try {
                int x = player.blockPosition().getX();
                int z = player.blockPosition().getZ();
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -6; dz <= 6; dz++) {
                        level.setBlock(new net.minecraft.core.BlockPos(x + dx, PLATFORM_Y, z + dz),
                            Blocks.QUARTZ_BLOCK.defaultBlockState(), 3);
                        for (int dy = 1; dy <= 8; dy++) {
                            level.setBlock(new net.minecraft.core.BlockPos(x + dx, PLATFORM_Y + dy, z + dz),
                                Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
                player.teleportTo(level, x + 0.5D, PLATFORM_Y + 1, z + 0.5D, Set.of(), 0F, -90F, false);
            } catch (Throwable error) {
                writeLog(TAG + " площадка не построилась: " + error);
            }
        }
    }

    // ------------------------------------------------------------------ помощники

    private static ItemStack toolFor(PlayerSkillEnum skill) {
        try {
            if (skill == PlayerSkillEnum.ALCHEMY) {
                return new ItemStack(CraftToolItems.ALCHEMY.get());
            }
            if (skill == PlayerSkillEnum.COOKING) {
                return new ItemStack(CraftToolItems.COOKING.get());
            }
            if (skill == PlayerSkillEnum.INSCRIBING) {
                return new ItemStack(CraftToolItems.INSCRIBING.get());
            }
            if (skill == PlayerSkillEnum.ARMOR_CRAFTING) {
                return new ItemStack(CraftToolItems.ARMOR.get());
            }
            if (skill == PlayerSkillEnum.WEAPON_CRAFTING) {
                return new ItemStack(CraftToolItems.WEAPON.get());
            }
            if (skill == PlayerSkillEnum.JEWEL_CRAFTING) {
                return new ItemStack(CraftToolItems.JEWEL.get());
            }
        } catch (Throwable ignored) {
        }
        return ItemStack.EMPTY;
    }

    /** Та же основа, что и у тулипа профессии: броня/меч/кольцо. */
    private static ItemStack baseGearFor(PlayerSkillEnum skill) {
        try {
            Item item = skill.getToolCraftItem();
            return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean gearMatches(PlayerSkillEnum skill, ItemStack stack) {
        try {
            return skill.gearMatchesProfession(stack.getItem());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<SlashIngredient> ingredientsFor(PlayerSkillEnum skill) {
        List<SlashIngredient> list = new ArrayList<>();
        try {
            for (SlashIngredient ing : com.robertx22.age_of_exile.database.registry.ExileDB.Ingredients().getList()) {
                if (ing != null && ing.isAllowedInProfession(skill.GUID())) {
                    list.add(ing);
                }
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    private static List<SlashIngredient> regularIngredients(PlayerSkillEnum skill) {
        List<SlashIngredient> list = new ArrayList<>();
        for (SlashIngredient ing : ingredientsFor(skill)) {
            if (!ing.isOneOfAKind() && !"soul_matrix".equals(ing.GUID())) {
                list.add(ing);
            }
        }
        return list;
    }

    private static SlashIngredient soulMatrix() {
        try {
            for (SlashIngredient ing : com.robertx22.age_of_exile.database.registry.ExileDB.Ingredients().getList()) {
                if (ing != null && "soul_matrix".equals(ing.GUID())) {
                    return ing;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SlashIngredient wrongIngredientFor(PlayerSkillEnum skill) {
        try {
            for (SlashIngredient ing : com.robertx22.age_of_exile.database.registry.ExileDB.Ingredients().getList()) {
                if (ing != null && ing.getItem() != null && ing.getItem() != Items.AIR
                    && !ing.isAllowedInProfession(skill.GUID())) {
                    return ing;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ItemStack buildIngredient(SlashIngredient ing, int tier) {
        if (ing == null) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack stack = new ItemStack(ing.getItem());
            StackSaving.INGREDIENTS.saveTo(stack, IngredientData.of(ing, tier));
            return stack;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static int expSnapshot(ServerPlayer player, PlayerSkillEnum skill) {
        PlayerSkillData data = Load.playerRPGData(player).professions.getDataFor(skill);
        return data.getExp() + data.getLvl() * 100000;
    }

    private static String safeReason(ProfCraftContainer menu) {
        try {
            Component reason = menu.getFailReason();
            return reason == null ? "" : plain(reason);
        } catch (Throwable error) {
            return "исключение: " + error;
        }
    }

    private static String plain(Component component) {
        try {
            return sanitize(component.getString());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String itemId(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) {
                return "-";
            }
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                .toString();
        } catch (Throwable ignored) {
            return "-";
        }
    }

    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\n\\r|]", " ").trim();
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("|", "/").replace("\n", " ");
    }

    private static String jsonEscape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
