package com.robertx22.age_of_exile.gamemaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // ------------------------------------------------------------------
    // Маленький сервер (см. Social)
    // ------------------------------------------------------------------

    /** Активность по локальным часам: сколько «человеко-секунд» сыграно в каждый час. */
    public long[] hour_activity = new long[24];

    /** «Первые» события сервера: ключ события → ник игрока. */
    public Map<String, String> firsts = new HashMap<>();

    /** Номер текущей недели (UTC-эпоха / 7 дней). */
    public long week_index = 0;

    /** Убийства всех игроков за текущую неделю. */
    public int week_kills = 0;

    /** Когда цель недели была выполнена впервые (epoch seconds), 0 — ещё нет. */
    public long week_goal_epoch = 0;

    /** Кто уже получил награду за цель недели. */
    public List<String> week_claimed = new ArrayList<>();

    /** Когда последний раз напоминали о цели недели (epoch seconds). */
    public long last_goal_hint = 0;

    /** Телеметрия: сезон, счётчики, профили (см. Telemetry). */
    public Telemetry.State telemetry = new Telemetry.State();

    /** Добавляет человеко-секунды в корзину часа (0–23). */
    public void addActivity(int hour, long seconds) {
        if (hour < 0 || hour > 23) {
            return;
        }
        if (hour_activity == null || hour_activity.length != 24) {
            hour_activity = new long[24];
        }
        if (seconds <= 0) {
            return;
        }
        // потолок на корзину: за 6 недель работы сервера данные остаются осмысленными
        hour_activity[hour] = Math.min(Long.MAX_VALUE / 4, hour_activity[hour] + seconds);
    }

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
        hour_activity = new long[24];
        firsts = new HashMap<>();
        week_index = 0;
        week_kills = 0;
        week_goal_epoch = 0;
        week_claimed = new ArrayList<>();
        last_goal_hint = 0;
        telemetry = new Telemetry.State();
    }
}
