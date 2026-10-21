package com.robertx22.age_of_exile.guides;

import com.robertx22.age_of_exile.saveclasses.PointData;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Описание одного гайда (готовой сборки).
 *
 * Гайд — это три плана, которые проверяются на живом персонаже:
 *  - распределение очков характеристик (веса, а не жёсткие числа — так гайд
 *    подстраивается под любой уровень и под уже вложенные очки);
 *  - упорядоченный список узлов древа талантов (валидный путь: каждый следующий
 *    узел соединён с предыдущим, поэтому его можно взять по правилам canAllocate);
 *  - упорядоченный список заклинаний/синергий школы с целевыми уровнями.
 */
public class GuideDefinition {

    public final String id;
    public final String name;
    public final String tagline;
    public final ChatFormatting color;
    public final String icon;
    /** id школы заклинаний (divine / hunting / nature / fire / water) */
    public final String school;
    /** какой набор экипировки выдавать на Полигоне: 0 воин, 1 охотник, 2 маг, 3 бижутерия */
    public final int gearKit;

    /**
     * id древа талантов, к которому относится план (talents / divine).
     * <p>
     * По умолчанию {@link #DEFAULT_TREE}; координаты из {@link #talentPlan(String)} читаются
     * именно из этого древа.
     */
    public String tree = DEFAULT_TREE;

    public static final String DEFAULT_TREE = "talents";

    public final List<StatWeight> stats = new ArrayList<>();
    public final List<TalentStep> talents = new ArrayList<>();
    public final List<SpellStep> spells = new ArrayList<>();
    public final List<String> tips = new ArrayList<>();
    public final List<String> milestones = new ArrayList<>();

    public GuideDefinition(String id, String name, String tagline, ChatFormatting color,
                           String icon, String school, int gearKit) {
        this.id = id;
        this.name = name;
        this.tagline = tagline;
        this.color = color;
        this.icon = icon;
        this.school = school;
        this.gearKit = gearKit;
    }

    public GuideDefinition stat(String statId, float weight) {
        this.stats.add(new StatWeight(statId, weight));
        return this;
    }

    public GuideDefinition spell(String spellId, int targetLevel) {
        this.spells.add(new SpellStep(spellId, targetLevel));
        return this;
    }

    public GuideDefinition tip(String tip) {
        this.tips.add(tip);
        return this;
    }

    public GuideDefinition milestone(String milestone) {
        this.milestones.add(milestone);
        return this;
    }

    /** В каком древе талантов лежит план (по умолчанию «talents»). */
    public GuideDefinition tree(String treeId) {
        this.tree = treeId == null || treeId.isEmpty() ? DEFAULT_TREE : treeId;
        return this;
    }

    /** План талантов из компактной строки "x:y:perk;x:y:perk;..." */
    public GuideDefinition talentPlan(String compact) {
        if (compact == null || compact.isEmpty()) {
            return this;
        }
        for (String entry : compact.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length < 2) {
                continue;
            }
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                String perk = parts.length > 2 ? parts[2].trim() : "";
                this.talents.add(new TalentStep(x, y, perk));
            } catch (NumberFormatException ignored) {
            }
        }
        return this;
    }

    public PointData pointAt(int index) {
        if (index < 0 || index >= talents.size()) {
            return null;
        }
        TalentStep step = talents.get(index);
        return new PointData(step.x, step.y);
    }

    public int indexOfPoint(PointData point) {
        if (point == null) {
            return -1;
        }
        for (int i = 0; i < talents.size(); i++) {
            TalentStep step = talents.get(i);
            if (step.x == point.x && step.y == point.y) {
                return i;
            }
        }
        return -1;
    }

    public boolean isPlanPoint(PointData point) {
        return indexOfPoint(point) >= 0;
    }

    /** Сколько очков талантов нужно, чтобы взять узел с индексом index (0-based). */
    public static int levelNeededForTalentStep(int index) {
        // очков талантов на уровне L = 1 + 2 * L, значит узел N требует L >= (N - 1) / 2
        return Math.max(0, index) / 2;
    }

    public static class StatWeight {
        public final String stat;
        public final float weight;

        public StatWeight(String stat, float weight) {
            this.stat = stat;
            this.weight = weight;
        }
    }

    public static class TalentStep {
        public final int x;
        public final int y;
        public final String perk;

        public TalentStep(int x, int y, String perk) {
            this.x = x;
            this.y = y;
            this.perk = perk;
        }

        public PointData point() {
            return new PointData(x, y);
        }
    }

    public static class SpellStep {
        public final String spell;
        public final int targetLevel;

        public SpellStep(String spell, int targetLevel) {
            this.spell = spell;
            this.targetLevel = targetLevel;
        }
    }
}
