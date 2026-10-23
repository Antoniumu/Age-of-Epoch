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
        stopWeb();
        save();
        for (PlayerProgress progress : data.players.values()) {
            progress.online = false;
        }
        loaded = false;
        server = null;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public GameMasterConfig config() {
        return config;
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

        for (ServerPlayer player : server.getPlayerList()
            .getPlayers()) {
            try {
                tickPlayer(player, now, seconds);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
        if (config.auto_balance && config.auto_welcome_hours > 0 && progress.last_seen > 0
            && now - progress.last_seen >= config.auto_welcome_hours * 3600.0) {
            progress.welcome_until = now + 1800;
        }
        progress.online = true;
        progress.last_seen = now;
        checkLevel(player, progress);
        progress.multiplier = computeMultiplier(progress);
        if (progress.history.isEmpty()) {
            progress.addHistory(progress.multiplier, progress.last_seen);
        }
        dirty = true;

        if (config.enabled && config.announce_on_login && (config.pacing || config.catch_up || config.auto_balance)) {
            MultiplierBreakdown breakdown = breakdown(progress);
            String text = "GameMaster: множитель опыта x" + fmt(breakdown.total)
                + " (темп x" + fmt(breakdown.pacing) + ", догоняющий x" + fmt(breakdown.catchUp)
                + ", автобаланс x" + fmt(breakdown.auto) + ")";
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

        double sumHealth = 0;
        double sumDamage = 0;
        double sumLoot = 0;
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

            sumHealth += directive.mob_health;
            sumDamage += directive.mob_damage;
            sumLoot += directive.loot;
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
            autoMobHealth = AutoBalance.rateLimit(autoMobHealth, sumHealth / count, step, minutes);
            autoMobDamage = AutoBalance.rateLimit(autoMobDamage, sumDamage / count, step, minutes);
            autoMobHealth = AutoBalance.clamp(autoMobHealth, config.auto_mob_health_min, config.auto_mob_health_max);
            autoMobDamage = AutoBalance.clamp(autoMobDamage, config.auto_mob_damage_min, config.auto_mob_damage_max);
        } else {
            autoMobHealth = 1.0;
            autoMobDamage = 1.0;
        }

        if (config.auto_touch_loot) {
            autoLoot = AutoBalance.rateLimit(autoLoot, sumLoot / count, step, minutes);
            autoLoot = AutoBalance.clamp(autoLoot, 1.0, config.auto_loot_max);
        } else {
            autoLoot = 1.0;
        }

        autoGlobalUpdated = now;
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
        breakdown.total = ExpTuner.clamp(breakdown.pacing * breakdown.catchUp * breakdown.auto,
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

    /** Глобальный множитель шанса дропа. */
    public double lootMultiplier() {
        return config.enabled && config.auto_balance && config.auto_touch_loot ? autoLoot : 1.0;
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
        public double total = 1;
    }
}
