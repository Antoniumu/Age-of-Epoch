package com.robertx22.age_of_exile.gamemaster;

import java.util.ArrayList;
import java.util.List;

/**
 * Удержание игрока: ежедневные серии входов, награды за вехи уровней,
 * «защита от невезения» (pity) и оценка риска ухода.
 *
 * <p>Класс чистый — только числа и структуры, без обращения к серверу и мирам.
 * Все решения GameMaster применяет сам.
 *
 * <p>Почему именно это, а не «нейросеть»:
 * <ul>
 *     <li>удержание ломается на <b>первых 30 минутах</b> (пустой мир, нет цели, смерть без прогресса)
 *     и на <b>сухих сериях</b> (300 убийств без униката) — это решается правилами, а не моделью;</li>
 *     <li>для обучения модели нужны данные, которых у мода нет; поэтому сначала телеметрия
 *     ({@link GameMaster#exportCsv()}), потом — офлайн-модель, а не инференс в тике.</li>
 * </ul>
 */
public final class Retention {

    private Retention() {
    }

    /** Результат начисления награды. */
    public static final class Reward {
        public int favor = 0;
        public int rare_charges = 0;
        public int epic_charges = 0;
        public int streak = 0;
        public boolean milestone = false;
        public String text = "";
    }

    /** Номер суток (UTC) — стабильный «игровой день» без привязки к часовому поясу. */
    public static long dayIndex(long epochSeconds) {
        return Math.max(0, epochSeconds) / 86400L;
    }

    /**
     * Начисляет награду за новый день и ведёт серию.
     * Возвращает пустую награду, если сегодня игрок уже заходил.
     */
    public static Reward onSession(PlayerProgress progress, long now, GameMasterConfig config) {
        Reward reward = new Reward();
        if (progress == null || config == null || !config.retention_enabled) {
            return reward;
        }

        long today = dayIndex(now);
        int streak;

        if (progress.last_session_day <= 0) {
            streak = 1;
        } else if (today > progress.last_session_day) {
            long gap = today - progress.last_session_day;
            int grace = Math.max(1, config.retention_streak_grace_days);
            streak = gap <= grace ? progress.streak_days + 1 : 1;
        } else {
            return reward; // уже засчитано в эти сутки
        }

        progress.last_session_day = today;
        progress.streak_days = (int) clamp(streak, 1, Math.max(1, config.retention_streak_max_days));
        addActiveDay(progress, (int) today);

        reward.streak = progress.streak_days;
        reward.favor = (int) Math.round(config.retention_daily_favor
            + Math.min(config.retention_streak_max_favor,
            config.retention_streak_bonus_favor * (progress.streak_days - 1)));

        // Вехи серии: награды выдаются только на конкретных значениях, иначе
        // «эпик каждый день после 14-го» обесценил бы дроп.
        if (progress.streak_days == 3 || progress.streak_days == 7) {
            reward.rare_charges = 1;
        } else if (progress.streak_days == 14) {
            reward.epic_charges = 1;
        } else if (progress.streak_days == 30) {
            reward.epic_charges = 2;
        }

        progress.guaranteed_rare += reward.rare_charges;
        progress.guaranteed_epic += reward.epic_charges;

        StringBuilder sb = new StringBuilder("Серия входов: ").append(progress.streak_days)
            .append(" дн. (+").append(reward.favor).append(" Благосклонности)");
        if (reward.rare_charges > 0) {
            sb.append(", гарантированная редкая вещь");
        }
        if (reward.epic_charges > 0) {
            sb.append(", гарантированная эпическая вещь");
        }
        reward.text = sb.toString();
        return reward;
    }

    private static void addActiveDay(PlayerProgress progress, int day) {
        if (progress.active_days == null) {
            progress.active_days = new ArrayList<>();
        }
        if (!progress.active_days.contains(day)) {
            progress.active_days.add(day);
        }
        // держим только последние 30 дней — файл не должен расти
        int cut = day - 30;
        progress.active_days.removeIf(d -> d < cut);
    }

    /**
     * Награда за веху уровня. Возвращает {@code null}, если уровень не веха
     * или награда за неё уже выдавалась.
     */
    public static Reward onLevelUp(PlayerProgress progress, int level, GameMasterConfig config) {
        if (progress == null || config == null || !config.retention_enabled) {
            return null;
        }
        int every = Math.max(1, config.retention_milestone_every);
        if (level <= progress.last_milestone_level || level % every != 0) {
            return null;
        }
        progress.last_milestone_level = level;
        progress.milestones_claimed++;

        Reward reward = new Reward();
        reward.milestone = true;
        reward.streak = progress.streak_days;
        reward.favor = (int) Math.round(config.retention_milestone_favor * (1.0 + level / 50.0));
        // Каждая вторая веха — эпик, остальные — редкие: ощутимая, но не бесконечная награда.
        if (progress.milestones_claimed % 2 == 0) {
            reward.epic_charges = 1;
        } else {
            reward.rare_charges = 1;
        }
        progress.guaranteed_rare += reward.rare_charges;
        progress.guaranteed_epic += reward.epic_charges;
        reward.text = "Веха: уровень " + level + "! +" + reward.favor + " Благосклонности, "
            + (reward.epic_charges > 0 ? "эпическая" : "редкая") + " вещь гарантирована";
        return reward;
    }

    /** Короткая цель на сессию: конкретная задача на входе, а не «играй как хочешь». */
    public static final class SessionGoal {
        public int target = 0;
        public int progress = 0;
        public int reward = 0;
        public boolean done = false;
        public String text = "";
    }

    /**
     * Назначает цель на текущие сутки (или возвращает уже назначенную).
     *
     * <p>Цель короткая: её видно сразу и её реально закрыть за одну сессию в 20–40 минут.
     * Награда — Благосклонность, то есть больше спавна и лута: игрок сам решает, когда
     * её потратить.
     */
    public static SessionGoal ensureSessionGoal(PlayerProgress progress, int level, long now,
                                                GameMasterConfig config) {
        if (progress == null || config == null || !config.retention_enabled
            || !config.retention_session_goal) {
            return null;
        }
        long today = dayIndex(now);
        if (progress.session_goal_day != today || progress.session_goal_target <= 0) {
            int cap = Math.max(1, config.retention_session_goal_max);
            int target = (int) Math.round(clamp(12 + level * 1.5, 12, cap));
            progress.session_goal_day = today;
            progress.session_goal_target = target;
            progress.session_goal_progress = 0;
            progress.session_goal_done = false;
            progress.session_goal_reward = (int) Math.round(
                config.retention_session_goal_favor * (1.0 + level / 50.0));
        }
        return goalOf(progress);
    }

    /** Текущая цель без назначения новой (может быть null). */
    public static SessionGoal goalOf(PlayerProgress progress) {
        if (progress == null || progress.session_goal_target <= 0) {
            return null;
        }
        SessionGoal goal = new SessionGoal();
        goal.target = progress.session_goal_target;
        goal.progress = Math.min(progress.session_goal_progress, progress.session_goal_target);
        goal.reward = progress.session_goal_reward;
        goal.done = progress.session_goal_done;
        goal.text = goal.done
            ? "Цель сессии выполнена"
            : "Цель сессии: убить " + goal.target + " мобов (" + goal.progress + "/" + goal.target
            + ") → +" + goal.reward + " Благосклонности";
        return goal;
    }

    /**
     * Учитывает убийство в цели сессии.
     *
     * @return награда, если цель только что выполнена, иначе 0
     */
    public static int registerKill(PlayerProgress progress, long now, GameMasterConfig config) {
        if (progress == null || config == null || !config.retention_session_goal) {
            return 0;
        }
        if (progress.session_goal_done || progress.session_goal_target <= 0) {
            return 0;
        }
        if (progress.session_goal_day != dayIndex(now)) {
            return 0;
        }
        progress.session_goal_progress++;
        if (progress.session_goal_progress >= progress.session_goal_target) {
            progress.session_goal_done = true;
            return Math.max(0, progress.session_goal_reward);
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Пакет возвращения
    // ------------------------------------------------------------------

    /**
     * Стоит ли выдать пакет возвращения: игрок отсутствовал дольше
     * {@code retention_return_days}. Возвращаться должно быть выгодно — иначе
     * после перерыва человек просто не заходит, чтобы «не начинать с нуля».
     */
    public static boolean shouldGrantReturn(PlayerProgress progress, long now, GameMasterConfig config) {
        if (progress == null || config == null || !config.retention_enabled
            || !config.retention_return_package || progress.last_seen <= 0) {
            return false;
        }
        long gap = now - progress.last_seen;
        return gap >= Math.max(1, config.retention_return_days) * 86400L;
    }

    /** Множитель опыта после перерыва: 1, если бонус не действует. */
    public static double returnExpBonus(PlayerProgress progress, long now, GameMasterConfig config) {
        if (progress == null || config == null || !config.retention_return_package) {
            return 1;
        }
        if (progress.return_until <= now) {
            return 1;
        }
        return Math.max(1, config.retention_return_exp_bonus);
    }

    /** Текст-приглашение вернуться (показывается при входе после перерыва). */
    public static String returnText(GameMasterConfig config) {
        int rare = config == null ? 0 : config.retention_return_rare;
        double hours = config == null ? 0 : config.retention_return_hours;
        double bonus = config == null ? 1 : config.retention_return_exp_bonus;
        StringBuilder sb = new StringBuilder("С возвращением! ");
        if (hours > 0 && bonus > 1) {
            sb.append("Опыт ×").append(String.format("%.2f", bonus)).append(" на ")
                .append(fmtHours(hours)).append(". ");
        }
        if (rare > 0) {
            sb.append("Гарантированная редкая вещь уже ждёт в следующем дропе.");
        }
        return sb.toString().trim();
    }

    private static String fmtHours(double hours) {
        if (hours < 1) {
            return Math.round(hours * 60) + " мин";
        }
        return (hours == Math.floor(hours) ? String.valueOf((int) hours) : String.format("%.1f", hours)) + " ч";
    }

    /**
     * Шанс «принудительного» униката за один дроп. Растёт с числом убийств
     * без униката: 0 до порога, затем линейно до 0.9.
     */
    public static double pityChance(int killsSinceUnique, GameMasterConfig config) {
        if (config == null || !config.retention_pity) {
            return 0;
        }
        int start = Math.max(1, config.retention_pity_start_kills);
        int full = Math.max(start + 1, config.retention_pity_full_kills);
        if (killsSinceUnique < start) {
            return 0;
        }
        double t = clamp((killsSinceUnique - start) / (double) (full - start), 0, 1);
        return 0.9 * t;
    }

    /**
     * Риск ухода, 0…1. Считается по трём наблюдаемым сигналам:
     * давности последнего входа, длине серии и наличию сессий вообще.
     */
    public static double churnRisk(PlayerProgress progress, long now) {
        if (progress == null) {
            return 1;
        }
        double daysSince = progress.last_seen > 0
            ? Math.max(0, (now - progress.last_seen) / 86400.0)
            : 14;
        double decay = clamp(daysSince / 14.0, 0, 1);
        double streakFactor = clamp(1.0 - progress.streak_days / 7.0, 0, 1);
        double newbie = progress.sessions <= 0 ? 0.25 : 0;
        return clamp(0.6 * decay + 0.2 * streakFactor + newbie + (progress.level >= 50 ? -0.1 : 0), 0, 1);
    }

    /** Сколько разных дней играл за последние {@code window} дней (для D1/D7). */
    public static int activeDaysWithin(PlayerProgress progress, int window, long now) {
        if (progress == null || progress.active_days == null) {
            return 0;
        }
        int today = (int) dayIndex(now);
        int count = 0;
        for (int day : progress.active_days) {
            if (today - day < window) {
                count++;
            }
        }
        return count;
    }

    /** День игры номер N (1 — первый день). 0 — ещё не играл. */
    public static int dayOfPlay(PlayerProgress progress, long now) {
        if (progress == null || progress.first_seen <= 0) {
            return 0;
        }
        return (int) (dayIndex(now) - dayIndex(progress.first_seen)) + 1;
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
