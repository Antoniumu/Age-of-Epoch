package com.robertx22.age_of_exile.gamemaster;

import java.util.ArrayList;
import java.util.List;

/**
 * Накопленная статистика одного игрока (сохраняется в gamemaster_data.json).
 */
public class PlayerProgress {

    public String uuid = "";
    public String name = "";

    /** Последний известный уровень. */
    public int level = 1;

    /** Последняя Благосклонность (используется для отображения spawn-rate в панели, включая offline игроков). */
    public int favor = 100;

    /** Реальные секунды игры (без AFK, если он включён в конфиге). */
    public double playtime_seconds = 0;

    /** «Базовые» секунды: время, пересчитанное так, как будто множитель опыта был 1. */
    public double baseline_seconds = 0;

    /** Базовые секунды, накопленные на текущем уровне (ещё не зачтены в таблицу). */
    public double level_baseline_seconds = 0;

    /** Сколько опыта реально выдано (после всех множителей). */
    public double exp_gained = 0;

    /** Сколько опыта было бы выдано без GameMaster. */
    public double raw_exp_gained = 0;

    /** Последний рассчитанный итоговый множитель. */
    public double multiplier = 1;

    // ------------------------------------------------------------------
    // Автобаланс (см. AutoBalance)
    // ------------------------------------------------------------------

    /** Сглаженное время прохождения уровня в реальных минутах. 0 — замеров ещё нет. */
    public double ema_minutes_per_level = 0;

    /** Значение playtime_seconds на момент получения текущего уровня. */
    public double level_started_playtime = 0;

    /** Смертей на текущем уровне (сбрасывается при повышении). */
    public int deaths_since_level = 0;

    /** Смертей всего. */
    public int deaths = 0;

    /** Текущий множитель опыта от автобаланса. */
    public double auto_exp_multiplier = 1;
    public double auto_mob_health = 1;
    public double auto_mob_damage = 1;
    public double auto_loot = 1;
    /** Множитель частоты спавна от автобаланса (итог всегда клампится в 0…1). */
    public double auto_spawn = 1;
    /** 0…1: насколько игрок близок к целевому темпу. */
    public double auto_flow = 0.5;
    /** Состояние автобаланса: stable / rescue / brake / push / welcome. */
    public String auto_state = "stable";
    /** Когда последний раз пересчитывали автобаланс (epoch seconds). */
    public long auto_updated_epoch = 0;

    /** Уровней, пройденных внутри целевого коридора (для панели). */
    public int levels_at_pace = 0;
    /** Уровней пройдено всего (для панели). */
    public int levels_gained = 0;

    /** Длительность текущей сессии в секундах (без AFK). */
    public double session_seconds = 0;
    /** Количество сессий. */
    public int sessions = 0;
    /** Средняя длительность сессии в секундах. */
    public double avg_session_seconds = 0;

    // ------------------------------------------------------------------
    // Удержание (см. Retention)
    // ------------------------------------------------------------------

    /** Длина текущей серии ежедневных входов. */
    public int streak_days = 0;
    /** Номер суток (epoch day) последнего засчитанного входа. */
    public long last_session_day = 0;
    /** Последние 30 активных суток — для D1/D7 и риска ухода. */
    public List<Integer> active_days = new ArrayList<>();
    /** Последний уровень, за который выдана веха. */
    public int last_milestone_level = 0;
    /** Сколько вех получено. */
    public int milestones_claimed = 0;
    /** Сколько вещей было сгенерировано с момента последнего униката (pity). */
    public int kills_since_unique = 0;
    /** Заряды гарантированной редкой/эпической вещи (тратятся на дропе). */
    public int guaranteed_rare = 0;
    public int guaranteed_epic = 0;

    /** Тип ежедневной цели: 0 — убийства, 1 — сундуки, 2 — ремесло. */
    public int session_goal_kind = 0;

    /** Цель на сессию: сколько убийств нужно и сколько уже сделано. */
    public int session_goal_target = 0;
    public int session_goal_progress = 0;
    /** Награда за выполненную цель (Благосклонность). */
    public int session_goal_reward = 0;
    /** Сутки, на которые назначена цель. */
    public long session_goal_day = 0;
    public boolean session_goal_done = false;

    /** До какого момента (epoch seconds) действует усиленный опыт после перерыва. */
    public long return_until = 0;

    /** Пройденное расстояние в блоках (для профиля «исследователь / фарм на месте»). */
    public double distance_travelled = 0;

    /** Когда последний раз сэмплировали биом/измерение (телеметрия). */
    public transient long telemetry_sample_epoch = 0;

    // ------------------------------------------------------------------
    // Pity для рун/самоцветов, мягкая смерть и кодекс
    // ------------------------------------------------------------------

    /** Убийств без выпавшей руны (pity). */
    public int kills_since_rune = 0;

    /** Убийств без выпавшего самоцвета (pity). */
    public int kills_since_gem = 0;

    /** До какого момента действует «печать» после смерти (epoch seconds). */
    public long seal_until = 0;

    /** Сколько убийств осталось, чтобы снять печать досрочно. */
    public int seal_kills_left = 0;

    /** Сколько раз печать уже вешали (для статистики). */
    public int seal_count = 0;

    /** Собранные уникаты (id предметов) — кодекс. */
    public List<String> codex = new ArrayList<>();

    /** Сколько ступеней награды за кодекс уже выдано. */
    public int codex_rewarded = 0;

    /** До какого уровня доигран онбординг (подсказка «куда идти дальше»). */
    public int onboarding_level = 0;

    /** Время последней активности (epoch seconds). */
    public long last_seen = 0;

    /** Время первого появления (epoch seconds). */
    public long first_seen = 0;

    /** Точки для графиков. */
    public List<HistoryPoint> history = new ArrayList<>();

    // --- transient (не сохраняются) ---

    public transient double last_snapshot_playtime = -1;
    public transient long last_snapshot_epoch = 0;
    public transient double last_x = 0;
    public transient double last_y = 0;
    public transient double last_z = 0;
    public transient boolean has_pos = false;
    public transient long last_moved_epoch = 0;
    public transient boolean online = false;
    /** До какого момента (epoch seconds) действует «тёплый приём» после долгого перерыва. */
    public transient long welcome_until = 0;

    public double playtimeHours() {
        return playtime_seconds / 3600.0;
    }

    public double baselineHours() {
        return baseline_seconds / 3600.0;
    }

    /** Минут, проведённых на текущем уровне. */
    public double minutesAtLevel() {
        if (level_started_playtime <= 0) {
            return 0;
        }
        return Math.max(0, (playtime_seconds - level_started_playtime) / 60.0);
    }

    public void addHistory(double multiplier, long epochSeconds) {
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(new HistoryPoint(playtimeHours(), level, multiplier, epochSeconds));

        // не даём файлу расти бесконечно: прореживаем вдвое
        if (history.size() > GameMasterData.MAX_HISTORY_POINTS) {
            List<HistoryPoint> thinned = new ArrayList<>(history.size() / 2 + 2);
            for (int i = 0; i < history.size(); i++) {
                if (i % 2 == 0 || i == history.size() - 1) {
                    thinned.add(history.get(i));
                }
            }
            history = thinned;
        }
    }

    public static class HistoryPoint {
        public double playtime_hours;
        public int level;
        public double multiplier;
        public long time;

        public HistoryPoint() {
        }

        public HistoryPoint(double playtimeHours, int level, double multiplier, long time) {
            this.playtime_hours = playtimeHours;
            this.level = level;
            this.multiplier = multiplier;
            this.time = time;
        }
    }
}
