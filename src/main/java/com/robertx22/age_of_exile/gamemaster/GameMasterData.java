package com.robertx22.age_of_exile.gamemaster;

import java.util.HashMap;
import java.util.Map;

/**
 * Сохраняемые данные GameMaster: прогресс игроков и замеры времени по уровням.
 * Файл: {@code config/age_of_epoch/gamemaster_data.json}
 */
public class GameMasterData {

    public static final int MAX_HISTORY_POINTS = 1200;

    public int version = 1;

    /** uuid → прогресс. */
    public Map<String, PlayerProgress> players = new HashMap<>();

    /** уровень → суммарное время. */
    public Map<String, LevelTimeStat> level_times = new HashMap<>();

    /** Сколько всего секунд базового времени замерено (для быстрого расчёта). */
    public double observed_seconds = 0;

    public GameMasterData() {
    }

    public PlayerProgress getOrCreate(String uuid, String name) {
        if (players == null) {
            players = new HashMap<>();
        }
        PlayerProgress progress = players.get(uuid);
        if (progress == null) {
            progress = new PlayerProgress();
            progress.uuid = uuid;
            progress.name = name;
            long now = System.currentTimeMillis() / 1000L;
            progress.first_seen = now;
            progress.last_seen = now;
            progress.last_snapshot_epoch = now;
            players.put(uuid, progress);
        }
        if (name != null && !name.isEmpty()) {
            progress.name = name;
        }
        progress.online = true;
        return progress;
    }

    public PlayerProgress get(String uuid) {
        return players == null ? null : players.get(uuid);
    }

    /** Дописывает время, проведённое на уровне, в общую таблицу. */
    public void commitLevelTime(int level, double baselineSeconds, int completions) {
        if (level_times == null) {
            level_times = new HashMap<>();
        }
        if (baselineSeconds <= 0 && completions <= 0) {
            return;
        }
        LevelTimeStat stat = level_times.computeIfAbsent(String.valueOf(level), k -> new LevelTimeStat());
        stat.add(baselineSeconds, completions);
        observed_seconds += Math.max(0, baselineSeconds);
    }

    /** Убирает прогресс игроков и замеры (не трогая конфиг). */
    public void reset() {
        players = new HashMap<>();
        level_times = new HashMap<>();
        observed_seconds = 0;
    }
}
