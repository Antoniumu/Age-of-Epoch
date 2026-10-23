package com.robertx22.age_of_exile.gamemaster;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.robertx22.age_of_exile.gamemaster.PlayerProgress.HistoryPoint;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ядро системы GameMaster.
 * <p>
 * Считает реальное время игры, хранит историю прогресса, вычисляет множители опыта
 * и spawn-rate мобов, сохраняет всё в JSON и обслуживает веб-панель.
 * <p>
 * Все методы, работающие с {@link PlayerProgress}/{@link GameMasterData}, вызываются
 * только из серверного потока. Веб-сервер маршалит свои запросы в основной поток.
 */
public class GameMaster {

    private static GameMaster INSTANCE;

    public static GameMaster get() {
        if (INSTANCE == null) {
            INSTANCE = new GameMaster();
        }
        return INSTANCE;
    }

    /** true, если GameMaster загружен и может применять настройки опыта/спавна. */
    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.loaded && INSTANCE.server != null;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    private final GameMasterConfig config = new GameMasterConfig();
    private final GameMasterData data = new GameMasterData();

    /** Телеметрия: журнал событий, агрегаты, отчёты по сезону. */
    private final Telemetry telemetry = new Telemetry(this);

    private boolean loaded = false;
    private boolean dirty = false;

    private MinecraftServer server;
    private GameMasterWebServer webServer;
    private String webStartedHost = null;
    private int webStartedPort = -1;
    private String webStartedToken = null;

    /** Кэш максимального уровня, чтобы не дёргать реестр в горячем пути. */
    private int maxLevelCache = 50;

    /** Кэш калибровки. */
    private double cachedK = 0;
    private double cachedBaselineHours = 40;
    private double measuredBaselineHours = -1;
    private double measuredObservedHours = 0;
    private double cachedReferenceLevel = 1;
    private boolean cachedHasPeers = false;
    private int cachedPeers = 0;

    /** Серверные множители от автобаланса (действуют на всех мобов). */
    private double autoMobHealth = 1.0;
    private double autoMobDamage = 1.0;
    private double autoLoot = 1.0;
    private long autoGlobalUpdated = 0;
    private double autoTargetHours = 0;

    /** Сколько игроков онлайн на последнем тике. */
    private int onlineCache = 0;

    /** Сколько из них реально играют (без AFK) — от этого считается кооп-бонус. */
    private int coopCache = 1;

    private long lastSecondTick = 0;
    private long lastStatsTick = 0;
    private long lastSaveTick = 0;

    // ------------------------------------------------------------------
    // Жизненный цикл
    // ------------------------------------------------------------------

    public static Path baseDir() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve("age_of_epoch");
    }

    public static Path configPath() {
        return baseDir().resolve("gamemaster.json");
    }

    public static Path dataPath() {
        return baseDir().resolve("gamemaster_data.json");
    }

    public void start(MinecraftServer server) {
        this.server = server;
        if (loaded) {
            restartWeb();
            return;
        }
        loadConfig();
        loadData();

        long now = now();
        cachedK = 0;
        cachedBaselineHours = Math.max(0.01, config.assumed_baseline_hours);
        lastSecondTick = 0;
        lastStatsTick = 0;
        lastSaveTick = now;
        dirty = true;
        loaded = true;

        recomputeStats(now);
        restartWeb();
        save();

        System.out.println("[GameMaster] Система запущена. Отслежено игроков: " + data.players.size()
            + ", базовое время до макс. уровня: " + fmt(cachedBaselineHours) + " ч"
            + ", множитель-калибровка k=" + fmt(cachedK));

        if (config.web_enabled) {
            System.out.println("[GameMaster] Панель: http://" + config.web_host + ":" + config.web_port + "/");
        }
    }

    public void stop() {
        // Ночное выключение: закрываем сессии тех, кто ещё онлайн, чтобы время
        // и статистика не «повисли» до утра, и чтобы сессия не посчиталась дважды.
        try {
            finalizeOnlineSessions();
        } catch (Exception ignored) {
        }
        try {
            telemetry.close();
        } catch (Exception ignored) {
        }
        stopWeb();
        save();
        for (PlayerProgress progress : data.players.values()) {
            progress.online = false;
        }
        loaded = false;
        server = null;
    }

    /**
     * Закрывает сессии всех онлайн-игроков (при остановке сервера).
     * Сервер гасится по ночам, поэтому это штатный путь, а не аварийный.
     */
    private void finalizeOnlineSessions() {
        if (server == null) {
            return;
        }
        long now = now();
        for (ServerPlayer player : server.getPlayerList()
            .getPlayers()) {
            try {
                PlayerProgress progress = data.get(player.getUUID()
                    .toString());
                if (progress == null || !progress.online) {
                    continue;
                }
                telemetry.logout(player, progress.session_seconds);
                accumulateSession(progress);
                progress.online = false;
                progress.last_seen = now;
            } catch (Exception ignored) {
            }
        }
        dirty = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public GameMasterConfig config() {
        return config;
    }

    public Telemetry telemetry() {
        return telemetry;
    }

    /** Пометить данные как изменённые (используется телеметрией). */
    public void markDirty() {
        dirty = true;
    }

    public GameMasterData data() {
        return data;
    }

    // ------------------------------------------------------------------
    // Сохранение / загрузка
    // ------------------------------------------------------------------

    public void loadConfig() {
        try {
            Files.createDirectories(baseDir());
            if (Files.exists(configPath())) {
                try (BufferedReader reader = Files.newBufferedReader(configPath(), StandardCharsets.UTF_8)) {
                    GameMasterConfig parsed = GameMasterConfig.GSON.fromJson(reader, GameMasterConfig.class);
                    if (parsed != null) {
                        // Файл без версии = старый. Тогда Благосклонность стартовала со 100,
                        // а база спавна была 100. Сейчас старт 500 — при старой базе спавн
                        // падал бы до x0.167. Поднимаем базу до стартового favor.
                        if (parsed.config_version == null) {
                            if (parsed.spawn_rate_favor_reference <= 100.0) {
                                parsed.spawn_rate_favor_reference = 500.0;
                            }
                            parsed.config_version = GameMasterConfig.CONFIG_VERSION;
                            System.out.println("[GameMaster] Конфиг обновлён до версии "
                                + GameMasterConfig.CONFIG_VERSION + ": база спавна по Благосклонности = "
                                + fmt(parsed.spawn_rate_favor_reference));
                        }
                        copyConfig(parsed);
                    }
                }
            } else {
                config.sanitize();
                saveConfig();
            }
        } catch (Exception e) {
            System.out.println("[GameMaster] Не удалось прочитать конфиг: " + e);
            e.printStackTrace();
        }
        config.sanitize();
    }

    private void copyConfig(GameMasterConfig parsed) {
        config.enabled = parsed.enabled;
        config.catch_up = parsed.catch_up;
        config.catch_up_per_level = parsed.catch_up_per_level;
        config.catch_up_max_bonus = parsed.catch_up_max_bonus;
        config.catch_up_penalize_ahead = parsed.catch_up_penalize_ahead;
        config.catch_up_penalty_per_level = parsed.catch_up_penalty_per_level;
        config.catch_up_min_multiplier = parsed.catch_up_min_multiplier;
        config.catch_up_reference_blend = parsed.catch_up_reference_blend;
        config.catch_up_min_playtime_minutes = parsed.catch_up_min_playtime_minutes;
        config.catch_up_activity_window_days = parsed.catch_up_activity_window_days;
        config.pacing = parsed.pacing;
        config.target_hours = parsed.target_hours;
        config.pacing_start_level = parsed.pacing_start_level;
        config.pacing_max_penalty = parsed.pacing_max_penalty;
        config.pacing_max_bonus = parsed.pacing_max_bonus;
        config.pacing_curve = parsed.pacing_curve;
        config.pacing_auto_calibrate = parsed.pacing_auto_calibrate;
        config.assumed_baseline_hours = parsed.assumed_baseline_hours;
        config.spawn_rate_enabled = parsed.spawn_rate_enabled;
        config.spawn_rate_dynamic = parsed.spawn_rate_dynamic;
        config.spawn_rate_manual_multiplier = parsed.spawn_rate_manual_multiplier;
        config.spawn_rate_favor_reference = parsed.spawn_rate_favor_reference;
        config.spawn_rate_dynamic_min_multiplier = parsed.spawn_rate_dynamic_min_multiplier;
        config.spawn_rate_player_overrides = parsed.spawn_rate_player_overrides == null
            ? new HashMap<>() : new HashMap<>(parsed.spawn_rate_player_overrides);
        config.exp_multiplier_min = parsed.exp_multiplier_min;
        config.exp_multiplier_max = parsed.exp_multiplier_max;
        config.auto_balance = parsed.auto_balance;
        config.auto_start_minutes = parsed.auto_start_minutes;
        config.auto_end_minutes = parsed.auto_end_minutes;
        config.auto_curve = parsed.auto_curve;
        config.auto_dead_zone = parsed.auto_dead_zone;
        config.auto_exp_min = parsed.auto_exp_min;
        config.auto_exp_max = parsed.auto_exp_max;
        config.auto_exp_step_per_minute = parsed.auto_exp_step_per_minute;
        config.auto_deaths_brake = parsed.auto_deaths_brake;
        config.auto_challenge_deaths = parsed.auto_challenge_deaths;
        config.auto_rescue_factor = parsed.auto_rescue_factor;
        config.auto_mob_health_min = parsed.auto_mob_health_min;
        config.auto_mob_health_max = parsed.auto_mob_health_max;
        config.auto_mob_damage_min = parsed.auto_mob_damage_min;
        config.auto_mob_damage_max = parsed.auto_mob_damage_max;
        config.auto_loot_max = parsed.auto_loot_max;
        config.auto_spawn_min = parsed.auto_spawn_min;
        config.auto_spawn_max = parsed.auto_spawn_max;
        config.retention_enabled = parsed.retention_enabled;
        config.retention_daily_favor = parsed.retention_daily_favor;
        config.retention_streak_bonus_favor = parsed.retention_streak_bonus_favor;
        config.retention_streak_max_favor = parsed.retention_streak_max_favor;
        config.retention_streak_max_days = parsed.retention_streak_max_days;
        config.retention_streak_grace_days = parsed.retention_streak_grace_days;
        config.retention_milestone_every = parsed.retention_milestone_every;
        config.retention_milestone_favor = parsed.retention_milestone_favor;
        config.retention_pity = parsed.retention_pity;
        config.retention_pity_start_kills = parsed.retention_pity_start_kills;
        config.retention_pity_full_kills = parsed.retention_pity_full_kills;
        config.retention_no_exp_loss_level = parsed.retention_no_exp_loss_level;
        config.retention_session_goal = parsed.retention_session_goal;
        config.retention_session_goal_favor = parsed.retention_session_goal_favor;
        config.retention_session_goal_max = parsed.retention_session_goal_max;
        config.retention_return_package = parsed.retention_return_package;
        config.retention_return_days = parsed.retention_return_days;
        config.retention_return_hours = parsed.retention_return_hours;
        config.retention_return_exp_bonus = parsed.retention_return_exp_bonus;
        config.retention_return_rare = parsed.retention_return_rare;
        config.retention_pity_runes = parsed.retention_pity_runes;
        config.retention_pity_rune_start_kills = parsed.retention_pity_rune_start_kills;
        config.retention_pity_rune_full_kills = parsed.retention_pity_rune_full_kills;
        config.retention_pity_gem_start_kills = parsed.retention_pity_gem_start_kills;
        config.retention_pity_gem_full_kills = parsed.retention_pity_gem_full_kills;
        config.retention_soft_death = parsed.retention_soft_death;
        config.retention_seal_minutes = parsed.retention_seal_minutes;
        config.retention_seal_kills = parsed.retention_seal_kills;
        config.retention_seal_exp_penalty = parsed.retention_seal_exp_penalty;
        config.social_goal_hint = parsed.social_goal_hint;
        config.social_goal_hint_minutes = parsed.social_goal_hint_minutes;
        config.codex_enabled = parsed.codex_enabled;
        config.codex_reward_step = parsed.codex_reward_step;
        config.codex_reward_favor = parsed.codex_reward_favor;
        config.codex_reward_epic = parsed.codex_reward_epic;
        config.social_enabled = parsed.social_enabled;
        config.social_coop_exp_per_player = parsed.social_coop_exp_per_player;
        config.social_coop_loot_per_player = parsed.social_coop_loot_per_player;
        config.social_coop_spawn_per_player = parsed.social_coop_spawn_per_player;
        config.social_coop_max = parsed.social_coop_max;
        config.social_group_content = parsed.social_group_content;
        config.social_group_hp_per_player = parsed.social_group_hp_per_player;
        config.social_group_hp_max = parsed.social_group_hp_max;
        config.social_group_radius = parsed.social_group_radius;
        config.social_prime_hint = parsed.social_prime_hint;
        config.social_prime_window_hours = parsed.social_prime_window_hours;
        config.social_announce_firsts = parsed.social_announce_firsts;
        config.social_goal_enabled = parsed.social_goal_enabled;
        config.social_goal_kills = parsed.social_goal_kills;
        config.social_goal_favor = parsed.social_goal_favor;
        config.social_goal_rare = parsed.social_goal_rare;
        config.telemetry_enabled = parsed.telemetry_enabled;
        config.telemetry_events = parsed.telemetry_events;
        config.telemetry_sample_seconds = parsed.telemetry_sample_seconds;
        config.telemetry_max_file_mb = parsed.telemetry_max_file_mb;
        config.telemetry_retention_days = parsed.telemetry_retention_days;
        config.telemetry_season = parsed.telemetry_season;
        config.telemetry_panel_top = parsed.telemetry_panel_top;
        config.config_version = parsed.config_version;
        config.auto_session_fatigue_minutes = parsed.auto_session_fatigue_minutes;
        config.auto_welcome_hours = parsed.auto_welcome_hours;
        config.auto_touch_mobs = parsed.auto_touch_mobs;
        config.auto_touch_loot = parsed.auto_touch_loot;
        config.auto_global_step_per_minute = parsed.auto_global_step_per_minute;
        config.afk_exclusion = parsed.afk_exclusion;
        config.afk_threshold_seconds = parsed.afk_threshold_seconds;
        config.snapshot_interval_seconds = parsed.snapshot_interval_seconds;
        config.save_interval_seconds = parsed.save_interval_seconds;
        config.show_multiplier_in_actionbar = parsed.show_multiplier_in_actionbar;
        config.announce_on_login = parsed.announce_on_login;
        config.web_enabled = parsed.web_enabled;
        config.web_host = parsed.web_host;
        config.web_port = parsed.web_port;
        config.web_token = parsed.web_token;
    }

    public void loadData() {
        data.reset();
        try {
            Files.createDirectories(baseDir());
            if (Files.exists(dataPath())) {
                try (BufferedReader reader = Files.newBufferedReader(dataPath(), StandardCharsets.UTF_8)) {
                    GameMasterData parsed = GSON.fromJson(reader, GameMasterData.class);
                    if (parsed != null) {
                        data.version = parsed.version;
                        data.players = parsed.players != null ? parsed.players : new java.util.HashMap<>();
                        data.level_times = parsed.level_times != null ? parsed.level_times : new java.util.HashMap<>();
                        data.observed_seconds = parsed.observed_seconds;
                    }
                }
            }
            for (PlayerProgress progress : data.players.values()) {
                if (progress.history == null) {
                    progress.history = new ArrayList<>();
                }
                if (progress.level < 1) {
                    progress.level = 1;
                }
                if (progress.auto_exp_multiplier <= 0) {
                    progress.auto_exp_multiplier = 1;
                }
                if (progress.auto_mob_health <= 0) {
                    progress.auto_mob_health = 1;
                }
                if (progress.auto_mob_damage <= 0) {
                    progress.auto_mob_damage = 1;
                }
                if (progress.auto_loot <= 0) {
                    progress.auto_loot = 1;
                }
                if (progress.auto_state == null) {
                    progress.auto_state = "stable";
                }
                progress.online = false;
                progress.last_snapshot_playtime = progress.playtime_seconds;
                progress.last_snapshot_epoch = progress.last_seen;
            }
        } catch (Exception e) {
            System.out.println("[GameMaster] Не удалось прочитать данные: " + e);
            e.printStackTrace();
        }
    }

    public void save() {
        if (!loaded) {
            return;
        }
        try {
            Files.createDirectories(baseDir());
            saveConfig();
            writeAtomic(dataPath(), GSON.toJson(data));
            dirty = false;
        } catch (Exception e) {
            System.out.println("[GameMaster] Не удалось сохранить данные: " + e);
            e.printStackTrace();
        }
    }

    private void saveConfig() {
        try {
            Files.createDirectories(baseDir());
            writeAtomic(configPath(), GameMasterConfig.GSON.toJson(config));
        } catch (Exception e) {
            System.out.println("[GameMaster] Не удалось сохранить конфиг: " + e);
        }
    }

    private static void writeAtomic(Path target, String json) throws Exception {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Перечитать файл конфига (команда /gm reload). */
    public void reloadConfig() {
        loadConfig();
        onConfigEdited();
    }

    /** Вызывается после любого изменения конфига (команда или веб-панель). */
    public void onConfigEdited() {
        config.sanitize();
        saveConfig();
        restartWeb();
        long now = now();
        recomputeStats(now);
        dirty = true;
        save();
    }

    public void resetData() {
        data.reset();
        cachedK = 0;
        cachedBaselineHours = Math.max(0.01, config.assumed_baseline_hours);
        measuredBaselineHours = -1;
        measuredObservedHours = 0;
        recomputeStats(now());
        dirty = true;
        save();
    }

    // ------------------------------------------------------------------
    // Тики и учёт времени
    // ------------------------------------------------------------------

    public void onServerTick(MinecraftServer server) {
        if (!loaded || this.server == null) {
            return;
        }
        this.server = server;

        long nowMs = System.currentTimeMillis();
        long deltaMs = nowMs - lastSecondTick;
        if (deltaMs < 1000L) {
            return;
        }
        lastSecondTick = nowMs;
        long now = nowMs / 1000L;

        // если сервер лагал, честно засчитываем пропущенные реальные секунды
        int seconds = (int) Math.min(60, Math.max(1, deltaMs / 1000L));

        // Маленький сервер: кто вообще онлайн и когда обычно собирается компания.
        List<ServerPlayer> online = server.getPlayerList()
            .getPlayers();
        onlineCache = online.size();
        if (config.social_enabled) {
            ensureWeek(now);
            if (config.social_prime_hint && onlineCache > 0) {
                data.addActivity(Social.localHour(now), (long) seconds * onlineCache);
            }
        }

        for (ServerPlayer player : server.getPlayerList()
            .getPlayers()) {
            try {
                tickPlayer(player, now, seconds);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Кооп-бонус считается по реально играющим: AFK-персонаж не должен
        // «зарабатывать» бонус для остальных.
        int activePlayers = 0;
        for (ServerPlayer player : online) {
            PlayerProgress progress = data.get(player.getUUID()
                .toString());
            boolean afk = config.afk_exclusion && progress != null
                && now - progress.last_moved_epoch > Math.max(5, config.afk_threshold_seconds);
            if (!afk) {
                activePlayers++;
            }
        }
        coopCache = Math.max(1, activePlayers);

        telemetry.flush();
        maybeHintWeeklyGoal(now);

        if (now - lastStatsTick >= 5) {
            recomputeStats(now);
        }
        if (now - lastSaveTick >= Math.max(5, config.save_interval_seconds)) {
            lastSaveTick = now;
            if (dirty) {
                save();
            }
        }
    }

    /** Возвращает запись игрока, при первом появлении сразу запоминая его уровень. */
    private PlayerProgress progressFor(ServerPlayer player) {
        PlayerProgress progress = data.getOrCreate(player.getUUID()
            .toString(), player.getGameProfile()
            .name());
        if (progress.history.isEmpty() && progress.playtime_seconds <= 0) {
            try {
                progress.level = Load.Unit(player)
                    .getLevel();
            } catch (Exception ignored) {
            }
            progress.level_baseline_seconds = 0;
        }
        if (progress.level_started_playtime <= 0) {
            progress.level_started_playtime = progress.playtime_seconds;
        }
        try {
            progress.favor = Load.playerRPGData(player).favor.getFavor();
        } catch (Exception ignored) {
        }
        return progress;
    }

    private void tickPlayer(ServerPlayer player, long now, int seconds) {
        PlayerProgress progress = progressFor(player);

        checkLevel(player, progress);
        progress.last_seen = now;
        progress.online = true;

        boolean moved = updateMovement(player, progress, now);
        boolean afk = config.afk_exclusion && !moved
            && now - progress.last_moved_epoch > Math.max(5, config.afk_threshold_seconds);

        if (!afk) {
            progress.playtime_seconds += seconds;
            progress.session_seconds += seconds;

            // нормируем время так, как будто все множители были равны 1 —
            // это и есть «базовое» время сборки, по которому мы калибруемся
            double normalizer = config.enabled ? Math.max(0.05, progress.multiplier) : 1.0;
            double baseSeconds = seconds / normalizer;
            progress.baseline_seconds += baseSeconds;
            progress.level_baseline_seconds += baseSeconds;

            maybeSnapshot(progress, now);
        }

        // Телеметрия: раз в N секунд фиксируем биом, измерение и время суток.
        telemetry.sample(player, progress, afk, seconds, now);

        progress.multiplier = computeMultiplier(progress);
        dirty = true;
    }

    private boolean updateMovement(ServerPlayer player, PlayerProgress progress, long now) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        boolean moved = false;
        if (!progress.has_pos) {
            progress.has_pos = true;
            progress.last_moved_epoch = now;
            moved = true;
        } else {
            double dx = x - progress.last_x;
            double dy = y - progress.last_y;
            double dz = z - progress.last_z;
            if (dx * dx + dy * dy + dz * dz > 0.0025) {
                progress.last_moved_epoch = now;
                moved = true;
                double step = Math.sqrt(dx * dx + dy * dy + dz * dz);
                // телепорты и переходы между измерениями не считаем «исследованием»
                if (step < 64) {
                    progress.distance_travelled += step;
                    telemetry.addFor(player.getUUID()
                        .toString(), "distance", step);
                    telemetry.add("distance", step);
                }
            }
        }
        progress.last_x = x;
        progress.last_y = y;
        progress.last_z = z;
        return moved;
    }

    private void maybeSnapshot(PlayerProgress progress, long now) {
        if (progress.last_snapshot_playtime < 0) {
            progress.last_snapshot_playtime = progress.playtime_seconds;
            progress.last_snapshot_epoch = now;
        }
        int interval = Math.max(10, config.snapshot_interval_seconds);
        if (progress.playtime_seconds - progress.last_snapshot_playtime >= interval) {
            progress.last_snapshot_playtime = progress.playtime_seconds;
            progress.last_snapshot_epoch = now;
            progress.addHistory(progress.multiplier, now);
            dirty = true;
        }
        if (progress.history.isEmpty()) {
            progress.addHistory(progress.multiplier, now);
        }
    }

    /** Фиксирует переход на новый уровень и засчитывает время, проведённое на старом. */
    private void checkLevel(ServerPlayer player, PlayerProgress progress) {
        int level;
        try {
            level = Load.Unit(player)
                .getLevel();
        } catch (Exception e) {
            return;
        }
        if (level == progress.level) {
            return;
        }
        long now = now();
        if (level > progress.level) {
            int completions = level - progress.level;
            data.commitLevelTime(progress.level, progress.level_baseline_seconds, completions);

            // --- автобаланс: фиксируем фактическое время уровня и коридор темпа ---
            if (config.auto_balance) {
                double minutes = progress.level_started_playtime > 0
                    ? Math.max(0, (progress.playtime_seconds - progress.level_started_playtime) / 60.0)
                    : 0;
                if (minutes > 0) {
                    AutoBalance.recordLevel(progress, minutes);
                    double target = AutoBalance.targetMinutes(progress.level, maxLevelCache, config);
                    double deadZone = Math.max(0, Math.min(0.9, config.auto_dead_zone));
                    if (minutes <= target * (1 + deadZone)) {
                        progress.levels_at_pace++;
                    }
                }
                progress.levels_gained += Math.max(1, completions);
                progress.deaths_since_level = 0;
                progress.level_started_playtime = progress.playtime_seconds;
            }

            progress.level_baseline_seconds = 0;
            progress.level = level;

            telemetry.levelUp(player, level);

            // Онбординг: после первых 10 уровней подсказываем, куда идти дальше.
            // Без подсказки часть игроков застревает на «а что теперь делать».
            if (level >= 10 && progress.onboarding_level < 10) {
                progress.onboarding_level = 10;
                player.sendSystemMessage(Component.literal(
                        "GameMaster: первые 10 уровней позади. Дальше — башни и делвы: там падают лучшие вещи, "
                            + "а с боссов можно получить уникат. Загляни в ближайшую башню.")
                    .withStyle(ChatFormatting.YELLOW));
                telemetry.retained(player, "onboarding", "lvl10", 0);
            }

            // «Первые» события сервера: первый 10-й уровень, 20-й и т.д.
            // На маленьком сервере это личная слава — и повод зайти остальным.
            for (int milestoneLevel : new int[]{10, 20, 30, 40, 50}) {
                if (level >= milestoneLevel) {
                    recordFirst("level_" + milestoneLevel, progress.name,
                        "первым достиг " + milestoneLevel + " уровня");
                }
            }

            // Веха удержания: награда за каждый N-й уровень.
            Retention.Reward milestone = Retention.onLevelUp(progress, level, config);
            if (milestone != null) {
                grantFavor(player, progress, milestone.favor);
                player.sendSystemMessage(Component.literal(milestone.text)
                    .withStyle(ChatFormatting.GOLD));
                telemetry.retained(player, "milestone", "level=" + level, milestone.favor);
            }

            progress.addHistory(progress.multiplier, now);
            progress.last_snapshot_playtime = progress.playtime_seconds;
            progress.last_snapshot_epoch = now;
            dirty = true;
            recomputeStats(now);
        } else {
            // админ понизил уровень — просто запоминаем, без замеров
            progress.level = level;
            progress.level_baseline_seconds = 0;
        }
    }

    /**
     * Смерть игрока: копим статистику для автобаланса (смертность на уровне).
     * Вызывается из OnPlayerDeath.
     */
    public void onDeath(ServerPlayer player) {
        if (!loaded || player == null) {
            return;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress == null) {
            progress = progressFor(player);
        }
        progress.deaths++;
        progress.deaths_since_level++;
        if (progress.level_started_playtime <= 0) {
            progress.level_started_playtime = progress.playtime_seconds;
        }
        dirty = true;
    }

    /** Начисляет Благосклонность игроку и обновляет кэш в прогрессе. */
    private void grantFavor(ServerPlayer player, PlayerProgress progress, int amount) {
        if (amount <= 0) {
            return;
        }
        try {
            var rpg = Load.playerRPGData(player);
            rpg.favor.addFavor(amount);
            rpg.syncToClient(player);
            progress.favor = rpg.favor.getFavor();
        } catch (Exception e) {
            progress.favor += amount;
        }
        dirty = true;
    }

    /**
     * Учитывает убийство моба: копит счётчик для pity-системы.
     * Вызывается из {@code LootInfo} (один раз на дроп с моба).
     */
    public void onMobLooted(ServerPlayer player) {
        if (!loaded || player == null || !config.retention_enabled) {
            return;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress == null) {
            return;
        }
        long now = now();
        progress.kills_since_unique++;
        progress.kills_since_rune++;
        progress.kills_since_gem++;

        // Мягкая смерть: убийства снимают печать досрочно.
        if (Retention.registerSealKill(progress, now)) {
            player.sendSystemMessage(Component.literal("GameMaster: печать снята.")
                .withStyle(ChatFormatting.GREEN));
            telemetry.event("seal", player, "cleared", 1, "");
        }

        // Общая цель недели: считаем убийства всех игроков сервера.
        if (config.social_enabled && config.social_goal_enabled) {
            ensureWeek(now);
            data.week_kills++;
            if (config.social_goal_kills > 0 && data.week_kills == config.social_goal_kills) {
                data.week_goal_epoch = now;
                broadcast("★ Общая цель недели выполнена: " + data.week_kills
                    + " убийств! Каждому, кто зайдёт, — награда.", ChatFormatting.GOLD);
            }
        }

        // Цель сессии: награда выдаётся в момент выполнения.
        int goalReward = Retention.registerKill(progress, now, config);
        if (goalReward > 0) {
            grantFavor(player, progress, goalReward);
            player.sendSystemMessage(Component.literal("GameMaster: цель сессии выполнена! +"
                + goalReward + " Благосклонности").withStyle(ChatFormatting.GREEN));
            telemetry.retained(player, "session_goal", "", goalReward);
        }
        dirty = true;
    }

    /** Учитывает выпадение униката: сбрасывает счётчик pity. */
    public void onUniqueDropped(ServerPlayer player) {
        if (!loaded || player == null) {
            return;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress != null) {
            progress.kills_since_unique = 0;
            dirty = true;
            recordFirst("unique", progress.name, "первый уникат сервера");
        }
    }

    /** Шанс принудительного униката для этого игрока (0…0.9). */
    public double uniquePityChance(ServerPlayer player) {
        if (!loaded || player == null || !config.retention_enabled) {
            return 0;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress == null) {
            return 0;
        }
        return Retention.pityChance(progress.kills_since_unique, config);
    }

    /** Сколько зарядов гарантированной редкости есть у игрока (без траты). */
    public int guaranteedRarityCharges(ServerPlayer player) {
        if (!loaded || player == null || !config.retention_enabled) {
            return 0;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress == null) {
            return 0;
        }
        return progress.guaranteed_epic > 0 ? 2 : (progress.guaranteed_rare > 0 ? 1 : 0);
    }

    /**
     * Забирает заряды гарантированного дропа: 2 — эпик, 1 — редкий, 0 — нет.
     * Заряд тратится только когда реально повышает редкость.
     */
    public int consumeGuaranteedRarity(ServerPlayer player) {
        if (!loaded || player == null || !config.retention_enabled) {
            return 0;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress == null) {
            return 0;
        }
        if (progress.guaranteed_epic > 0) {
            progress.guaranteed_epic--;
            dirty = true;
            return 2;
        }
        if (progress.guaranteed_rare > 0) {
            progress.guaranteed_rare--;
            dirty = true;
            return 1;
        }
        return 0;
    }

    /**
     * true, если смерть не должна отнимать опыт.
     *
     * <p>Два случая: новичок (до {@code retention_no_exp_loss_level}) и мягкая смерть —
     * тогда вместо опыта вешается «печать» (см. {@link Retention#applySeal}).
     * Метод вызывается из {@code EntityData.onDeath} раньше, чем {@code onDeath}
     * GameMaster, поэтому печать ставится именно здесь.
     */
    public boolean skipsExpLossOnDeath(ServerPlayer player) {
        if (!loaded || player == null || !config.retention_enabled) {
            return false;
        }
        // Берём фактический уровень из юнита: прогресс GameMaster обновляется
        // раз в несколько секунд и может отставать на момент смерти.
        int level = 0;
        try {
            level = Load.Unit(player)
                .getLevel();
        } catch (Exception ignored) {
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (level <= 0) {
            level = progress != null ? progress.level : 1;
        }
        if (level < config.retention_no_exp_loss_level) {
            return true;
        }
        if (config.retention_soft_death && progress != null) {
            if (Retention.applySeal(progress, now(), config)) {
                try {
                    player.sendSystemMessage(Component.literal("GameMaster: " + Retention.sealText(config))
                        .withStyle(ChatFormatting.GRAY));
                } catch (Exception ignored) {
                }
                telemetry.event("seal", player, "death", 1, "level=" + level);
                dirty = true;
            }
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Pity для рун и самоцветов, кодекс
    // ------------------------------------------------------------------

    /** Шанс принудительной руны за дроп (0…0.9). */
    public double runePityChance(ServerPlayer player) {
        if (!loaded || player == null || !config.retention_enabled) {
            return 0;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        return progress == null ? 0 : Retention.runePityChance(progress.kills_since_rune, config);
    }

    /** Шанс принудительного самоцвета за дроп. */
    public double gemPityChance(ServerPlayer player) {
        if (!loaded || player == null || !config.retention_enabled) {
            return 0;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        return progress == null ? 0 : Retention.gemPityChance(progress.kills_since_gem, config);
    }

    /** Сбрасывает счётчик рун (руна выпала). */
    public void onRuneDropped(ServerPlayer player) {
        if (!loaded || player == null) {
            return;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress != null) {
            progress.kills_since_rune = 0;
            dirty = true;
        }
    }

    /** Сбрасывает счётчик самоцветов. */
    public void onGemDropped(ServerPlayer player) {
        if (!loaded || player == null) {
            return;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress != null) {
            progress.kills_since_gem = 0;
            dirty = true;
        }
    }

    /**
     * Учитывает собранный уникат в кодексе и выдаёт награду за новую ступень.
     */
    public void recordCodex(ServerPlayer player, String itemId) {
        if (!loaded || player == null || !config.codex_enabled || itemId == null || itemId.isEmpty()) {
            return;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress == null) {
            return;
        }
        Retention.Reward reward = Retention.registerCodexEntry(progress, itemId, config);
        if (reward == null) {
            return;
        }
        grantFavor(player, progress, reward.favor);
        player.sendSystemMessage(Component.literal("GameMaster: " + reward.text)
            .withStyle(ChatFormatting.LIGHT_PURPLE));
        telemetry.retained(player, "codex", itemId, reward.favor);
        broadcast("★ " + player.getGameProfile()
            .name() + " собрал " + Retention.codexSize(progress) + " уникатов в кодексе!", ChatFormatting.GOLD);
        dirty = true;
    }

    /** Сколько уникатов в кодексе у игрока. */
    public int codexSize(String uuid) {
        PlayerProgress progress = uuid == null ? null : data.get(uuid);
        return Retention.codexSize(progress);
    }

    /** Сколько уникатов собрано всеми игроками (разные предметы). */
    public int codexUniqueTotal() {
        java.util.Set<String> all = new java.util.HashSet<>();
        for (PlayerProgress p : data.players.values()) {
            if (p != null && p.codex != null) {
                all.addAll(p.codex);
            }
        }
        return all.size();
    }

    // ------------------------------------------------------------------
    // Маленький сервер: социальный слой (см. Social)
    // ------------------------------------------------------------------

    /**
     * Сообщает входящему игроку, что происходит на сервере: кто онлайн, когда
     * обычно собираются, и как продвигается общая цель недели.
     */
    private void announceSocialState(ServerPlayer player, PlayerProgress progress, long now) {
        if (!config.enabled || !config.social_enabled) {
            return;
        }
        try {
            // новая неделя должна начинаться с чистого счётчика даже без тика
            ensureWeek(now);
            List<ServerPlayer> online = onlinePlayers();
            List<String> names = new ArrayList<>();
            for (ServerPlayer other : online) {
                if (other != player) {
                    names.add(other.getGameProfile()
                        .name());
                }
            }
            if (names.isEmpty()) {
                player.sendSystemMessage(Component.literal(
                        "GameMaster: сейчас ты на сервере один — мобы и лут настроены под одиночную игру.")
                    .withStyle(ChatFormatting.GRAY));
            } else {
                player.sendSystemMessage(Component.literal("GameMaster: сейчас в игре " + (names.size() + 1)
                        + " — кооп-бонус ×" + fmt(Social.coopExp(names.size() + 1, config))
                        + " к опыту: " + String.join(", ", names))
                    .withStyle(ChatFormatting.AQUA));
            }

            if (config.social_prime_hint && Social.hasActivity(data.hour_activity)) {
                int start = Social.bestWindowStart(data.hour_activity, config.social_prime_window_hours);
                if (start >= 0) {
                    player.sendSystemMessage(Component.literal("GameMaster: обычно все собираются "
                            + Social.formatWindow(start, config.social_prime_window_hours))
                        .withStyle(ChatFormatting.GRAY));
                }
            }

            if (config.social_goal_enabled && config.social_goal_kills > 0) {
                boolean reached = data.week_goal_epoch > 0 || data.week_kills >= config.social_goal_kills;
                String uuid = player.getUUID()
                    .toString();
                boolean claimed = data.week_claimed != null && data.week_claimed.contains(uuid);

                if (reached && !claimed) {
                    if (data.week_claimed == null) {
                        data.week_claimed = new ArrayList<>();
                    }
                    data.week_claimed.add(uuid);
                    grantFavor(player, progress, (int) Math.round(config.social_goal_favor));
                    progress.guaranteed_rare += Math.max(0, config.social_goal_rare);
                    dirty = true;
                    player.sendSystemMessage(Component.literal("GameMaster: общая цель недели выполнена! +"
                            + Math.round(config.social_goal_favor) + " Благосклонности"
                            + (config.social_goal_rare > 0 ? " и гарантированная редкая вещь" : ""))
                        .withStyle(ChatFormatting.GOLD));
                    telemetry.retained(player, "weekly_goal", "", config.social_goal_favor);
                } else if (!reached) {
                    player.sendSystemMessage(Component.literal("GameMaster: цель недели — "
                            + Social.goalText(data.week_kills, config.social_goal_kills))
                        .withStyle(ChatFormatting.GRAY));
                }
            }
        } catch (Exception e) {
            // социальные сообщения не должны ломать вход в игру
        }
    }

    /**
     * Напоминание о невыполненной цели недели — только тем, кто онлайн и только
     * в прайм-тайм (когда люди и так собираются). Так подсказка не выглядит спамом
     * и не будит сервер ночью: рассылка живёт внутри тика.
     */
    private void maybeHintWeeklyGoal(long now) {
        if (!config.enabled || !config.social_enabled || !config.social_goal_enabled
            || !config.social_goal_hint || config.social_goal_kills <= 0) {
            return;
        }
        if (onlineCache <= 0 || weeklyGoalReached()) {
            return;
        }
        if (now - data.last_goal_hint < Math.max(5, config.social_goal_hint_minutes) * 60L) {
            return;
        }
        // Если данных достаточно — напоминаем только в окно компании.
        if (Social.hasActivity(data.hour_activity)) {
            int start = Social.bestWindowStart(data.hour_activity, config.social_prime_window_hours);
            if (start >= 0) {
                int window = Math.max(1, Math.min(24, config.social_prime_window_hours));
                int hour = Social.localHour(now);
                int offset = Math.floorMod(hour - start, 24);
                if (offset >= window) {
                    return;
                }
            }
        }
        data.last_goal_hint = now;
        dirty = true;
        int left = Math.max(0, config.social_goal_kills - data.week_kills);
        broadcast("☆ До общей цели недели осталось " + left + " убийств. "
            + "Вместе это 20–30 минут на человека.", ChatFormatting.AQUA);
        telemetry.event("goal_hint", null, "weekly", left, "");
    }

    /** Сбрасывает недельный счётчик, когда началась новая неделя. */
    private void ensureWeek(long now) {
        long week = Social.weekIndex(now);
        if (data.week_index == week) {
            return;
        }
        data.week_index = week;
        data.week_kills = 0;
        data.week_goal_epoch = 0;
        data.week_claimed = new ArrayList<>();
        dirty = true;
    }

    /** Сообщение всем, кто сейчас онлайн (например, о «первом» событии сервера). */
    public void broadcast(String text, ChatFormatting color) {
        if (server == null) {
            return;
        }
        Component message = Component.literal(text)
            .withStyle(color);
        for (ServerPlayer online : onlinePlayers()) {
            try {
                online.sendSystemMessage(message);
            } catch (Exception ignored) {
            }
        }
    }

    /** Запоминает «первое» событие сервера и объявляет его (один раз на сезон). */
    private void recordFirst(String key, String name, String text) {
        if (!config.social_enabled || key == null || name == null || name.isEmpty()) {
            return;
        }
        if (data.firsts == null) {
            data.firsts = new HashMap<>();
        }
        if (data.firsts.containsKey(key)) {
            return;
        }
        data.firsts.put(key, name);
        dirty = true;
        if (config.social_announce_firsts) {
            broadcast("★ " + name + " " + text + "!", ChatFormatting.GOLD);
        }
    }

    /**
     * Множитель HP моба по числу игроков рядом с ним. Считается в момент спавна
     * или пересчёта статов: группа из четырёх не убивает босса за один удар,
     * а одиночка не получает искусственно толстых мобов.
     */
    public double mobGroupHealthMultiplier(net.minecraft.world.entity.Entity entity) {
        if (entity == null || !loaded || !config.enabled || !config.social_enabled
            || !config.social_group_content || server == null) {
            return 1;
        }
        try {
            double radius = Math.max(1, config.social_group_radius);
            double radiusSq = radius * radius;
            int nearby = 0;
            for (ServerPlayer player : onlinePlayers()) {
                if (player.level() != entity.level()) {
                    continue;
                }
                if (player.distanceToSqr(entity) <= radiusSq) {
                    nearby++;
                }
            }
            return Social.groupHealthMultiplier(nearby, config);
        } catch (Exception e) {
            return 1;
        }
    }

    /** Сколько игроков онлайн на последнем тике. */
    public int onlineCount() {
        return onlineCache;
    }

    /** Кооп-множитель опыта (для панели и команд). */
    public double coopExpMultiplier() {
        return config.enabled && config.social_enabled ? Social.coopExp(coopCache, config) : 1;
    }

    /** Кооп-множитель лута. */
    public double coopLootMultiplier() {
        return config.enabled && config.social_enabled ? Social.coopLoot(coopCache, config) : 1;
    }

    /** Кооп-множитель спавна. */
    public double coopSpawnMultiplier() {
        return config.enabled && config.social_enabled ? Social.coopSpawn(coopCache, config) : 1;
    }

    /** Убийств всей командой за текущую неделю. */
    public int weeklyKills() {
        return data.week_kills;
    }

    /** Выполнена ли общая цель недели. */
    public boolean weeklyGoalReached() {
        return data.week_goal_epoch > 0 || (config.social_goal_kills > 0 && data.week_kills >= config.social_goal_kills);
    }

    /** Открыта новая сессия (вход в игру): обновляем среднюю длину сессии. */
    private void accumulateSession(PlayerProgress progress) {
        if (progress.session_seconds > 60) {
            progress.sessions++;
            progress.avg_session_seconds = ((progress.avg_session_seconds * (progress.sessions - 1))
                + progress.session_seconds) / progress.sessions;
        }
        progress.session_seconds = 0;
    }

    public void onLogin(ServerPlayer player) {
        if (!loaded) {
            return;
        }
        PlayerProgress progress = progressFor(player);
        long now = now();

        // Если прошлая сессия не закрылась (сервер упал или был выключен жёстко),
        // засчитываем её сейчас — иначе она потеряется навсегда.
        if (progress.session_seconds > 0) {
            accumulateSession(progress);
        }

        // Возвращение после перерыва: короткое окно бонуса и гарантированный дроп.
        // Считаем ДО перезаписи last_seen, иначе перерыв «исчезает».
        boolean returning = Retention.shouldGrantReturn(progress, now, config);
        if (returning) {
            progress.return_until = now + (long) Math.max(0, config.retention_return_hours * 3600.0);
            progress.guaranteed_rare += Math.max(0, config.retention_return_rare);
        }

        if (config.auto_balance && config.auto_welcome_hours > 0 && progress.last_seen > 0
            && now - progress.last_seen >= config.auto_welcome_hours * 3600.0) {
            progress.welcome_until = now + 1800;
        }
        progress.online = true;
        progress.last_seen = now;
        checkLevel(player, progress);

        // Удержание: серия входов, ежедневная награда и гарантированные дропы.
        if (config.retention_enabled) {
            if (progress.first_seen <= 0) {
                progress.first_seen = now;
            }
            Retention.Reward daily = Retention.onSession(progress, now, config);
            if (daily.favor > 0 || daily.text != null && !daily.text.isEmpty()) {
                if (daily.favor > 0) {
                    grantFavor(player, progress, daily.favor);
                }
                player.sendSystemMessage(Component.literal("GameMaster: " + daily.text)
                    .withStyle(ChatFormatting.AQUA));
                telemetry.retained(player, "streak", "day=" + progress.streak_days, daily.favor);
            }

            // Короткая цель на сессию: одна конкретная задача вместо «играй как хочешь».
            Retention.SessionGoal goal = Retention.ensureSessionGoal(progress, progress.level, now, config);
            if (goal != null && !goal.done) {
                player.sendSystemMessage(Component.literal("GameMaster: " + goal.text)
                    .withStyle(ChatFormatting.YELLOW));
            }
        }

        if (returning) {
            player.sendSystemMessage(Component.literal("GameMaster: " + Retention.returnText(config))
                .withStyle(ChatFormatting.GREEN));
        }

        announceSocialState(player, progress, now);

        telemetry.login(player);

        progress.multiplier = computeMultiplier(progress);
        if (progress.history.isEmpty()) {
            progress.addHistory(progress.multiplier, progress.last_seen);
        }
        dirty = true;

        if (config.enabled && config.announce_on_login && (config.pacing || config.catch_up || config.auto_balance)) {
            MultiplierBreakdown breakdown = breakdown(progress);
            String text = "GameMaster: множитель опыта x" + fmt(breakdown.total)
                + " (темп x" + fmt(breakdown.pacing) + ", догоняющий x" + fmt(breakdown.catchUp)
                + ", автобаланс x" + fmt(breakdown.auto)
                + (breakdown.coop > 1.001 ? ", кооп x" + fmt(breakdown.coop) : "")
                + (breakdown.returnBonus > 1.001 ? ", возвращение x" + fmt(breakdown.returnBonus) : "")
                + ")";
            player.sendSystemMessage(Component.literal(text)
                .withStyle(ChatFormatting.GOLD));
        }
    }

    public void onLogout(ServerPlayer player) {
        if (!loaded) {
            return;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress == null) {
            return;
        }
        telemetry.logout(player, progress.session_seconds);
        progress.online = false;
        progress.last_seen = now();
        checkLevel(player, progress);
        accumulateSession(progress);
        progress.addHistory(progress.multiplier, progress.last_seen);
        progress.last_snapshot_playtime = progress.playtime_seconds;
        progress.last_snapshot_epoch = progress.last_seen;
        dirty = true;
        recomputeStats(now());
        save();
    }

    // ------------------------------------------------------------------
    // Расчёт множителей
    // ------------------------------------------------------------------

    public void recomputeStats(long now) {
        lastStatsTick = now;
        maxLevelCache = ExpTuner.maxLevel();
        int maxLevel = maxLevelCache;

        double minSeconds = Math.max(0, config.catch_up_min_playtime_minutes) * 60.0;
        long windowSeconds = Math.max(1, config.catch_up_activity_window_days) * 86400L;

        double sum = 0;
        double max = 0;
        int peers = 0;

        for (PlayerProgress progress : data.players.values()) {
            if (progress.playtime_seconds < minSeconds) {
                continue;
            }
            if (progress.last_seen < now - windowSeconds) {
                continue;
            }
            sum += progress.level;
            max = Math.max(max, progress.level);
            peers++;
        }

        cachedPeers = peers;
        cachedHasPeers = peers >= 2;

        if (peers == 0) {
            cachedReferenceLevel = 1;
        } else {
            double average = sum / peers;
            double blend = ExpTuner.clamp(config.catch_up_reference_blend, 0, 1);
            cachedReferenceLevel = average + blend * (max - average);
        }

        double measured = measureBaselineHours(maxLevel);
        measuredBaselineHours = measured;
        measuredObservedHours = data.observed_seconds / 3600.0;

        double baseline = Math.max(0.01, config.assumed_baseline_hours);
        if (measured > 0) {
            double confidence = ExpTuner.clamp(measuredObservedHours / 6.0, 0, 1);
            baseline = baseline * (1 - confidence) + measured * confidence;
        }
        cachedBaselineHours = Math.max(0.01, baseline);

        if (config.pacing_auto_calibrate) {
            cachedK = ExpTuner.solveK(config, cachedBaselineHours, maxLevel);
        } else {
            cachedK = -Math.max(0, Math.min(0.99, config.pacing_max_penalty));
        }

        recomputeAuto(now);

        for (ServerPlayer player : onlinePlayers()) {
            PlayerProgress progress = data.get(player.getUUID()
                .toString());
            if (progress != null) {
                progress.multiplier = computeMultiplier(progress);
            }
        }
    }

    /**
     * Пересчитывает решения автобаланса: для каждого онлайн-игрока — множитель опыта,
     * для сервера — усреднённые множители HP/урона мобов и лута.
     */
    private void recomputeAuto(long now) {
        autoTargetHours = AutoBalance.totalTargetHours(maxLevelCache, config);

        if (!config.auto_balance) {
            autoMobHealth = 1.0;
            autoMobDamage = 1.0;
            autoLoot = 1.0;
            autoGlobalUpdated = now;
            return;
        }

        // Медиана, а не среднее: на сервере с 4–6 игроками одна тяжёлая сессия
        // (новичок, который умирает каждые две минуты) не должна ослаблять мобов
        // всем остальным. При двух игроках медиана совпадает со средним.
        List<Double> healthTargets = new ArrayList<>();
        List<Double> damageTargets = new ArrayList<>();
        List<Double> lootTargets = new ArrayList<>();
        int count = 0;

        for (ServerPlayer player : onlinePlayers()) {
            PlayerProgress progress = data.get(player.getUUID()
                .toString());
            if (progress == null) {
                continue;
            }
            AutoBalance.Directive directive = AutoBalance.evaluate(progress, config, maxLevelCache, now);

            progress.auto_exp_multiplier = directive.exp;
            progress.auto_mob_health = directive.mob_health;
            progress.auto_mob_damage = directive.mob_damage;
            progress.auto_loot = directive.loot;

            // спавн меняем так же плавно, как и серверных мобов, чтобы state-переходы
            // не давали «мигание» популяции мобов
            double playerMinutes = progress.auto_updated_epoch > 0
                ? Math.min(60, Math.max(0.1, (now - progress.auto_updated_epoch) / 60.0))
                : 60;
            progress.auto_spawn = AutoBalance.rateLimit(progress.auto_spawn, directive.spawn,
                config.auto_global_step_per_minute, playerMinutes);
            progress.auto_spawn = AutoBalance.clamp(progress.auto_spawn, config.auto_spawn_min,
                config.auto_spawn_max);
            progress.auto_flow = directive.flow;
            progress.auto_state = directive.state;
            progress.auto_updated_epoch = now;

            healthTargets.add(directive.mob_health);
            damageTargets.add(directive.mob_damage);
            lootTargets.add(directive.loot);
            count++;
        }

        if (count == 0) {
            return;
        }

        double minutes = autoGlobalUpdated > 0
            ? Math.min(60, Math.max(0.1, (now - autoGlobalUpdated) / 60.0))
            : 60;
        double step = config.auto_global_step_per_minute;

        if (config.auto_touch_mobs) {
            autoMobHealth = AutoBalance.rateLimit(autoMobHealth, median(healthTargets), step, minutes);
            autoMobDamage = AutoBalance.rateLimit(autoMobDamage, median(damageTargets), step, minutes);
            autoMobHealth = AutoBalance.clamp(autoMobHealth, config.auto_mob_health_min, config.auto_mob_health_max);
            autoMobDamage = AutoBalance.clamp(autoMobDamage, config.auto_mob_damage_min, config.auto_mob_damage_max);
        } else {
            autoMobHealth = 1.0;
            autoMobDamage = 1.0;
        }

        if (config.auto_touch_loot) {
            autoLoot = AutoBalance.rateLimit(autoLoot, median(lootTargets), step, minutes);
            autoLoot = AutoBalance.clamp(autoLoot, 1.0, config.auto_loot_max);
        } else {
            autoLoot = 1.0;
        }

        autoGlobalUpdated = now;
    }

    /** Медиана значений (устойчива к одиночным выбросам). */
    private static double median(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 1.0;
        }
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int size = sorted.size();
        return size % 2 == 1
            ? sorted.get(size / 2)
            : (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }

    /** Оценка времени до максимума по замерам (в часах), либо -1, если замеров нет. */
    private double measureBaselineHours(int maxLevel) {
        if (data.level_times.isEmpty()) {
            return -1;
        }
        double[] weights = ExpTuner.expWeights(maxLevel);
        double seconds = 0;
        double weightedExp = 0;

        for (Map.Entry<String, LevelTimeStat> entry : data.level_times.entrySet()) {
            int level;
            try {
                level = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            if (level < 1 || level >= maxLevel) {
                continue;
            }
            LevelTimeStat stat = entry.getValue();
            if (stat == null || stat.seconds <= 0 || stat.completions <= 0) {
                continue;
            }
            seconds += stat.seconds;
            weightedExp += weights[level - 1] * stat.completions;
        }

        if (seconds <= 0 || weightedExp <= 0) {
            return -1;
        }

        double secondsPerExp = seconds / weightedExp;
        double totalExp = 0;
        for (double weight : weights) {
            totalExp += weight;
        }
        return secondsPerExp * totalExp / 3600.0;
    }

    public double computeMultiplier(PlayerProgress progress) {
        if (!config.enabled) {
            return 1;
        }
        double multiplier = 1.0;

        // Автобаланс заменяет фиксированную цель «N часов»: иначе два регулятора
        // тянут опыт в разные стороны и гасят друг друга.
        if (config.pacing && !config.auto_balance) {
            multiplier *= ExpTuner.pacingMultiplier(progress.level, cachedK, config.pacing_start_level,
                maxLevelCache, config.pacing_curve);
        }
        if (config.catch_up) {
            multiplier *= ExpTuner.catchUpMultiplier(progress.level, cachedReferenceLevel, cachedHasPeers, config);
        }
        if (config.auto_balance && progress.auto_exp_multiplier > 0) {
            multiplier *= progress.auto_exp_multiplier;
        }
        // Маленький сервер: играть вместе выгоднее, чем по очереди.
        if (config.social_enabled) {
            multiplier *= Social.coopExp(coopCache, config);
        }
        // Возвращение после перерыва: короткое окно усиленного опыта.
        multiplier *= Retention.returnExpBonus(progress, now(), config);
        // Мягкая смерть: пока висит печать, опыт немного медленнее.
        multiplier *= Retention.sealExpMultiplier(progress, now(), config);

        return ExpTuner.clamp(multiplier, config.exp_multiplier_min, config.exp_multiplier_max);
    }

    /** Разбивка множителя для сообщений и панели. */
    public MultiplierBreakdown breakdown(PlayerProgress progress) {
        MultiplierBreakdown breakdown = new MultiplierBreakdown();
        if (!config.enabled) {
            return breakdown;
        }
        if (config.pacing && !config.auto_balance) {
            breakdown.pacing = ExpTuner.pacingMultiplier(progress.level, cachedK, config.pacing_start_level,
                maxLevelCache, config.pacing_curve);
        }
        if (config.catch_up) {
            breakdown.catchUp = ExpTuner.catchUpMultiplier(progress.level, cachedReferenceLevel, cachedHasPeers,
                config);
        }
        if (config.auto_balance && progress.auto_exp_multiplier > 0) {
            breakdown.auto = progress.auto_exp_multiplier;
        }
        if (config.social_enabled) {
            breakdown.coop = Social.coopExp(coopCache, config);
        }
        breakdown.returnBonus = Retention.returnExpBonus(progress, now(), config);
        breakdown.seal = Retention.sealExpMultiplier(progress, now(), config);
        breakdown.total = ExpTuner.clamp(
            breakdown.pacing * breakdown.catchUp * breakdown.auto * breakdown.coop * breakdown.returnBonus
                * breakdown.seal,
            config.exp_multiplier_min, config.exp_multiplier_max);
        return breakdown;
    }

    /**
     * Применяет множители к выдаваемому опыту.
     * Возвращает изменённое количество опыта.
     */
    public int tuneExp(ServerPlayer player, int rawExp) {
        if (!loaded || player == null) {
            return rawExp;
        }
        try {
            PlayerProgress progress = progressFor(player);
            checkLevel(player, progress);

            double multiplier = computeMultiplier(progress);
            progress.multiplier = multiplier;

            int result = rawExp;
            if (rawExp > 0) {
                long scaled = Math.round(rawExp * multiplier);
                result = (int) Math.max(1, Math.min(Integer.MAX_VALUE, scaled));
            }

            progress.raw_exp_gained += Math.max(0, rawExp);
            progress.exp_gained += Math.max(0, result);
            progress.last_seen = now();
            dirty = true;
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return rawExp;
        }
    }

    /** Множитель игрока на текущий момент (для отображения). */
    public double multiplierFor(ServerPlayer player) {
        if (!loaded) {
            return 1;
        }
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress == null) {
            return 1;
        }
        return computeMultiplier(progress);
    }

    /** Полный множитель вероятности спавна для игрока с учётом общего и динамического режимов. */
    public double spawnRateMultiplier(ServerPlayer player) {
        if (player == null || !loaded || !config.enabled || !config.spawn_rate_enabled) {
            return 1;
        }
        if (!config.spawn_rate_dynamic) {
            return spawnRateMultiplier(player.getUUID().toString(), 0);
        }
        int favor = 100;
        try {
            favor = Load.playerRPGData(player).favor.getFavor();
        } catch (Exception ignored) {
            PlayerProgress progress = data.get(player.getUUID().toString());
            if (progress != null) {
                favor = progress.favor;
            }
        }
        return spawnRateMultiplier(player.getUUID().toString(), favor);
    }

    /** Версия для веб-панели/команд, использующая последнее сохранённое значение Благосклонности. */
    public double spawnRateMultiplier(String uuid, int favor) {
        if (!loaded || !config.enabled || !config.spawn_rate_enabled) {
            return 1;
        }
        double rate = spawnRateManualMultiplier(uuid) * spawnRateDynamicMultiplier(favor);

        // Автобаланс может вернуть часть урезанного спавна (или слегка прижать его),
        // но итог всё равно остаётся вероятностью 0…1.
        if (config.auto_balance) {
            rate *= autoSpawnMultiplier(uuid);
        }
        // Компания онлайн — больше мобов: мир должен быть живым, а не пустым.
        if (config.social_enabled) {
            rate *= Social.coopSpawn(coopCache, config);
        }
        return ExpTuner.clamp(rate, 0, 1);
    }

    /** Множитель спавна от автобаланса для игрока (1 — без изменений). */
    public double autoSpawnMultiplier(String uuid) {
        if (uuid == null || !config.auto_balance) {
            return 1.0;
        }
        PlayerProgress progress = data.get(uuid);
        if (progress == null || progress.auto_spawn <= 0) {
            return 1.0;
        }
        return AutoBalance.clamp(progress.auto_spawn, config.auto_spawn_min, config.auto_spawn_max);
    }

    public double spawnRateManualMultiplier(String uuid) {
        Double override = uuid == null ? null : config.spawn_rate_player_overrides.get(uuid);
        return override == null ? config.spawn_rate_manual_multiplier : override;
    }

    public double spawnRateDynamicMultiplier(int favor) {
        if (!config.spawn_rate_dynamic) {
            return 1;
        }
        return ExpTuner.favorSpawnRate(favor, config.spawn_rate_favor_reference,
            config.spawn_rate_dynamic_min_multiplier);
    }

    public Double spawnRatePlayerOverride(String uuid) {
        return uuid == null ? null : config.spawn_rate_player_overrides.get(uuid);
    }

    public void setSpawnRatePlayerOverride(String uuid, double multiplier) {
        if (uuid == null) {
            return;
        }
        try {
            uuid = UUID.fromString(uuid).toString();
        } catch (IllegalArgumentException e) {
            return;
        }
        double value = GameMasterConfig.clamp(multiplier, 0, 1);
        Double previous = config.spawn_rate_player_overrides.put(uuid, value);
        if (previous == null || Math.abs(previous - value) > 0.000001) {
            onConfigEdited();
        }
    }

    public boolean clearSpawnRatePlayerOverride(String uuid) {
        if (uuid == null) {
            return false;
        }
        boolean removed = config.spawn_rate_player_overrides.remove(uuid) != null;
        if (removed) {
            onConfigEdited();
        }
        return removed;
    }

    private List<ServerPlayer> onlinePlayers() {
        if (server == null) {
            return new ArrayList<>();
        }
        return server.getPlayerList()
            .getPlayers();
    }

    // ------------------------------------------------------------------
    // Веб-панель
    // ------------------------------------------------------------------

    private void restartWeb() {
        if (!config.web_enabled || server == null) {
            stopWeb();
            return;
        }
        boolean same = webServer != null
            && java.util.Objects.equals(webStartedHost, config.web_host)
            && webStartedPort == config.web_port
            && java.util.Objects.equals(webStartedToken, config.web_token);
        if (same) {
            return;
        }
        stopWeb();
        try {
            webServer = new GameMasterWebServer(this, config.web_host, config.web_port, config.web_token);
            webServer.start();
            webStartedHost = config.web_host;
            webStartedPort = config.web_port;
            webStartedToken = config.web_token;
        } catch (Exception e) {
            System.out.println("[GameMaster] Не удалось поднять веб-панель на " + config.web_host + ":"
                + config.web_port + " — " + e);
            webServer = null;
        }
    }

    private void stopWeb() {
        if (webServer != null) {
            try {
                webServer.stop();
            } catch (Exception ignored) {
            }
            webServer = null;
        }
        webStartedHost = null;
        webStartedPort = -1;
        webStartedToken = null;
    }

    /** Применяет JSON из веб-панели: настройки и ручной override конкретного игрока. */
    public List<String> applyConfigJson(JsonObject json) {
        List<String> changed = config.applyJson(json);
        if (json != null && json.has("spawn_rate_player_uuid")) {
            try {
                JsonElement uuidElement = json.get("spawn_rate_player_uuid");
                String uuid = UUID.fromString(uuidElement.getAsString()).toString();
                boolean clear = json.has("spawn_rate_player_clear")
                    && json.get("spawn_rate_player_clear").getAsBoolean();
                if (clear) {
                    if (config.spawn_rate_player_overrides.remove(uuid) != null) {
                        changed.add("spawn_rate_player_overrides");
                    }
                } else if (json.has("spawn_rate_player_multiplier")) {
                    double multiplier = json.get("spawn_rate_player_multiplier").getAsDouble();
                    if (Double.isFinite(multiplier)) {
                        multiplier = GameMasterConfig.clamp(multiplier, 0, 1);
                        Double previous = config.spawn_rate_player_overrides.put(uuid, multiplier);
                        if (previous == null || Math.abs(previous - multiplier) > 0.000001) {
                            changed.add("spawn_rate_player_overrides");
                        }
                    }
                }
            } catch (Exception ignored) {
                // некорректный UUID или значение — игнорируем только override
            }
        }
        if (!changed.isEmpty()) {
            onConfigEdited();
        }
        return changed;
    }

    public MinecraftServer server() {
        return server;
    }

    // ------------------------------------------------------------------
    // JSON для панели
    // ------------------------------------------------------------------

    public String buildStateJson() {
        JsonObject root = new JsonObject();
        root.addProperty("server_time", now());
        root.addProperty("max_level", maxLevelCache);

        JsonObject cfg = GameMasterConfig.GSON.toJsonTree(config)
            .getAsJsonObject();
        cfg.remove("web_token");
        cfg.addProperty("has_web_token", config.web_token != null && !config.web_token.isEmpty());
        root.add("config", cfg);

        JsonObject summary = new JsonObject();
        summary.addProperty("baseline_hours", round2(cachedBaselineHours));
        summary.addProperty("measured_baseline_hours", measuredBaselineHours > 0 ? round2(measuredBaselineHours) : -1);
        summary.addProperty("observed_hours", round2(measuredObservedHours));
        summary.addProperty("predicted_hours",
            round2(ExpTuner.predictedHours(config, cachedBaselineHours, cachedK, maxLevelCache)));
        summary.addProperty("target_hours", round2(config.target_hours));
        summary.addProperty("k", round2(cachedK));
        summary.addProperty("reference_level", round2(cachedReferenceLevel));
        summary.addProperty("peers", cachedPeers);
        summary.addProperty("tracked", data.players.size());
        summary.addProperty("online", onlinePlayers().size());
        summary.addProperty("spawn_rate_enabled", config.enabled && config.spawn_rate_enabled);
        summary.addProperty("spawn_rate_dynamic", config.spawn_rate_dynamic);
        summary.addProperty("spawn_rate_manual_multiplier", round2(config.spawn_rate_manual_multiplier));
        summary.addProperty("spawn_rate_favor_reference", round2(config.spawn_rate_favor_reference));

        JsonObject auto = new JsonObject();
        auto.addProperty("enabled", config.enabled && config.auto_balance);
        auto.addProperty("target_hours", round2(autoTargetHours()));
        auto.addProperty("target_start_minutes", round2(AutoBalance.targetMinutes(1, maxLevelCache, config)));
        auto.addProperty("target_end_minutes",
            round2(AutoBalance.targetMinutes(maxLevelCache, maxLevelCache, config)));
        auto.addProperty("mob_health", round2(mobHealthMultiplier()));
        auto.addProperty("mob_damage", round2(mobDamageMultiplier()));
        auto.addProperty("loot", round2(lootMultiplier()));
        double spawnSum = 0;
        int spawnCount = 0;
        for (ServerPlayer player : onlinePlayers()) {
            PlayerProgress progress = data.get(player.getUUID().toString());
            if (progress != null) {
                spawnSum += autoSpawnMultiplier(progress.uuid);
                spawnCount++;
            }
        }
        auto.addProperty("spawn", spawnCount > 0 ? round2(spawnSum / spawnCount) : 1.0);
        auto.addProperty("touch_mobs", config.auto_touch_mobs);
        auto.addProperty("touch_loot", config.auto_touch_loot);
        summary.add("auto", auto);

        JsonObject retention = new JsonObject();
        int streakMax = 0;
        int activeSum = 0;
        int tracked = 0;
        double riskSum = 0;
        int pitySum = 0;
        for (PlayerProgress p : data.players.values()) {
            if (p == null) {
                continue;
            }
            streakMax = Math.max(streakMax, p.streak_days);
            activeSum += Retention.activeDaysWithin(p, 7, now());
            riskSum += Retention.churnRisk(p, now());
            pitySum += p.kills_since_unique;
            tracked++;
        }
        retention.addProperty("enabled", config.retention_enabled);
        retention.addProperty("max_streak", streakMax);
        retention.addProperty("avg_active_days_7", tracked > 0 ? round2(activeSum / (double) tracked) : 0);
        retention.addProperty("avg_churn_risk", tracked > 0 ? round2(riskSum / tracked) : 0);
        retention.addProperty("avg_kills_since_unique", tracked > 0 ? pitySum / tracked : 0);
        retention.addProperty("pity_start_kills", config.retention_pity_start_kills);
        retention.addProperty("milestone_every", config.retention_milestone_every);
        summary.add("retention", retention);

        // Маленький сервер: кто онлайн, когда собираются, общая цель недели.
        JsonObject social = new JsonObject();
        social.addProperty("enabled", config.enabled && config.social_enabled);
        social.addProperty("online", onlineCache);
        social.addProperty("coop_exp", round2(coopExpMultiplier()));
        social.addProperty("coop_loot", round2(coopLootMultiplier()));
        social.addProperty("coop_spawn", round2(coopSpawnMultiplier()));
        social.addProperty("group_hp", round2(Social.groupHealthMultiplier(coopCache, config)));
        social.addProperty("active", coopCache);
        social.addProperty("group_radius", round2(config.social_group_radius));
        boolean hasActivity = Social.hasActivity(data.hour_activity);
        int window = Math.max(1, Math.min(24, config.social_prime_window_hours));
        int primeStart = hasActivity ? Social.bestWindowStart(data.hour_activity, window) : -1;
        social.addProperty("prime_start", primeStart);
        social.addProperty("prime_end", primeStart < 0 ? -1 : Math.floorMod(primeStart + window, 24));
        social.addProperty("prime_window", Social.formatWindow(primeStart, window));
        JsonArray heat = new JsonArray();
        for (double value : Social.normalized(data.hour_activity)) {
            heat.add(round2(value));
        }
        social.add("heat", heat);
        social.addProperty("goal_enabled", config.social_goal_enabled);
        social.addProperty("goal_kills", data.week_kills);
        social.addProperty("goal_target", config.social_goal_kills);
        social.addProperty("goal_reached", weeklyGoalReached());
        social.addProperty("goal_claimed", data.week_claimed == null ? 0 : data.week_claimed.size());
        JsonObject firsts = new JsonObject();
        if (data.firsts != null) {
            for (Map.Entry<String, String> entry : data.firsts.entrySet()) {
                firsts.addProperty(entry.getKey(), entry.getValue());
            }
        }
        social.add("firsts", firsts);
        social.add("codex_unique", codexUniqueTotal());
        int codexSum = 0;
        int codexPlayers = 0;
        for (PlayerProgress p : data.players.values()) {
            if (p == null) {
                continue;
            }
            codexSum += Retention.codexSize(p);
            if (Retention.codexSize(p) > 0) {
                codexPlayers++;
            }
        }
        social.addProperty("codex_total", codexSum);
        social.addProperty("codex_players", codexPlayers);
        social.addProperty("codex_enabled", config.codex_enabled);
        summary.add("social", social);

        summary.add("telemetry", telemetry.panelJson(config.telemetry_panel_top));

        root.add("summary", summary);

        List<PlayerProgress> sorted = new ArrayList<>(data.players.values());
        sorted.sort(Comparator.comparingDouble((PlayerProgress p) -> p.level)
            .thenComparingDouble(p -> p.playtime_seconds)
            .reversed());

        JsonArray players = new JsonArray();
        for (PlayerProgress progress : sorted) {
            players.add(progressJson(progress));
        }
        root.add("players", players);

        return GSON.toJson(root);
    }

    /** JSON для внешних инструментов: живые топы, лента и профили игроков. */
    public String buildTelemetryJson() {
        JsonObject root = new JsonObject();
        root.addProperty("server_time", now());
        root.add("telemetry", telemetry.panelJson(Math.max(20, config.telemetry_panel_top)));
        JsonArray players = new JsonArray();
        for (PlayerProgress progress : data.players.values()) {
            if (progress == null) {
                continue;
            }
            JsonObject item = new JsonObject();
            item.addProperty("uuid", progress.uuid);
            item.addProperty("name", progress.name);
            item.addProperty("level", progress.level);
            item.addProperty("online", progress.online);
            item.add("preferences", telemetry.preferencesJson(progress.uuid));
            players.add(item);
        }
        root.add("players", players);
        return GSON.toJson(root);
    }

    private JsonObject progressJson(PlayerProgress progress) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", progress.uuid);
        json.addProperty("name", progress.name);
        json.addProperty("level", progress.level);
        json.addProperty("playtime_hours", round2(progress.playtimeHours()));
        json.addProperty("baseline_hours", round2(progress.baselineHours()));
        json.addProperty("exp_gained", Math.round(progress.exp_gained));
        json.addProperty("raw_exp_gained", Math.round(progress.raw_exp_gained));
        json.addProperty("online", progress.online);
        json.addProperty("last_seen", progress.last_seen);
        json.addProperty("last_seen_minutes", progress.last_seen > 0 ? (now() - progress.last_seen) / 60 : -1);
        json.addProperty("favor", progress.favor);
        json.addProperty("spawn_rate_multiplier", round2(spawnRateMultiplier(progress.uuid, progress.favor)));
        json.addProperty("spawn_rate_dynamic_multiplier", round2(spawnRateDynamicMultiplier(progress.favor)));
        json.addProperty("spawn_rate_manual_multiplier", round2(spawnRateManualMultiplier(progress.uuid)));
        Double spawnOverride = spawnRatePlayerOverride(progress.uuid);
        json.addProperty("spawn_rate_override", spawnOverride == null ? -1 : round2(spawnOverride));

        MultiplierBreakdown breakdown = breakdown(progress);
        json.addProperty("multiplier", round2(breakdown.total));
        json.addProperty("pacing_multiplier", round2(breakdown.pacing));
        json.addProperty("catch_up_multiplier", round2(breakdown.catchUp));
        json.addProperty("auto_exp_multiplier", round2(breakdown.auto));
        json.addProperty("auto_state", progress.auto_state);
        json.addProperty("auto_flow", round2(progress.auto_flow));
        json.addProperty("auto_mob_health", round2(progress.auto_mob_health));
        json.addProperty("auto_mob_damage", round2(progress.auto_mob_damage));
        json.addProperty("auto_loot", round2(progress.auto_loot));
        json.addProperty("auto_spawn", round2(autoSpawnMultiplier(progress.uuid)));
        json.addProperty("target_minutes", round2(AutoBalance.targetMinutes(progress.level, maxLevelCache, config)));
        json.addProperty("observed_minutes", round2(progress.ema_minutes_per_level));
        json.addProperty("minutes_at_level", round2(progress.minutesAtLevel()));
        json.addProperty("deaths", progress.deaths);
        json.addProperty("deaths_since_level", progress.deaths_since_level);
        json.addProperty("deaths_per_hour", round2(AutoBalance.deathsPerHour(progress)));
        json.addProperty("levels_gained", progress.levels_gained);
        json.addProperty("levels_at_pace", progress.levels_at_pace);
        json.addProperty("session_minutes", round2(progress.session_seconds / 60.0));
        json.addProperty("sessions", progress.sessions);
        json.addProperty("avg_session_minutes", round2(progress.avg_session_seconds / 60.0));
        json.addProperty("streak_days", progress.streak_days);
        json.addProperty("active_days_7", Retention.activeDaysWithin(progress, 7, now()));
        json.addProperty("active_days_30",
            progress.active_days == null ? 0 : progress.active_days.size());
        json.addProperty("day_of_play", Retention.dayOfPlay(progress, now()));
        json.addProperty("churn_risk", round2(Retention.churnRisk(progress, now())));
        json.addProperty("milestones", progress.milestones_claimed);
        json.addProperty("kills_since_unique", progress.kills_since_unique);
        json.addProperty("guaranteed_rare", progress.guaranteed_rare);
        json.addProperty("guaranteed_epic", progress.guaranteed_epic);
        json.addProperty("coop_multiplier", round2(config.social_enabled
            ? Social.coopExp(coopCache, config) : 1));
        json.addProperty("session_goal_progress", progress.session_goal_progress);
        json.addProperty("session_goal_target", progress.session_goal_target);
        json.addProperty("session_goal_done", progress.session_goal_done);
        json.addProperty("return_bonus", round2(Retention.returnExpBonus(progress, now(), config)));
        json.addProperty("distance", round2(progress.distance_travelled));
        json.addProperty("seal_active", Retention.sealActive(progress, now()));
        json.addProperty("seal_kills_left", progress.seal_kills_left);
        json.addProperty("codex", Retention.codexSize(progress));
        json.addProperty("kills_since_rune", progress.kills_since_rune);
        json.addProperty("kills_since_gem", progress.kills_since_gem);
        JsonObject preferences = telemetry.preferencesJson(progress.uuid);
        if (preferences.size() > 0) {
            json.add("preferences", preferences);
        }

        // для графика хватает ~300 точек, не гоняем весь массив в браузер
        JsonArray history = new JsonArray();
        int size = progress.history.size();
        int step = Math.max(1, size / 300);
        for (int i = 0; i < size; i++) {
            if (i % step != 0 && i != size - 1) {
                continue;
            }
            HistoryPoint point = progress.history.get(i);
            JsonObject h = new JsonObject();
            h.addProperty("playtime_hours", round2(point.playtime_hours));
            h.addProperty("level", point.level);
            h.addProperty("multiplier", round2(point.multiplier));
            h.addProperty("time", point.time);
            history.add(h);
        }
        json.add("history", history);
        return json;
    }

    /**
     * Экспорт телеметрии в CSV — сырьё для офлайн-аналитики и моделей
     * (прогноз оттока, подбор параметров). В игровом тике ничего не обучается.
     *
     * @return путь к файлу
     */
    public Path exportCsv() throws java.io.IOException {
        Path path = baseDir().resolve("gamemaster_export.csv");
        long now = now();
        StringBuilder sb = new StringBuilder();
        sb.append("uuid,name,level,playtime_hours,sessions,avg_session_minutes,streak_days,")
            .append("active_days_7,day_of_play,churn_risk,deaths,deaths_per_hour,")
            .append("minutes_per_level,target_minutes,exp_multiplier,auto_state,")
            .append("kills_since_unique,milestones,guaranteed_rare,guaranteed_epic,last_seen_days,")
            .append("goal_progress,goal_target,return_bonus,coop_multiplier,")
            .append("distance,top_biome,style,codex,seal_count,kills_since_rune\n");
        for (PlayerProgress p : data.players.values()) {
            if (p == null) {
                continue;
            }
            sb.append(csv(p.uuid)).append(',')
                .append(csv(p.name)).append(',')
                .append(p.level).append(',')
                .append(round2(p.playtimeHours())).append(',')
                .append(p.sessions).append(',')
                .append(round2(p.avg_session_seconds / 60.0)).append(',')
                .append(p.streak_days).append(',')
                .append(Retention.activeDaysWithin(p, 7, now)).append(',')
                .append(Retention.dayOfPlay(p, now)).append(',')
                .append(round2(Retention.churnRisk(p, now))).append(',')
                .append(p.deaths).append(',')
                .append(round2(AutoBalance.deathsPerHour(p))).append(',')
                .append(round2(p.ema_minutes_per_level)).append(',')
                .append(round2(AutoBalance.targetMinutes(p.level, maxLevelCache, config))).append(',')
                .append(round2(p.auto_exp_multiplier)).append(',')
                .append(csv(p.auto_state)).append(',')
                .append(p.kills_since_unique).append(',')
                .append(p.milestones_claimed).append(',')
                .append(p.guaranteed_rare).append(',')
                .append(p.guaranteed_epic).append(',')
                .append(p.last_seen > 0 ? round2((now - p.last_seen) / 86400.0) : -1).append(',')
                .append(p.session_goal_progress).append(',')
                .append(p.session_goal_target).append(',')
                .append(round2(Retention.returnExpBonus(p, now, config))).append(',')
                .append(round2(config.social_enabled ? Social.coopExp(coopCache, config) : 1)).append(',')
                .append(round2(p.distance_travelled)).append(',')
                .append(csv(telemetryTop(p.uuid, "biome_time|"))).append(',')
                .append(csv(telemetryStyle(p.uuid))).append(',')
                .append(Retention.codexSize(p)).append(',')
                .append(p.seal_count).append(',')
                .append(p.kills_since_rune)
                .append('\n');
        }
        Files.createDirectories(baseDir());
        Files.write(path, sb.toString()
            .getBytes(StandardCharsets.UTF_8));
        return path;
    }

    /** Верхний ключ профиля игрока (для CSV/отчётов). */
    private String telemetryTop(String uuid, String prefix) {
        try {
            JsonObject preferences = telemetry.preferencesJson(uuid);
            String key = prefix.equals("biome_time|") ? "top_biome" : "";
            if (!key.isEmpty() && preferences.has(key)) {
                return preferences.get(key)
                    .getAsString();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String telemetryStyle(String uuid) {
        try {
            JsonObject preferences = telemetry.preferencesJson(uuid);
            if (preferences.has("style")) {
                return preferences.get("style")
                    .getAsString();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replace('"', ' ')
            .replace(',', ' ')
            .replace('\n', ' ');
        return '"' + clean + '"';
    }

    public static double round2(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    public static String fmt(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        if (Math.abs(value - Math.round(value)) < 0.005) {
            return String.valueOf(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    public String describeMultiplier(ServerPlayer player) {
        PlayerProgress progress = data.get(player.getUUID()
            .toString());
        if (progress == null) {
            return "нет данных";
        }
        MultiplierBreakdown breakdown = breakdown(progress);
        return "x" + fmt(breakdown.total) + " (темп x" + fmt(breakdown.pacing) + ", догоняющий x"
            + fmt(breakdown.catchUp) + ", автобаланс x" + fmt(breakdown.auto) + ", уровень " + progress.level
            + ", время " + fmt(progress.playtimeHours()) + " ч)";
    }

    /** Геттеры для панели/команд. */
    public double baselineHours() {
        return cachedBaselineHours;
    }

    public double measuredBaselineHours() {
        return measuredBaselineHours;
    }

    public double calibrationK() {
        return cachedK;
    }

    public double referenceLevel() {
        return cachedReferenceLevel;
    }

    public int peerCount() {
        return cachedPeers;
    }

    public double predictedHours() {
        return ExpTuner.predictedHours(config, cachedBaselineHours, cachedK, maxLevelCache);
    }

    // ------------------------------------------------------------------
    // Автобаланс: наружу
    // ------------------------------------------------------------------

    /** Глобальный множитель HP мобов (1 — как в базе). */
    public double mobHealthMultiplier() {
        return config.enabled && config.auto_balance && config.auto_touch_mobs ? autoMobHealth : 1.0;
    }

    /** Глобальный множитель урона мобов. */
    public double mobDamageMultiplier() {
        return config.enabled && config.auto_balance && config.auto_touch_mobs ? autoMobDamage : 1.0;
    }

    /** Глобальный множитель шанса дропа (автобаланс + кооп-бонус). */
    public double lootMultiplier() {
        double loot = config.enabled && config.auto_balance && config.auto_touch_loot ? autoLoot : 1.0;
        if (config.enabled && config.social_enabled) {
            loot *= Social.coopLoot(coopCache, config);
        }
        return loot;
    }

    /** Суммарное целевое время 1 → максимум, часы. */
    public double autoTargetHours() {
        if (autoTargetHours <= 0) {
            autoTargetHours = AutoBalance.totalTargetHours(maxLevelCache, config);
        }
        return autoTargetHours;
    }

    /** Решения автобаланса для игрока (или дефолт, если данных нет). */
    public AutoBalance.Directive autoDirective(PlayerProgress progress) {
        if (progress == null) {
            return new AutoBalance.Directive();
        }
        return AutoBalance.evaluate(progress, config, maxLevelCache, now());
    }

    public static long now() {
        return System.currentTimeMillis() / 1000L;
    }

    /** Итоговый множитель и его составляющие. */
    public static class MultiplierBreakdown {
        public double pacing = 1;
        public double catchUp = 1;
        /** Множитель «режиссёра» (AutoBalance) по темпу и смертям. */
        public double auto = 1;
        /** Кооп-бонус за игру вместе (1 — ты один). */
        public double coop = 1;
        /** Бонус за возвращение после перерыва (1 — не действует). */
        public double returnBonus = 1;
        /** Штраф «печати» после смерти (1 — печати нет). */
        public double seal = 1;
        public double total = 1;
    }
}
