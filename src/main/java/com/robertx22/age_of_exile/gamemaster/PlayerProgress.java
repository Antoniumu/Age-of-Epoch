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

    public double playtimeHours() {
        return playtime_seconds / 3600.0;
    }

    public double baselineHours() {
        return baseline_seconds / 3600.0;
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
