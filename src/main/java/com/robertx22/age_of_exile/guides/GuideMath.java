package com.robertx22.age_of_exile.guides;

import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.database.data.stats.StatScaling;
import com.robertx22.age_of_exile.database.data.stats.types.defense.Armor;
import com.robertx22.age_of_exile.database.data.stats.types.defense.DodgeRating;
import com.robertx22.age_of_exile.database.data.stats.types.generated.ElementalResist;
import com.robertx22.age_of_exile.database.data.stats.types.resources.energy.Energy;
import com.robertx22.age_of_exile.database.data.stats.types.resources.health.Health;
import com.robertx22.age_of_exile.database.data.stats.types.resources.mana.Mana;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.enumclasses.ModType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Арифметика гайдов — тонкое зеркало формул самого мода.
 *
 * Всё, что тут считается, повторяет исходники:
 *  - очки характеристик: level * STAT_POINTS_PER_LEVEL
 *  - очки талантов: STARTING_TALENT_POINTS + TALENT_POINTS_AT_MAX_LEVEL * level / MAX_LEVEL
 *  - очки заклинаний: SPELL_POINTS_PER_LEVEL * level
 *  - 1 очко VIT = 2 HP * NORMAL(level) (DatapackStats.VIT + StatScaling.NORMAL)
 *  - эффективность защиты: IUsableStat.getUsableValue (гипербола с асимптотой 80%)
 */
public class GuideMath {

    /** NORMAL-скейлинг уровня (1 + 0.2 * (level - 1)). */
    public static float normal(int level) {
        return StatScaling.NORMAL.scale(1F, level);
    }

    /** Сколько очков характеристик доступно на уровне. */
    public static int statBudget(int level) {
        return (int) (level * GameBalanceConfig.get().STAT_POINTS_PER_LEVEL);
    }

    /** Сколько очков талантов доступно на уровне. */
    public static int talentBudget(int level) {
        return (int) (GameBalanceConfig.get().STARTING_TALENT_POINTS
            + GameBalanceConfig.get().TALENT_POINTS_AT_MAX_LEVEL * (float) level / GameBalanceConfig.get().MAX_LEVEL);
    }

    /** Сколько очков заклинаний доступно на уровне. */
    public static int spellBudget(int level) {
        return (int) (GameBalanceConfig.get().SPELL_POINTS_PER_LEVEL * level);
    }

    /**
     * Идеальная разбивка очков характеристик под веса гайда (метод наибольших остатков).
     * Сумма всегда ровно total — поэтому гайд работает на любом уровне.
     */
    public static Map<String, Integer> idealSplit(GuideDefinition guide, int total) {
        Map<String, Integer> out = new LinkedHashMap<>();
        float sum = 0;
        for (GuideDefinition.StatWeight w : guide.stats) {
            sum += w.weight;
            out.put(w.stat, 0);
        }
        if (sum <= 0 || total <= 0) {
            return out;
        }
        int assigned = 0;
        List<float[]> fractions = new ArrayList<>();
        for (int i = 0; i < guide.stats.size(); i++) {
            GuideDefinition.StatWeight w = guide.stats.get(i);
            float exact = total * w.weight / sum;
            int base = (int) Math.floor(exact);
            out.put(w.stat, base);
            assigned += base;
            fractions.add(new float[]{i, exact - base});
        }
        int left = total - assigned;
        fractions.sort(Comparator.comparingDouble((float[] a) -> a[1]).reversed());
        int idx = 0;
        while (left > 0 && !fractions.isEmpty()) {
            int statIndex = (int) fractions.get(idx % fractions.size())[0];
            String stat = guide.stats.get(statIndex).stat;
            out.put(stat, out.getOrDefault(stat, 0) + 1);
            left--;
            idx++;
        }
        return out;
    }

    /** % снижения физического урона бронёй (реальная формула мода). */
    public static float armorPercent(float armor, int level) {
        return Armor.getInstance().getUsableValue((int) Math.max(0, armor), level);
    }

    /** % шанса уклонения от атак. */
    public static float dodgePercent(float dodge, int level) {
        return DodgeRating.getInstance().getUsableValue((int) Math.max(0, dodge), level);
    }

    /** % снижения урона стихией. */
    public static float resistPercent(float resist, Elements element, int level) {
        return new ElementalResist(element).getUsableValue((int) Math.max(0, resist), level);
    }

    /** Сколько брони нужно для заданного % снижения урона. */
    public static float armorForPercent(float percent, int level) {
        float p = Math.min(0.79F, Math.max(0.01F, percent));
        float needed = Armor.getInstance().scale(ModType.FLAT, 20F, level);
        float target = p / 0.8F;
        float ratio = 1.5F * target / (1F - target);
        return ratio * needed;
    }

    /** Вклад 1 очка характеристики на уровне — ровно как в DatapackStats. */
    public static Map<String, Float> perPointCoreStat(String statId, int level) {
        Map<String, Float> map = new LinkedHashMap<>();
        float normal = normal(level);
        switch (statId) {
            case "strength" -> {
                map.put("physical_weapon_damage", 0.1F * normal);
                map.put("armor", 1F * normal);
            }
            case "dexterity" -> {
                map.put("physical_weapon_damage", 0.1F * normal);
                map.put("dodge", 1F * normal);
            }
            case "intelligence" -> {
                map.put("spell_power", 0.1F * normal);
                map.put("fire_resist", 1F * normal);
                map.put("water_resist", 1F * normal);
                map.put("earth_resist", 1F * normal);
            }
            case "vitality" -> {
                map.put("health", 2F * normal);
                map.put("health_regen", 0.1F * normal);
            }
            case "wisdom" -> {
                map.put("mana", 2F * normal);
                map.put("mana_regen", 0.1F * normal);
            }
            case "agility" -> {
                map.put("energy", 2F * normal);
                map.put("energy_regen", 0.1F * normal);
            }
            default -> {
            }
        }
        return map;
    }

    /** Вклад вложенных очков характеристик в базовые статы (без экипировки и талантов). */
    public static Map<String, Float> coreStatTotals(Map<String, Integer> split, int level) {
        Map<String, Float> total = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : split.entrySet()) {
            for (Map.Entry<String, Float> gain : perPointCoreStat(e.getKey(), level).entrySet()) {
                total.merge(gain.getKey(), gain.getValue() * e.getValue(), Float::sum);
            }
        }
        return total;
    }

    /** Какие статы даст следующее очко характеристики (для подсказки в тултипе). */
    public static String nextPointPreview(String statId, int level) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Float> e : perPointCoreStat(statId, level).entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(prettyStat(e.getKey(), level)).append(" +").append(fmt(e.getValue()));
        }
        return sb.toString();
    }

    public static String prettyStat(String id, int level) {
        return switch (id) {
            case "health" -> "HP";
            case "mana" -> "мана";
            case "energy" -> "энергия";
            case "armor" -> "броня";
            case "dodge" -> "уклонение";
            case "spell_power" -> "сила заклинаний";
            case "physical_weapon_damage" -> "физ. урон";
            case "health_regen" -> "реген HP/3с";
            case "mana_regen" -> "реген маны/3с";
            case "energy_regen" -> "реген энергии/3с";
            case "fire_resist" -> "сопр. огню";
            case "water_resist" -> "сопр. воде";
            case "earth_resist" -> "сопр. природе";
            default -> id;
        };
    }

    public static String fmt(float value) {
        if (Math.abs(value) >= 100) {
            return String.valueOf(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /** Проекция сборки «только характеристики» (без экипировки и талантов). */
    public static Projection project(GuideDefinition guide, int level) {
        Map<String, Integer> split = idealSplit(guide, statBudget(level));
        Map<String, Float> totals = coreStatTotals(split, level);

        Projection p = new Projection();
        p.level = level;
        p.split = split;

        // BaseStatsAdder: health 10 (scaled), mana 30 (scaled) + 10 (flat),
        // energy 30 (scaled), spell power и физ. урон: 1 (scaled) + 3 (flat).
        float baseHp = Health.getInstance().scale(ModType.FLAT, 10F, level);
        float baseMana = Mana.getInstance().scale(ModType.FLAT, 30F, level);
        float baseEnergy = Energy.getInstance().scale(ModType.FLAT, 30F, level);
        float baseSp = 1F * normal(level) + 3F;
        float basePhys = 1F * normal(level) + 3F;

        p.hp = baseHp + totals.getOrDefault("health", 0F);
        p.mana = baseMana + 10F + totals.getOrDefault("mana", 0F);
        p.energy = baseEnergy + totals.getOrDefault("energy", 0F);
        p.spellPower = baseSp + totals.getOrDefault("spell_power", 0F);
        p.physDamage = basePhys + totals.getOrDefault("physical_weapon_damage", 0F);
        p.armor = totals.getOrDefault("armor", 0F);
        p.dodge = totals.getOrDefault("dodge", 0F);
        p.fireResist = totals.getOrDefault("fire_resist", 0F);
        p.armorPct = armorPercent(p.armor, level);
        p.dodgePct = dodgePercent(p.dodge, level);
        p.resistPct = resistPercent(p.fireResist, Elements.Fire, level);
        p.talents = talentBudget(level);
        p.spells = spellBudget(level);
        return p;
    }

    public static class Projection {
        public int level;
        public Map<String, Integer> split = new LinkedHashMap<>();
        public float hp, mana, energy, spellPower, physDamage, armor, dodge, fireResist;
        public float armorPct, dodgePct, resistPct;
        public int talents, spells;

        public String splitLine() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : split.entrySet()) {
                if (e.getValue() <= 0) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(shortName(e.getKey())).append(" ").append(e.getValue());
            }
            return sb.toString();
        }
    }

    public static String shortName(String statId) {
        return switch (statId) {
            case "strength" -> "СИЛ";
            case "dexterity" -> "ЛОВ";
            case "intelligence" -> "ИНТ";
            case "vitality" -> "ЖИВ";
            case "wisdom" -> "МУД";
            case "agility" -> "ЛВК";
            default -> statId;
        };
    }
}
