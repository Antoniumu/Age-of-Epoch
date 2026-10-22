package com.robertx22.age_of_exile.gamemaster;

import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Чистая математика GameMaster: профиль темпа и догоняющий множитель.
 * Не трогает состояние сервера, поэтому её легко проверять отдельно.
 */
public class ExpTuner {

    private static final Map<Integer, double[]> WEIGHT_CACHE = new HashMap<>();

    public static double clamp(double value, double min, double max) {
        return GameMasterConfig.clamp(value, min, max);
    }

    public static int maxLevel() {
        try {
            return Math.max(2, GameBalanceConfig.get().MAX_LEVEL);
        } catch (Exception e) {
            return 50;
        }
    }

    /**
     * Вес уровня — сколько опыта нужно, чтобы уйти с уровня {@code (index + 1)}.
     * Время на уровень пропорционально этому весу (скорость опыта одинаковая).
     */
    public static double[] expWeights(int maxLevel) {
        return WEIGHT_CACHE.computeIfAbsent(maxLevel, ml -> {
            double[] weights = new double[Math.max(1, ml - 1)];
            for (int level = 1; level < ml; level++) {
                double w;
                try {
                    w = Math.max(1, LevelUtils.getExpRequiredForLevel(level + 1));
                } catch (Exception e) {
                    w = 1;
                }
                weights[level - 1] = w;
            }
            return weights;
        });
    }

    /** 0 до стартового уровня, затем плавно растёт до 1 на максимальном уровне. */
    public static double pacingRamp(int level, int startLevel, int maxLevel, double curve) {
        if (level <= startLevel) {
            return 0;
        }
        int span = Math.max(1, maxLevel - startLevel);
        double t = Math.min(1.0, (level - startLevel) / (double) span);
        return Math.pow(t, Math.max(0.1, curve));
    }

    /**
     * Множитель темпа на уровне.
     * {@code k < 0} — штраф, растущий к максимальному уровню; {@code k > 0} — ускорение.
     */
    public static double pacingMultiplier(int level, double k, int startLevel, int maxLevel, double curve) {
        double m = 1.0 + k * pacingRamp(level, startLevel, maxLevel, curve);
        return Math.max(0.02, m);
    }

    /**
     * Во сколько раз растянется полный путь до максимального уровня при данном {@code k}.
     * 1.0 — без изменений, 2.5 — в 2.5 раза дольше.
     */
    public static double stretch(double k, double[] weights, int startLevel, double curve) {
        int maxLevel = weights.length + 1;
        double weighted = 0;
        double total = 0;
        for (int level = 1; level < maxLevel; level++) {
            double w = weights[level - 1];
            total += w;
            weighted += w / pacingMultiplier(level, k, startLevel, maxLevel, curve);
        }
        if (total <= 0) {
            return 1;
        }
        return weighted / total;
    }

    /**
     * Подбирает {@code k}, чтобы итоговое время до максимума совпало с целевым.
     *
     * @param baselineHours сколько часов занимает путь без модификаторов
     */
    public static double solveK(GameMasterConfig config, double baselineHours, int maxLevel) {
        double maxPenalty = Math.max(0, Math.min(0.99, config.pacing_max_penalty));
        double maxBonus = Math.max(0, config.pacing_max_bonus);
        double lo = -maxPenalty;
        double hi = maxBonus;

        double[] weights = expWeights(maxLevel);
        double desired = Math.max(0.01, config.target_hours / Math.max(0.01, baselineHours));

        double slowest = stretch(lo, weights, config.pacing_start_level, config.pacing_curve);
        double fastest = stretch(hi, weights, config.pacing_start_level, config.pacing_curve);

        // медленнее, чем позволяет максимальный штраф — упираемся в предел
        if (desired >= slowest) {
            return lo;
        }
        // быстрее, чем позволяет максимальное ускорение — упираемся в предел
        if (desired <= fastest) {
            return hi;
        }

        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2.0;
            double s = stretch(mid, weights, config.pacing_start_level, config.pacing_curve);
            if (s > desired) {
                // всё ещё медленнее цели — нужно больше ускорять (k выше)
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0;
    }

    /**
     * Догоняющий множитель: отстающие получают бонус, ушедшие вперёд — небольшой штраф.
     *
     * @param referenceLevel ориентир группы
     * @param hasPeers       есть ли вообще с кем сравнивать
     */
    public static double catchUpMultiplier(int level, double referenceLevel, boolean hasPeers, GameMasterConfig config) {
        if (!hasPeers) {
            return 1;
        }

        double delta = referenceLevel - level;

        if (delta > 0.05) {
            double bonus = Math.min(config.catch_up_max_bonus, config.catch_up_per_level * delta);
            return 1.0 + Math.max(0, bonus);
        }

        if (delta < -0.05 && config.catch_up_penalize_ahead) {
            double penalty = config.catch_up_penalty_per_level * (-delta);
            return Math.max(config.catch_up_min_multiplier, 1.0 - Math.max(0, penalty));
        }

        return 1.0;
    }

    /** Предсказанное реальное время до максимума при текущей калибровке. */
    public static double predictedHours(GameMasterConfig config, double baselineHours, double k, int maxLevel) {
        double stretch = stretch(k, expWeights(maxLevel), config.pacing_start_level, config.pacing_curve);
        return baselineHours * stretch;
    }
}
