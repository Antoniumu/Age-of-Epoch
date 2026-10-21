package com.robertx22.age_of_exile.guides;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.capability.player.EntitySpellCap;
import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.data.perks.Perk;
import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.synergy.Synergy;
import com.robertx22.age_of_exile.database.data.talent_tree.TalentTree;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.PointData;
import com.robertx22.age_of_exile.saveclasses.perks.TalentsData;
import com.robertx22.age_of_exile.saveclasses.spells.SpellsData;
import com.robertx22.age_of_exile.simulation.SimulationManager;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.questbook.QuestBookService;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Советник гайдов.
 *
 * Одна и та же логика работает и на клиенте (подсветка кнопок), и на сервере
 * (сборка билда на Полигоне). Гайд никогда не «ломается» из-за отклонений:
 * он каждый раз пересчитывает ближайший шаг от ТЕКУЩЕГО состояния персонажа.
 */
public class GuideAdvisor {

    public static final String TALENT_TREE_ID = "talents";
    public static final int MAX_BRIDGE_STEPS = 4;

    // ------------------------------------------------------------------ охват гайда
    /** До какого уровня гайд ведёт игрока (настраивается хостом, по умолчанию 20). */
    public static int maxLevel() {
        return GuideMath.guideMaxLevel();
    }

    /** Персонаж уже прошёл гайд: подсказки и подсветка выключены. */
    public static boolean isBeyondGuide(Player player) {
        if (player == null) {
            return true;
        }
        try {
            return GuideMath.beyondGuide(Load.Unit(player)
                .getLevel());
        } catch (Exception e) {
            return false;
        }
    }

    /** Строка «STR 45%, VIT 35%, DEX 20%» — что держать после гайда. */
    public static String statWeightsLine(GuideDefinition guide) {
        if (guide == null) {
            return "";
        }
        float sum = 0;
        for (GuideDefinition.StatWeight w : guide.stats) {
            sum += w.weight;
        }
        StringBuilder sb = new StringBuilder();
        for (GuideDefinition.StatWeight w : guide.stats) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            int pct = sum <= 0 ? 0 : Math.round(w.weight / sum * 100F);
            sb.append(GuideMath.shortName(w.stat))
                .append(" ")
                .append(pct)
                .append("%");
        }
        return sb.toString();
    }

    /** Текст «гайд закончился» — один и тот же в окне, чате и подсказках. */
    public static String finishedText() {
        return "Гайд пройден до ур. " + maxLevel() + " — дальше прокачивайтесь сами";
    }

    // ------------------------------------------------------------------ активный гайд
    public static GuideDefinition activeGuide(Player player) {
        if (player == null) {
            return null;
        }
        try {
            return GuideData.byId(Load.playerRPGData(player).active_guide);
        } catch (Exception e) {
            return null;
        }
    }

    public static String activeGuideId(Player player) {
        GuideDefinition g = activeGuide(player);
        return g == null ? "" : g.id;
    }

    public static void setActiveGuide(Player player, String guideId) {
        if (player == null) {
            return;
        }
        RPGPlayerData data = Load.playerRPGData(player);
        GuideDefinition guide = GuideData.byId(guideId);
        data.active_guide = guide == null ? "" : guide.id;
        data.syncToClient(player);
        if (guide == null) {
            player.sendSystemMessage(Component.literal("§7[Гайд] Гайд отключён."));
            return;
        }
        player.sendSystemMessage(Component.literal("§a[Гайд] Активен: §f" + guide.name
            + "§a. Подсказки подсвечиваются у характеристик, талантов и заклинаний."));
        player.sendSystemMessage(Component.literal("§7Гайд ведёт до ур. " + maxLevel()
            + " — дальше прокачивайтесь сами."));
        player.sendSystemMessage(Component.literal("§7На Полигоне этот гайд собирается в готовую сборку целиком."));

        // советы советника — в чат, чтобы длинный текст не обрезался в окне
        try {
            Progress progress = progress(player);
            int shown = 0;
            for (String advice : progress.advice) {
                if (shown++ >= 3) {
                    break;
                }
                player.sendSystemMessage(Component.literal(advice));
            }
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ характеристики
    public static Map<String, Integer> currentStats(Player player) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (player == null) {
            return map;
        }
        try {
            map.putAll(Load.playerRPGData(player).statPoints.map);
        } catch (Exception ignored) {
        }
        return map;
    }

    public static int spentStatPoints(Player player) {
        int spent = 0;
        for (int v : currentStats(player).values()) {
            spent += v;
        }
        return spent;
    }

    /**
     * Какое очко характеристики вложить следующим.
     *
     * Считаем идеальную долю от ВСЕГО бюджета уровня (вложенные + свободные очки),
     * затем выбираем стат с самым большим дефицитом. Так гайд подстраивается
     * под игрока, который уже вложил часть очков «не туда».
     */
    public static String recommendedStat(Player player) {
        GuideDefinition guide = activeGuide(player);
        if (guide == null || player == null) {
            return null;
        }
        if (isBeyondGuide(player)) {
            return null;
        }
        try {
            RPGPlayerData data = Load.playerRPGData(player);
            if (data.statPoints.getFreePoints(player) <= 0) {
                return null;
            }
            int level = Load.Unit(player).getLevel();
            int budget = GuideMath.statBudget(GuideMath.cappedLevel(level));
            Map<String, Integer> ideal = GuideMath.idealSplit(guide, budget);
            Map<String, Integer> current = currentStats(player);

            String best = null;
            int bestDeficit = Integer.MIN_VALUE;
            for (GuideDefinition.StatWeight w : guide.stats) {
                int deficit = ideal.getOrDefault(w.stat, 0) - current.getOrDefault(w.stat, 0);
                if (deficit > bestDeficit) {
                    bestDeficit = deficit;
                    best = w.stat;
                }
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isRecommendedStat(Player player, String statId) {
        String rec = recommendedStat(player);
        return rec != null && rec.equals(statId);
    }

    // ------------------------------------------------------------------ таланты
    public static TalentTree talentTree() {
        try {
            if (ExileDB.TalentTrees().isRegistered(TALENT_TREE_ID)) {
                return ExileDB.TalentTrees().get(TALENT_TREE_ID);
            }
            List<TalentTree> all = ExileDB.TalentTrees().getList();
            return all.isEmpty() ? null : all.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<PointData> allocatedPerks(Player player, TalentTree tree) {
        List<PointData> list = new ArrayList<>();
        if (player == null || tree == null) {
            return list;
        }
        try {
            list.addAll(Load.playerRPGData(player).talents.getSchool(tree).getAllocatedPoints(tree));
        } catch (Exception ignored) {
        }
        return list;
    }

    public static List<PointData> allocatedPerks(Player player) {
        return allocatedPerks(player, talentTree());
    }

    public static boolean isPlanTalent(Player player, PointData point) {
        GuideDefinition guide = activeGuide(player);
        return guide != null && guide.isPlanPoint(point);
    }

    // кэш: 688 кнопок древа перерисовываются каждый кадр
    private static Object talentCachePlayer = null;
    private static long talentCacheTime = 0;
    private static String talentCacheSignature = "";
    private static PointData talentCachePoint = null;
    private static int talentCacheIndex = -1;

    /** Следующий узел древа, который надо взять (или null). */
    public static PointData recommendedTalent(Player player) {
        if (player == null) {
            return null;
        }
        GuideDefinition guide = activeGuide(player);
        if (guide == null) {
            return null;
        }
        TalentTree tree = talentTree();
        if (tree == null) {
            return null;
        }
        if (isBeyondGuide(player)) {
            return null;
        }
        List<PointData> allocated = allocatedPerks(player, tree);
        int level = Load.Unit(player).getLevel();
        String signature = player.getUUID() + "|" + guide.id + "|" + level + "|" + allocated.size();
        long now = System.currentTimeMillis();
        if (talentCachePlayer == player && signature.equals(talentCacheSignature)
            && now - talentCacheTime < 250) {
            return talentCachePoint;
        }

        PointData result = computeRecommendedTalent(player, guide, tree, allocated, level);
        int index = result == null ? -1 : guide.indexOfPoint(result);

        talentCachePlayer = player;
        talentCacheSignature = signature;
        talentCacheTime = now;
        talentCachePoint = result;
        talentCacheIndex = index;
        return result;
    }

    /** Индекс рекомендуемого узла в плане (-1 если рекомендации нет). */
    public static int recommendedTalentIndex(Player player) {
        recommendedTalent(player);
        return talentCacheIndex;
    }

    public static boolean isRecommendedTalent(Player player, PointData point) {
        PointData rec = recommendedTalent(player);
        return rec != null && rec.equals(point);
    }

    private static PointData computeRecommendedTalent(Player player, GuideDefinition guide,
                                                      TalentTree tree, List<PointData> allocated, int level) {
        int inScope = GuideMath.talentStepsInScope(guide);
        if (inScope <= 0) {
            return null;
        }
        int free;
        try {
            free = Load.playerRPGData(player).talents.getFreePoints(Load.Unit(player), tree.getSchool_type());
        } catch (Exception e) {
            return null;
        }
        if (free <= 0) {
            return null;
        }
        Set<PointData> taken = new HashSet<>(allocated);

        // ничего не взято — рекомендуем стартовый путь гайда
        if (taken.isEmpty()) {
            PointData start = guide.pointAt(0);
            return tree.calcData.getPerk(start) == null ? null : start;
        }

        // если выбран другой стартовый путь, план недостижим (one_of_a_kind = start)
        if (!isStartCompatible(guide, taken, tree)) {
            return null;
        }

        for (int i = 0; i < inScope; i++) {
            PointData point = guide.pointAt(i);
            if (point == null || taken.contains(point)) {
                continue;
            }
            if (tree.calcData.getPerk(point) == null) {
                continue;
            }
            if (GuideDefinition.levelNeededForTalentStep(i) > level) {
                return null; // ещё нет нужного количества очков талантов
            }
            if (isAdjacent(tree, point, taken)) {
                return point;
            }
            List<PointData> path = pathTo(tree, taken, point, MAX_BRIDGE_STEPS);
            return path == null || path.isEmpty() ? null : path.get(0);
        }
        return null;
    }

    /** Совпадает ли стартовый путь игрока со стартовым узлом гайда. */
    public static boolean isStartCompatible(GuideDefinition guide, Set<PointData> allocated, TalentTree tree) {
        if (allocated.isEmpty()) {
            return true;
        }
        Set<PointData> starts = new HashSet<>();
        for (PointData p : allocated) {
            Perk perk = tree.calcData.getPerk(p);
            if (perk != null && perk.is_entry) {
                starts.add(p);
            }
        }
        if (starts.isEmpty()) {
            return true;
        }
        return starts.contains(guide.pointAt(0));
    }

    public static boolean isStartCompatible(Player player, GuideDefinition guide) {
        TalentTree tree = talentTree();
        if (tree == null || guide == null) {
            return true;
        }
        return isStartCompatible(guide, new HashSet<>(allocatedPerks(player, tree)), tree);
    }

    private static boolean isAdjacent(TalentTree tree, PointData point, Set<PointData> allocated) {
        Set<PointData> connections = tree.calcData.connections.get(point);
        if (connections == null) {
            return false;
        }
        for (PointData con : connections) {
            if (allocated.contains(con)) {
                return true;
            }
        }
        return false;
    }

    /**
     * BFS от взятых узлов к цели. Возвращает список новых узлов по порядку
     * (первый — тот, который надо взять сейчас).
     */
    public static List<PointData> pathTo(TalentTree tree, Set<PointData> allocated, PointData target, int limit) {
        if (allocated.contains(target)) {
            return new ArrayList<>();
        }
        Map<PointData, PointData> prev = new HashMap<>();
        Map<PointData, Integer> depth = new HashMap<>();
        ArrayDeque<PointData> queue = new ArrayDeque<>();
        for (PointData p : allocated) {
            prev.put(p, null);
            depth.put(p, 0);
            queue.add(p);
        }
        while (!queue.isEmpty()) {
            PointData cur = queue.poll();
            int d = depth.getOrDefault(cur, 0);
            if (d >= limit) {
                continue;
            }
            Set<PointData> next = tree.calcData.connections.get(cur);
            if (next == null) {
                continue;
            }
            for (PointData n : next) {
                if (prev.containsKey(n)) {
                    continue;
                }
                prev.put(n, cur);
                depth.put(n, d + 1);
                if (n.equals(target)) {
                    List<PointData> path = new ArrayList<>();
                    PointData node = n;
                    while (node != null && !allocated.contains(node)) {
                        path.add(0, node);
                        node = prev.get(node);
                    }
                    return path;
                }
                queue.add(n);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ заклинания
    public static SpellSchool spellSchool(GuideDefinition guide) {
        if (guide == null) {
            return null;
        }
        try {
            if (ExileDB.SpellSchools().isRegistered(guide.school)) {
                return ExileDB.SpellSchools().get(guide.school);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Следующее заклинание/синергия, в которое стоит вложить очко (или null). */
    public static String recommendedSpell(Player player) {
        GuideDefinition guide = activeGuide(player);
        if (guide == null || player == null || isBeyondGuide(player)) {
            return null;
        }
        int cap = maxLevel();
        try {
            EntitySpellCap.ISpellsCap spells = Load.spells(player);
            if (spells.getFreeSpellPoints() <= 0) {
                return null;
            }
            SpellSchool school = spellSchool(guide);
            if (school == null) {
                return null;
            }
            SpellsData data = spells.getSpellsData();
            for (GuideDefinition.SpellStep step : guide.spells) {
                if (data.getLevelOf(step.spell) >= step.targetLevel) {
                    continue;
                }
                if (!spellInScope(school, step.spell, cap)) {
                    continue;
                }
                Spell spell = ExileDB.Spells().isRegistered(step.spell) ? ExileDB.Spells().get(step.spell) : null;
                if (spell != null) {
                    if (spells.canLearn(school, spell)) {
                        return step.spell;
                    }
                    continue;
                }
                if (ExileDB.Synergies().isRegistered(step.spell)) {
                    Synergy synergy = ExileDB.Synergies().get(step.spell);
                    if (spells.canLearn(school, synergy)) {
                        return step.spell;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static boolean isRecommendedSpell(Player player, String spellId) {
        String rec = recommendedSpell(player);
        return rec != null && rec.equals(spellId);
    }

    /** Открывается ли заклинание плана в пределах охвата гайда. */
    private static boolean spellInScope(SpellSchool school, String spellId, int cap) {
        PointData point = school.spells.get(spellId);
        if (point == null) {
            point = school.synergies.get(spellId);
        }
        return point == null || school.getLevelNeededToAllocate(point) <= cap;
    }

    /** Какой уровень нужен для следующего заклинания плана (0 если всё изучено). */
    public static int requiredLevelForNextSpell(Player player) {
        GuideDefinition guide = activeGuide(player);
        SpellSchool school = spellSchool(guide);
        if (guide == null || school == null || isBeyondGuide(player)) {
            return 0;
        }
        int cap = maxLevel();
        SpellsData data = Load.spells(player).getSpellsData();
        for (GuideDefinition.SpellStep step : guide.spells) {
            if (data.getLevelOf(step.spell) >= step.targetLevel) {
                continue;
            }
            if (!spellInScope(school, step.spell, cap)) {
                continue;
            }
            PointData point = school.spells.get(step.spell);
            if (point == null) {
                point = school.synergies.get(step.spell);
            }
            if (point != null) {
                return school.getLevelNeededToAllocate(point);
            }
        }
        return 0;
    }

    /**
     * Узлы вне плана, которые можно безопасно откатить: после удаления все
     * оставшиеся изученные узлы по-прежнему связаны со стартовым путём,
     * и игра разрешает сброс (canRemove — нужны очки сброса).
     *
     * Узлы, через которые проходит путь к плану, не предлагаются к сбросу —
     * без них план развалится.
     */
    public static List<PointData> refundablePoints(Player player, GuideDefinition guide,
                                                  TalentTree tree, Set<PointData> taken) {
        List<PointData> out = new ArrayList<>();
        if (player == null || guide == null || tree == null || taken == null || taken.size() < 2) {
            return out;
        }
        PointData entry = null;
        for (PointData p : taken) {
            Perk perk = tree.calcData.getPerk(p);
            if (perk != null && perk.is_entry) {
                entry = p;
                break;
            }
        }
        if (entry == null) {
            return out;
        }
        TalentsData talents = Load.playerRPGData(player).talents;
        for (PointData p : taken) {
            if (guide.isPlanPoint(p) || p.equals(entry)) {
                continue;
            }
            if (!talents.canRemoveIgnoringResetPoints(tree, p)) {
                continue; // крайние узлы откатить нельзя — сначала внутренние
            }
            if (connectedWithout(tree, taken, entry, p)) {
                out.add(p);
            }
        }
        return out;
    }

    /** Остаётся ли граф изученных узлов связным со стартом, если убрать skip. */
    private static boolean connectedWithout(TalentTree tree, Set<PointData> taken,
                                           PointData entry, PointData skip) {
        Set<PointData> seen = new HashSet<>();
        ArrayDeque<PointData> queue = new ArrayDeque<>();
        seen.add(entry);
        queue.add(entry);
        while (!queue.isEmpty()) {
            PointData cur = queue.poll();
            Set<PointData> next = tree.calcData.connections.get(cur);
            if (next == null) {
                continue;
            }
            for (PointData n : next) {
                if (n.equals(skip) || !taken.contains(n) || !seen.add(n)) {
                    continue;
                }
                queue.add(n);
            }
        }
        for (PointData p : taken) {
            if (!p.equals(skip) && !seen.contains(p)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ прогресс и советы
    public static class Progress {
        public int statSpent, statBudget;
        public int talentSpent, talentBudget;
        public int spellSpent, spellBudget;
        public int planTalentsTaken, planTalentsTotal;
        /** уровень персонажа и предел, до которого ведёт гайд */
        public int level, guideMaxLevel = GuideMath.DEFAULT_GUIDE_MAX_LEVEL;
        /** персонаж выше предела — гайд закончен */
        public boolean guideFinished;
        public boolean startCompatible = true;
        public List<String> advice = new ArrayList<>();
    }

    public static Progress progress(Player player) {
        Progress p = new Progress();
        GuideDefinition guide = activeGuide(player);
        if (player == null || guide == null) {
            return p;
        }
        TalentTree tree = talentTree();
        int level = Load.Unit(player).getLevel();
        int cap = GuideMath.guideMaxLevel();
        int goalLevel = GuideMath.cappedLevel(level);
        p.level = level;
        p.guideMaxLevel = cap;
        p.guideFinished = level > cap;
        p.statBudget = GuideMath.statBudget(goalLevel);
        p.statSpent = spentStatPoints(player);
        p.talentBudget = GuideMath.talentBudget(goalLevel);
        p.spellBudget = GuideMath.spellBudget(goalLevel);
        try {
            p.spellSpent = p.spellBudget - Load.spells(player).getFreeSpellPoints();
        } catch (Exception ignored) {
        }
        List<PointData> allocated = allocatedPerks(player, tree);
        p.talentSpent = allocated.size();
        p.planTalentsTotal = GuideMath.talentStepsInScope(guide);
        Set<PointData> taken = new HashSet<>(allocated);
        for (int i = 0; i < p.planTalentsTotal; i++) {
            if (taken.contains(guide.pointAt(i))) {
                p.planTalentsTaken++;
            }
        }
        if (tree != null) {
            p.startCompatible = isStartCompatible(guide, taken, tree);
        }
        p.advice = advices(player, guide, tree, taken, p);
        return p;
    }

    private static List<String> advices(Player player, GuideDefinition guide, TalentTree tree,
                                        Set<PointData> taken, Progress progress) {
        List<String> list = new ArrayList<>();
        RPGPlayerData data = Load.playerRPGData(player);
        int level = Load.Unit(player).getLevel();

        if (progress.guideFinished) {
            // Гайд закончился: дальше игрок решает сам, но советы по откату
            // лишних талантов остаются полезными и остаются в списке ниже.
            list.add("§a" + finishedText() + ". Ориентир по характеристикам: "
                + statWeightsLine(guide) + ". Таланты продолжайте от последнего взятого узла.");
        }

        if (!progress.startCompatible) {
            list.add("§cСтартовый путь не совпадает с гайдом. Взять можно только один стартовый путь, "
                + "поэтому план талантов недостижим — соберите сборку заново на Полигоне.");
        }

        // таланты вне плана
        List<PointData> offPlan = new ArrayList<>();
        for (PointData p : taken) {
            if (!guide.isPlanPoint(p)) {
                offPlan.add(p);
            }
        }
        if (!offPlan.isEmpty() && progress.startCompatible) {
            List<PointData> refundable = refundablePoints(player, guide, tree, taken);
            List<String> names = new ArrayList<>();
            for (PointData p : refundable) {
                Perk perk = tree != null ? tree.calcData.getPerk(p) : null;
                if (perk != null && names.size() < 3) {
                    names.add(perk.locName().getString());
                }
            }
            if (!refundable.isEmpty()) {
                if (data.talents.reset_points > 0) {
                    list.add("§eСтоит откатить узлов вне плана: " + refundable.size()
                        + (names.isEmpty() ? "" : " (" + String.join(", ", names)
                        + (refundable.size() > 3 ? "…" : "") + ")")
                        + " — они не работают на сборку. ПКМ по узлу сбрасывает очко (осталось: "
                        + data.talents.reset_points + ").");
                } else {
                    list.add("§eУзлов вне плана: " + refundable.size()
                        + " — их лучше откатить, но очков сброса нет. Их дают на Полигоне "
                        + "(«Бонусные очки») или новой сборкой.");
                }
            } else {
                list.add("§7Вне плана узлов: " + offPlan.size()
                    + " — все они держат путь к нужным талантам, откатывать не нужно. План продолжается от них.");
            }
        }

        // отставание по уровню (после предела гайда неактуально)
        if (!progress.guideFinished && progress.startCompatible && !guide.talents.isEmpty()) {
            int next = -1;
            int inScope = GuideMath.talentStepsInScope(guide);
            for (int i = 0; i < inScope; i++) {
                if (!taken.contains(guide.pointAt(i))) {
                    next = i;
                    break;
                }
            }
            if (next >= 0) {
                int need = GuideDefinition.levelNeededForTalentStep(next);
                if (need > level) {
                    list.add("§7Следующий узел плана откроется на ур. " + need + " (очки талантов: 1 + 2×ур.).");
                }
            }
        }

        // характеристики вне плана — их не откатить, гайд компенсирует
        int offStats = 0;
        for (Map.Entry<String, Integer> e : currentStats(player).entrySet()) {
            boolean inPlan = false;
            for (GuideDefinition.StatWeight w : guide.stats) {
                if (w.stat.equals(e.getKey())) {
                    inPlan = true;
                    break;
                }
            }
            if (!inPlan) {
                offStats += e.getValue();
            }
        }
        if (offStats > 0) {
            list.add("§7Очков характеристик вне плана: " + offStats
                + ". Откатить их нельзя — гайд сдвигает остальные статы, чтобы компенсировать.");
        }

        // заклинания
        int spellFree;
        try {
            spellFree = Load.spells(player).getFreeSpellPoints();
        } catch (Exception e) {
            spellFree = 0;
        }
        if (!progress.guideFinished && spellFree > 0) {
            int need = requiredLevelForNextSpell(player);
            if (need > level) {
                list.add("§7Следующее заклинание плана откроется на ур. " + need + ".");
            }
        }
        return list;
    }

    /** Короткая строка «что делать сейчас». */
    public static String nextStepText(Player player) {
        GuideDefinition guide = activeGuide(player);
        if (guide == null) {
            return "Гайд не выбран";
        }
        if (isBeyondGuide(player)) {
            return finishedText();
        }
        String stat = recommendedStat(player);
        if (stat != null) {
            return "Вложите очко: " + GuideMath.shortName(stat);
        }
        PointData talent = recommendedTalent(player);
        if (talent != null) {
            int index = guide.indexOfPoint(talent);
            String name;
            TalentTree tree = talentTree();
            Perk perk = tree != null ? tree.calcData.getPerk(talent) : null;
            name = perk != null ? perk.locName().getString() : talent.toString();
            return index >= 0
                ? "Талант №" + (index + 1) + " плана: " + name
                : "Дойдите до плана: " + name;
        }
        String spell = recommendedSpell(player);
        if (spell != null) {
            return "Улучшите заклинание: " + spellName(spell);
        }
        return "План выполнен — свободные очки";
    }

    /** Подсказка для экрана древа талантов. */
    public static String talentHintText(Player player) {
        GuideDefinition guide = activeGuide(player);
        if (guide == null) {
            return "";
        }
        if (isBeyondGuide(player)) {
            return "";
        }
        TalentTree tree = talentTree();
        if (tree != null && !isStartCompatible(guide, new HashSet<>(allocatedPerks(player, tree)), tree)) {
            return "Гайд: выбран другой стартовый путь — соберите сборку заново";
        }
        PointData rec = recommendedTalent(player);
        if (rec != null) {
            Perk perk = tree != null ? tree.calcData.getPerk(rec) : null;
            String name = perk != null ? perk.locName().getString() : rec.toString();
            int index = guide.indexOfPoint(rec);
            return index >= 0 ? "Гайд: узел №" + (index + 1) + " — " + name : "Гайд: путь к плану — " + name;
        }
        if (guide.talents.isEmpty() || tree == null) {
            return "";
        }
        int level = Load.Unit(player).getLevel();
        int inScope = GuideMath.talentStepsInScope(guide);
        for (int i = 0; i < inScope; i++) {
            if (!Load.playerRPGData(player).talents.isAllocated(tree, guide.pointAt(i))) {
                int need = GuideDefinition.levelNeededForTalentStep(i);
                if (need > level) {
                    return "Гайд: следующий узел плана с ур. " + need;
                }
                int free = Load.playerRPGData(player).talents.getFreePoints(Load.Unit(player), tree.getSchool_type());
                if (free <= 0) {
                    return "Гайд: свободных очков талантов нет";
                }
                return "Гайд: следующий узел плана далеко — откройте вкладку «Гайды»";
            }
        }
        return "Гайд: план талантов изучен";
    }

    /** Подсказка для экрана заклинаний. */
    public static String spellHintText(Player player) {
        GuideDefinition guide = activeGuide(player);
        if (guide == null) {
            return "";
        }
        SpellSchool school = spellSchool(guide);
        if (school == null || isBeyondGuide(player)) {
            return "";
        }
        String spell = recommendedSpell(player);
        if (spell != null) {
            return "Гайд: улучшите " + spellName(spell);
        }
        int need = requiredLevelForNextSpell(player);
        if (need > Load.Unit(player).getLevel()) {
            return "Гайд: следующее заклинание с ур. " + need;
        }
        return "Гайд: школа «" + school.locName().getString() + "» прокачана по плану";
    }

    /** Локализованное имя заклинания/синергии (или id, если оно не зарегистрировано). */
    public static String spellName(String id) {
        try {
            if (ExileDB.Spells().isRegistered(id)) {
                return ExileDB.Spells().get(id).locName().getString();
            }
            if (ExileDB.Synergies().isRegistered(id)) {
                return ExileDB.Synergies().get(id).locName().getString();
            }
        } catch (Exception ignored) {
        }
        return id;
    }

    /** Текущая веха гайда по уровню персонажа («что делать на этом отрезке»). */
    public static String milestoneText(Player player, GuideDefinition guide) {
        if (player == null || guide == null || guide.milestones.isEmpty()) {
            return "";
        }
        int rawLevel = Load.Unit(player)
            .getLevel();
        if (GuideMath.beyondGuide(rawLevel)) {
            return ""; // гайд закончился — вехи больше не «сейчас»
        }
        int level = GuideMath.cappedLevel(rawLevel);
        int[] bounds = {5, 10, 20, 30, 40, 50};
        int index = 0;
        for (int i = 0; i < bounds.length; i++) {
            if (level > bounds[i]) {
                index = i + 1;
            }
        }
        if (index >= guide.milestones.size()) {
            index = guide.milestones.size() - 1;
        }
        return guide.milestones.get(index);
    }

    // ------------------------------------------------------------------ сборка на Полигоне
    /**
     * Полная сборка персонажа по гайду. Только сервер, только Полигон.
     *
     * @param clearFirst сбросить текущие таланты/статы/заклинания
     * @param gear       выдать набор экипировки под гайд
     */
    public static void applyInSimulation(ServerPlayer player, GuideDefinition guide,
                                         boolean clearFirst, boolean gear) {
        if (player == null || guide == null) {
            return;
        }
        if (!WorldUtils.isSimulationWorld(player.level())) {
            player.sendSystemMessage(Component.literal(
                "§e[Гайд] Собрать сборку можно только на Полигоне (/sim). В обычном мире гайд только подсказывает."));
            return;
        }
        if (clearFirst) {
            SimulationManager.resetBuildInSimulation(player);
        }

        EntityData unitData = Load.Unit(player);
        RPGPlayerData data = Load.playerRPGData(player);
        int level = unitData.getLevel();

        // --- характеристики ---
        int budget = GuideMath.statBudget(level);
        Map<String, Integer> split = GuideMath.idealSplit(guide, budget);
        data.statPoints.map.clear();
        for (Map.Entry<String, Integer> e : split.entrySet()) {
            if (e.getValue() > 0) {
                data.statPoints.map.put(e.getKey(), e.getValue());
            }
        }

        // --- таланты ---
        TalentTree tree = talentTree();
        int allocatedTalents = 0;
        if (tree != null) {
            int free = data.talents.getFreePoints(unitData, tree.getSchool_type());
            Set<PointData> taken = new HashSet<>(data.talents.getSchool(tree).getAllocatedPoints(tree));
            for (int i = 0; i < guide.talents.size() && free > 0; i++) {
                if (GuideDefinition.levelNeededForTalentStep(i) > level) {
                    break;
                }
                PointData point = guide.pointAt(i);
                Perk perk = point == null ? null : tree.calcData.getPerk(point);
                if (point == null || perk == null || taken.contains(point)) {
                    continue;
                }
                if (perk.lvl_req > level) {
                    continue; // узел требует более высокий уровень — собираем то, что доступно
                }
                if (!taken.isEmpty() && !isAdjacent(tree, point, taken)) {
                    // план недостижим напрямую — прокладываем путь по живому графу
                    List<PointData> path = pathTo(tree, taken, point, 8);
                    if (path == null) {
                        continue;
                    }
                    boolean ok = true;
                    for (PointData step : path) {
                        if (free <= 0) {
                            ok = false;
                            break;
                        }
                        data.talents.allocate(player, tree, step);
                        taken.add(step);
                        free--;
                        allocatedTalents++;
                    }
                    if (!ok) {
                        break;
                    }
                    continue;
                }
                data.talents.allocate(player, tree, point);
                taken.add(point);
                free--;
                allocatedTalents++;
            }
        }

        // --- заклинания ---
        SpellSchool school = spellSchool(guide);
        EntitySpellCap.ISpellsCap spells = Load.spells(player);
        SpellsData spellData = spells.getSpellsData();
        int learned = 0;
        if (school != null) {
            spellData.schools.add(school.GUID());
            for (GuideDefinition.SpellStep step : guide.spells) {
                Spell spell = ExileDB.Spells().isRegistered(step.spell) ? ExileDB.Spells().get(step.spell) : null;
                Synergy synergy = spell == null && ExileDB.Synergies().isRegistered(step.spell)
                    ? ExileDB.Synergies().get(step.spell) : null;
                PointData point = school.spells.get(step.spell);
                if (point == null) {
                    point = school.synergies.get(step.spell);
                }
                if (point == null || school.getLevelNeededToAllocate(point) > level) {
                    continue;
                }
                int max = spell != null ? spell.getMaxLevel() : (synergy != null ? synergy.getMaxLevel() : 8);
                int target = Math.min(step.targetLevel, max);
                int guard = 0;
                while (guard++ < 64) {
                    if (spell != null ? !spells.canLearn(school, spell) : (synergy == null || !spells.canLearn(school, synergy))) {
                        break;
                    }
                    if (spellData.getLevelOf(step.spell) >= target) {
                        break;
                    }
                    if (spell != null) {
                        spellData.learnSpell(spell, school);
                    } else {
                        spellData.learnSynergy(synergy, school);
                    }
                    learned++;
                }
            }
        }

        unitData.setEquipsChanged(true);
        unitData.tryRecalculateStats();
        SimulationManager.healPlayerFull(player);
        SimulationManager.syncAll(player);

        int planTaken = 0;
        Set<PointData> nowTaken = new HashSet<>(allocatedPerks(player, tree));
        for (int i = 0; i < guide.talents.size(); i++) {
            if (nowTaken.contains(guide.pointAt(i))) {
                planTaken++;
            }
        }

        player.sendSystemMessage(Component.literal("§6══════════════════════════════════"));
        player.sendSystemMessage(Component.literal("§a[Гайд] Сборка «" + guide.name + "» собрана!"));
        player.sendSystemMessage(Component.literal("§7• Характеристики: §f" + splitLine(split)));
        player.sendSystemMessage(Component.literal("§7• Таланты: §f" + allocatedTalents
            + " §7новых, узлов плана: §f" + planTaken + "/" + guide.talents.size()));
        player.sendSystemMessage(Component.literal("§7• Очки заклинаний вложено: §f" + learned));
        player.sendSystemMessage(Component.literal("§8Гайд ведёт до ур. " + maxLevel()
            + "; здесь сборка собрана целиком — это её финальная форма."));
        player.sendSystemMessage(Component.literal("§6══════════════════════════════════"));

        if (gear) {
            SimulationManager.giveGearKit(player, guide.gearKit);
            SimulationManager.giveGearKit(player, 3);
            SimulationManager.healPlayerFull(player);
            SimulationManager.syncAll(player);
        }

        // первое заклинание плана — в хотбар, если он пуст
        try {
            if (spellData.hotbars.isEmpty()) {
                for (GuideDefinition.SpellStep step : guide.spells) {
                    if (spellData.getLevelOf(step.spell) > 0) {
                        spellData.hotbars.put(0, step.spell);
                        break;
                    }
                }
                Load.spells(player).syncToClient(player);
            }
        } catch (Exception ignored) {
        }

        QuestBookService.onSimulationGuideApplied(player, guide.id);
    }

    public static String splitLine(Map<String, Integer> split) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : split.entrySet()) {
            if (e.getValue() <= 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(GuideMath.shortName(e.getKey())).append(" ").append(e.getValue());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ утилиты для GUI
    /** Строки описания статов гайда с числом очков «сейчас» и «цель». */
    public static List<String> statPlanLines(Player player, GuideDefinition guide) {
        List<String> lines = new ArrayList<>();
        if (player == null || guide == null) {
            return lines;
        }
        int level = GuideMath.cappedLevel(Load.Unit(player)
            .getLevel());
        Map<String, Integer> ideal = GuideMath.idealSplit(guide, GuideMath.statBudget(level));
        Map<String, Integer> current = currentStats(player);
        for (GuideDefinition.StatWeight w : guide.stats) {
            lines.add(GuideMath.shortName(w.stat) + ": " + current.getOrDefault(w.stat, 0)
                + " / " + ideal.getOrDefault(w.stat, 0));
        }
        return lines;
    }

    public static ChatFormatting colorOf(GuideDefinition guide) {
        return guide == null ? ChatFormatting.GRAY : guide.color;
    }
}
