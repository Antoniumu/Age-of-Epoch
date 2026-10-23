package com.robertx22.age_of_exile.gamemaster;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Телеметрия «всё обо всём»: сырой журнал событий + агрегаты в памяти + отчёты по сезону.
 *
 * <p>Зачем: между сезонами нужно принимать решения по фактам, а не по ощущениям. Поэтому
 * собирается три слоя:
 * <ol>
 *     <li><b>Сырые события</b> — JSONL-журнал, по строке на событие (вход, смерть, убийство,
 *     дроп, каст, сундук, веха, награда). Каждая строка знает время, игрока, уровень,
 *     измерение, биом и координаты, поэтому потом можно считать что угодно, включая то,
 *     чего сегодня в коде ещё нет.</li>
 *     <li><b>Агрегаты</b> — счётчики по любым ключам вида {@code категория|значение}
 *     (например {@code biome_time|minecraft:forest}). Считаются в памяти, живут в
 *     {@code gamemaster_data.json}, мгновенно отдаются в веб-панель.</li>
 *     <li><b>Отчёты</b> — {@code TELEMETRY.md}-совместимый markdown по сезону: топы,
 *     профили игроков и автоматические выводы «что менять в следующем сезоне».</li>
 * </ol>
 *
 * <p>Ключевой принцип: телеметрия <b>никогда не влияет на игру</b>. Любая ошибка здесь
 * ловится и гасится, а не роняет тик.
 */
public class Telemetry {

    /** Потолок числа глобальных счётчиков: файл данных не должен расти бесконечно. */
    public static final int MAX_GLOBAL_KEYS = 5000;

    /** Потолок числа счётчиков на игрока. */
    public static final int MAX_PLAYER_KEYS = 300;

    /** Сколько последних событий держим в памяти для «живой ленты» в панели. */
    public static final int MAX_FEED = 80;

    // ------------------------------------------------------------------
    // Сохраняемое состояние
    // ------------------------------------------------------------------

    /** Состояние телеметрии, которое лежит в {@code gamemaster_data.json}. */
    public static class State {
        /** Имя текущего сезона. */
        public String season = "";
        /** Когда сезон начался (epoch seconds). */
        public long started = 0;
        /** Сколько событий записано за сезон. */
        public long events = 0;
        /** Глобальные счётчики: ключ {@code категория|значение} → сумма. */
        public Map<String, Double> counters = new HashMap<>();
        /** Счётчики по игрокам: uuid → (ключ → сумма). */
        public Map<String, Map<String, Double>> players = new HashMap<>();
        /** uuid → последний известный ник (чтобы отчёт читался по-человечески). */
        public Map<String, String> names = new HashMap<>();
    }

    /** Одно событие в «живой ленте» панели. */
    public static class FeedEntry {
        public long t;
        public String type = "";
        public String player = "";
        public String subject = "";
        public double value;
        public String dim = "";
        public String biome = "";
        public int level;
        public String extra = "";
    }

    private final GameMaster gm;

    private final Deque<FeedEntry> feed = new ArrayDeque<>();

    private BufferedWriter writer;
    private Path currentFile;
    private LocalDate currentDay;
    private long currentBytes;
    private long lastFlush;
    private long lastErrorLog;

    public Telemetry(GameMaster gm) {
        this.gm = gm;
    }

    // ------------------------------------------------------------------
    // Состояние и сезоны
    // ------------------------------------------------------------------

    /** Состояние телеметрии (создаётся при первом обращении). */
    public State state() {
        GameMasterData data = gm.data();
        if (data.telemetry == null) {
            data.telemetry = new State();
        }
        State state = data.telemetry;
        if (state.counters == null) {
            state.counters = new HashMap<>();
        }
        if (state.players == null) {
            state.players = new HashMap<>();
        }
        if (state.names == null) {
            state.names = new HashMap<>();
        }
        return state;
    }

    /** Телеметрия включена? */
    public boolean enabled() {
        if (!gm.isLoaded()) {
            return false;
        }
        GameMasterConfig config = gm.config();
        return config != null && config.telemetry_enabled;
    }

    private void ensureSeason(long now) {
        State state = state();
        if (state.started <= 0) {
            state.started = now;
        }
        if (state.season == null || state.season.isEmpty()) {
            String configured = gm.config() == null ? "" : gm.config().telemetry_season;
            state.season = configured == null || configured.isEmpty() ? "season-1" : configured;
        }
    }

    /** Имя текущего сезона. */
    public String season() {
        ensureSeason(GameMaster.now());
        return state().season;
    }

    /**
     * Начинает новый сезон: сохраняет отчёт и архив агрегатов, обнуляет счётчики.
     *
     * @return путь к отчёту прошлого сезона ({@code null}, если отчёт не записался)
     */
    public Path startSeason(String name, long now) {
        Path report = null;
        try {
            report = writeSeasonReport(now);
        } catch (Exception e) {
            log("не удалось записать отчёт сезона", e);
        }
        try {
            State state = state();
            String old = state.season == null || state.season.isEmpty() ? "season" : state.season;
            Path dir = seasonsDir();
            Files.createDirectories(dir);
            Path archive = dir.resolve(safeName(old) + "-" + now + ".json");
            Files.writeString(archive, GameMasterConfig.GSON.toJson(state), StandardCharsets.UTF_8);

            state.season = name == null || name.isEmpty() ? "season-" + (now / 86400L) : name;
            state.started = now;
            state.events = 0;
            state.counters = new HashMap<>();
            state.players = new HashMap<>();
            // имена оставляем: они нужны для отчётов, даже если игрок не играл в этом сезоне
            feed.clear();
            gm.markDirty();
        } catch (Exception e) {
            log("не удалось начать новый сезон", e);
        }
        return report;
    }

    // ------------------------------------------------------------------
    // Счётчики
    // ------------------------------------------------------------------

    /** Прибавляет значение к глобальному счётчику. */
    public void add(String key, double value) {
        if (key == null || value == 0 || !enabled()) {
            return;
        }
        try {
            State state = state();
            ensureSeason(GameMaster.now());
            state.counters.merge(key, value, Double::sum);
            if (state.counters.size() > MAX_GLOBAL_KEYS) {
                prune(state.counters, MAX_GLOBAL_KEYS);
            }
            gm.markDirty();
        } catch (Exception e) {
            log("add", e);
        }
    }

    /** Прибавляет значение к счётчику игрока. */
    public void addFor(String uuid, String key, double value) {
        if (uuid == null || key == null || value == 0 || !enabled()) {
            return;
        }
        try {
            State state = state();
            Map<String, Double> map = state.players.computeIfAbsent(uuid, k -> new HashMap<>());
            map.merge(key, value, Double::sum);
            if (map.size() > MAX_PLAYER_KEYS) {
                prune(map, MAX_PLAYER_KEYS);
            }
            gm.markDirty();
        } catch (Exception e) {
            log("addFor", e);
        }
    }

    /** Убирает самые маленькие значения, пока карта не влезет в лимит. */
    private static void prune(Map<String, Double> map, int limit) {
        if (map.size() <= limit) {
            return;
        }
        List<Map.Entry<String, Double>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparingDouble(Map.Entry::getValue));
        int toRemove = map.size() - limit;
        for (int i = 0; i < toRemove && i < entries.size(); i++) {
            map.remove(entries.get(i)
                .getKey());
        }
    }

    // ------------------------------------------------------------------
    // События
    // ------------------------------------------------------------------

    /**
     * Записывает событие: в живую ленту, в агрегаты и в JSONL-журнал.
     *
     * @param type    тип события (login, death, kill, loot, spell, crate, retain, level…)
     * @param player  игрок (может быть null для серверных событий)
     * @param subject объект события (моб, биом, предмет, причина смерти…)
     * @param value   числовое значение (урон, сумма, количество)
     * @param extra   свободный текст (редкость, деталь)
     */
    public void event(String type, ServerPlayer player, String subject, double value, String extra) {
        if (!enabled() || type == null) {
            return;
        }
        try {
            long now = GameMaster.now();
            ensureSeason(now);
            State state = state();
            state.events++;

            FeedEntry entry = new FeedEntry();
            entry.t = now;
            entry.type = type;
            entry.subject = subject == null ? "" : subject;
            entry.value = value;
            entry.extra = extra == null ? "" : extra;

            String uuid = null;
            if (player != null) {
                try {
                    uuid = player.getUUID()
                        .toString();
                    entry.player = player.getGameProfile()
                        .name();
                    state.names.put(uuid, entry.player);
                } catch (Exception ignored) {
                }
                try {
                    entry.dim = dimension(player.level());
                    entry.biome = biome(player.level(), player.blockPosition());
                } catch (Exception ignored) {
                }
                try {
                    entry.level = levelOf(player);
                } catch (Exception ignored) {
                }
                addFor(uuid, "event|" + type, 1);
                if (!entry.subject.isEmpty()) {
                    addFor(uuid, "subject_" + type + "|" + entry.subject, Math.max(1, value));
                }
            }

            add("event|" + type, 1);
            if (!entry.subject.isEmpty()) {
                add("subject_" + type + "|" + entry.subject, Math.max(1, value));
            }

            feed.addLast(entry);
            while (feed.size() > MAX_FEED) {
                feed.removeFirst();
            }

            if (gm.config().telemetry_events) {
                journal(entry, uuid);
            }
            gm.markDirty();
        } catch (Exception e) {
            log("event " + type, e);
        }
    }

    /** Компактные события-хелперы. */
    public void login(ServerPlayer player) {
        event("login", player, "", 0, "");
    }

    public void logout(ServerPlayer player, double sessionSeconds) {
        event("logout", player, "", Math.round(sessionSeconds), "");
        if (sessionSeconds > 60) {
            add("session_seconds", sessionSeconds);
            add("session_count", 1);
            if (player != null) {
                String uuid = player.getUUID()
                    .toString();
                addFor(uuid, "session_seconds", sessionSeconds);
                addFor(uuid, "session_count", 1);
            }
        }
    }

    public void levelUp(ServerPlayer player, int level) {
        event("level", player, String.valueOf(level), level, "");
    }

    public void death(ServerPlayer player, String cause) {
        event("death", player, cause == null ? "unknown" : cause, 1, "");
    }

    public void spell(ServerPlayer player, String spellId) {
        event("spell", player, spellId, 1, "");
    }

    public void loot(ServerPlayer player, String origin, String mobId, String rarity, int amount, int tier) {
        event("loot", player, mobId == null ? "" : mobId,
            Math.max(0, amount), origin + "|" + (rarity == null ? "" : rarity) + "|t" + tier);
        if (rarity != null && !rarity.isEmpty()) {
            add("loot_mob_rarity|" + rarity, 1);
            if (player != null) {
                addFor(player.getUUID()
                    .toString(), "loot_mob_rarity|" + rarity, 1);
            }
        }
    }

    public void gear(ServerPlayer player, String rarity, String itemId, int level) {
        String subject = itemId == null || itemId.isEmpty() ? rarity : itemId;
        event("gear", player, subject, 1, "rarity=" + (rarity == null ? "" : rarity) + "|lvl=" + level);
        if (rarity != null && !rarity.isEmpty()) {
            add("gear_rarity|" + rarity, 1);
            if (player != null) {
                addFor(player.getUUID()
                    .toString(), "gear_rarity|" + rarity, 1);
            }
        }
    }

    public void crate(ServerPlayer player, String type, String rewardId) {
        event("crate", player, rewardId == null ? "" : rewardId, 1, type);
    }

    public void retained(ServerPlayer player, String kind, String detail, double value) {
        event("retain", player, kind, value, detail);
    }

    /**
     * Смерть любого существа (регистрируется на {@code LivingDeathEvent}).
     * Даёт убийства с мобом, биомом и измерением — и смерти игроков с причиной.
     *
     * @param killer игрок, нанёсший смертельный удар (может быть null)
     */
    public void onLivingDeath(LivingEntity entity, String damageCause, ServerPlayer killer) {
        if (!enabled() || entity == null) {
            return;
        }
        try {
            if (entity instanceof ServerPlayer player) {
                death(player, damageCause);
                return;
            }
            // только настоящие мобы: стойки для брони и прочие «живые» объекты не считаем
            if (!(entity instanceof net.minecraft.world.entity.Mob)) {
                return;
            }
            String mobId = entity.getType() == null ? "unknown"
                : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                .toString();
            if (killer != null) {
                event("kill", killer, mobId, 1, "");
            } else {
                event("kill_env", null, mobId, 1, "");
            }
            add("kill_mob|" + mobId, 1);
            add("kill_biome|" + biome(entity.level(), entity.blockPosition()), 1);
        } catch (Exception e) {
            log("onLivingDeath", e);
        }
    }

    // ------------------------------------------------------------------
    // Сэмплирование мира (биомы, измерения, время суток)
    // ------------------------------------------------------------------

    /**
     * Раз в {@code telemetry_sample_seconds} фиксирует, где игрок находится.
     * Так появляется «сколько времени игрок провёл в каждом биоме/измерении»
     * без тяжёлых хуков на каждое перемещение.
     */
    public void sample(ServerPlayer player, PlayerProgress progress, boolean afk, int seconds, long now) {
        if (!enabled() || player == null || progress == null || afk || seconds <= 0) {
            return;
        }
        try {
            int interval = Math.max(1, gm.config().telemetry_sample_seconds);
            if (now - progress.telemetry_sample_epoch < interval) {
                return;
            }
            progress.telemetry_sample_epoch = now;

            String dim = dimension(player.level());
            String biome = biome(player.level(), player.blockPosition());
            String uuid = player.getUUID()
                .toString();
            String name = player.getGameProfile()
                .name();
            state().names.put(uuid, name);

            add("dim_time|" + dim, seconds);
            add("biome_time|" + biome, seconds);
            add("hour|" + Social.localHour(now), seconds);
            addFor(uuid, "dim_time|" + dim, seconds);
            addFor(uuid, "biome_time|" + biome, seconds);
            addFor(uuid, "hour|" + Social.localHour(now), seconds);
            addFor(uuid, "play_seconds", seconds);
        } catch (Exception e) {
            log("sample", e);
        }
    }

    // ------------------------------------------------------------------
    // Журнал (JSONL)
    // ------------------------------------------------------------------

    private void journal(FeedEntry entry, String uuid) {
        try {
            LocalDate today = LocalDate.now();
            if (writer == null || currentDay == null || !currentDay.equals(today)) {
                openWriter(today);
            }
            long maxBytes = Math.max(1, gm.config().telemetry_max_file_mb) * 1024L * 1024L;
            if (currentBytes > maxBytes) {
                closeWriter();
                openWriter(today);
            }
            StringBuilder line = new StringBuilder(256);
            line.append('{')
                .append("\"t\":").append(entry.t)
                .append(",\"season\":").append(quote(season()))
                .append(",\"type\":").append(quote(entry.type))
                .append(",\"player\":").append(quote(entry.player))
                .append(",\"uuid\":").append(quote(uuid == null ? "" : uuid))
                .append(",\"subject\":").append(quote(entry.subject))
                .append(",\"value\":").append(round2(entry.value))
                .append(",\"dim\":").append(quote(entry.dim))
                .append(",\"biome\":").append(quote(entry.biome))
                .append(",\"level\":").append(entry.level)
                .append(",\"extra\":").append(quote(entry.extra))
                .append('}');
            writer.write(line.toString());
            writer.newLine();
            currentBytes += line.length() + 1;
        } catch (Exception e) {
            log("journal", e);
            closeWriter();
        }
    }

    private void openWriter(LocalDate day) {
        try {
            Path dir = safeDir();
            Files.createDirectories(dir);
            cleanup(dir);
            Path file = dir.resolve("events-" + day + "-" + (System.currentTimeMillis() % 100000L) + ".jsonl");
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
            currentFile = file;
            currentDay = day;
            currentBytes = 0;
        } catch (Exception e) {
            log("не удалось открыть журнал телеметрии", e);
            writer = null;
        }
    }

    /** Удаляет старые журналы по сроку хранения. */
    private void cleanup(Path dir) {
        int days = Math.max(0, gm.config().telemetry_retention_days);
        if (days <= 0) {
            return;
        }
        long cutoff = System.currentTimeMillis() - days * 86400_000L;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jsonl")) {
            for (Path file : stream) {
                try {
                    if (Files.getLastModifiedTime(file)
                        .toMillis() < cutoff) {
                        Files.deleteIfExists(file);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** Сбрасывает буфер журнала на диск (вызывается из тика и при остановке сервера). */
    public void flush() {
        long now = System.currentTimeMillis();
        if (now - lastFlush < 1000) {
            return;
        }
        lastFlush = now;
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                closeWriter();
            }
        }
    }

    /** Закрывает журнал (остановка сервера). */
    public void close() {
        flush();
        closeWriter();
    }

    private void closeWriter() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (Exception ignored) {
            }
        }
        writer = null;
        currentFile = null;
        currentDay = null;
        currentBytes = 0;
    }

    /** Текущий файл журнала (для команд). */
    public String journalPath() {
        return currentFile == null ? "" : currentFile.toString();
    }

    // ------------------------------------------------------------------
    // Панель и отчёты
    // ------------------------------------------------------------------

    /** JSON для панели: топы, живая лента, сезон. */
    public JsonObject panelJson(int topCount) {
        JsonObject json = new JsonObject();
        State state = state();
        long now = GameMaster.now();
        json.addProperty("enabled", enabled());
        json.addProperty("season", state.season);
        json.addProperty("started", state.started);
        json.addProperty("days", state.started > 0 ? round2((now - state.started) / 86400.0) : 0);
        json.addProperty("events", state.events);
        json.addProperty("tracked_players", state.players.size());
        json.addProperty("journal", gm.config().telemetry_events ? journalPath() : "");
        double sessionSeconds = state.counters.getOrDefault("session_seconds", 0.0);
        double sessionCount = state.counters.getOrDefault("session_count", 0.0);
        json.addProperty("avg_session_minutes",
            sessionCount > 0 ? round2(sessionSeconds / sessionCount / 60.0) : 0);
        json.addProperty("sessions", Math.round(sessionCount));

        json.add("biomes", topJson(state.counters, "biome_time|", topCount));
        json.add("dimensions", topJson(state.counters, "dim_time|", topCount));
        json.add("mobs", topJson(state.counters, "kill_mob|", topCount));
        json.add("death_causes", topJson(state.counters, "subject_death|", topCount));
        json.add("loot", topJson(state.counters, "subject_loot|", topCount));
        json.add("gear", topJson(state.counters, "subject_gear|", topCount));
        json.add("spells", topJson(state.counters, "subject_spell|", topCount));
        json.add("crates", topJson(state.counters, "subject_crate|", topCount));
        json.add("chests", topJson(state.counters, "subject_chest|", topCount));
        json.add("retention", topJson(state.counters, "subject_retain|", topCount));
        json.add("hours", hoursJson(state.counters));

        JsonArray entries = new JsonArray();
        int skip = Math.max(0, feed.size() - Math.max(10, topCount * 2));
        int index = 0;
        for (FeedEntry entry : feed) {
            if (index++ < skip) {
                continue;
            }
            JsonObject item = new JsonObject();
            item.addProperty("t", entry.t);
            item.addProperty("type", entry.type);
            item.addProperty("player", entry.player);
            item.addProperty("subject", entry.subject);
            item.addProperty("value", round2(entry.value));
            item.addProperty("dim", entry.dim);
            item.addProperty("biome", entry.biome);
            item.addProperty("level", entry.level);
            item.addProperty("extra", entry.extra);
            entries.add(item);
        }
        json.add("feed", entries);
        return json;
    }

    /** Топ значений по префиксу ключа. */
    private JsonArray topJson(Map<String, Double> counters, String prefix, int limit) {
        Map<String, Double> filtered = new LinkedHashMap<>();
        if (counters != null) {
            for (Map.Entry<String, Double> entry : counters.entrySet()) {
                if (entry.getKey() != null && entry.getKey()
                    .startsWith(prefix) && entry.getValue() != null && entry.getValue() > 0) {
                    filtered.put(entry.getKey()
                        .substring(prefix.length()), entry.getValue());
                }
            }
        }
        List<Map.Entry<String, Double>> list = new ArrayList<>(filtered.entrySet());
        list.sort(Map.Entry.<String, Double>comparingByValue()
            .reversed());
        JsonArray array = new JsonArray();
        for (int i = 0; i < Math.min(Math.max(1, limit), list.size()); i++) {
            JsonObject item = new JsonObject();
            item.addProperty("key", list.get(i)
                .getKey());
            item.addProperty("value", round2(list.get(i)
                .getValue()));
            array.add(item);
        }
        return array;
    }

    private JsonArray hoursJson(Map<String, Double> counters) {
        JsonArray array = new JsonArray();
        for (int hour = 0; hour < 24; hour++) {
            Double value = counters.get("hour|" + hour);
            array.add(round2(value == null ? 0 : value));
        }
        return array;
    }

    /** Профиль предпочтений игрока — «что ему нравится» по фактическим данным. */
    public JsonObject preferencesJson(String uuid) {
        JsonObject json = new JsonObject();
        Map<String, Double> map = state().players.get(uuid);
        if (map == null || map.isEmpty()) {
            return json;
        }
        String biome = topKey(map, "biome_time|");
        String dim = topKey(map, "dim_time|");
        String mob = topKey(map, "subject_kill|");
        String death = topKey(map, "subject_death|");
        String spell = topKey(map, "subject_spell|");
        String hour = topKey(map, "hour|");
        double biomeSeconds = topValue(map, "biome_time|");
        double playSeconds = value(map, "play_seconds");

        json.addProperty("top_biome", biome);
        json.addProperty("top_biome_share", playSeconds > 0 ? round2(biomeSeconds / playSeconds) : 0);
        json.addProperty("top_dimension", dim);
        json.addProperty("top_mob", mob);
        json.addProperty("top_death", death);
        json.addProperty("top_spell", spell);
        json.addProperty("top_hour", hour);
        json.addProperty("play_seconds", round2(playSeconds));
        json.addProperty("kills", round2(value(map, "event|kill")));
        json.addProperty("deaths", round2(value(map, "event|death")));
        json.addProperty("distance", round2(value(map, "distance")));
        json.addProperty("style", style(map, playSeconds));
        return json;
    }

    /** Приблизительный стиль игры по соотношению активностей. */
    private String style(Map<String, Double> map, double playSeconds) {
        double kills = value(map, "event|kill");
        double distance = value(map, "distance");
        double loot = value(map, "event|loot");
        double biomeCount = 0;
        for (String key : map.keySet()) {
            if (key.startsWith("biome_time|") && value(map, key) > 60) {
                biomeCount++;
            }
        }
        if (playSeconds < 900) {
            return "мало данных";
        }
        if (kills > 0 && distance / Math.max(1, kills) < 30 && loot / Math.max(1, kills) > 0.5) {
            return "фарм на месте";
        }
        if (biomeCount >= 5 && distance > kills * 60) {
            return "исследователь";
        }
        if (kills > playSeconds / 60.0) {
            return "воин";
        }
        return "смешанный";
    }

    private static String topKey(Map<String, Double> map, String prefix) {
        String best = "";
        double bestValue = 0;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey()
                .startsWith(prefix) && entry.getValue() != null && entry.getValue() > bestValue) {
                bestValue = entry.getValue();
                best = entry.getKey()
                    .substring(prefix.length());
            }
        }
        return best;
    }

    private static double topValue(Map<String, Double> map, String prefix) {
        double best = 0;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey()
                .startsWith(prefix) && entry.getValue() != null) {
                best = Math.max(best, entry.getValue());
            }
        }
        return best;
    }

    private static double value(Map<String, Double> map, String key) {
        Double value = map.get(key);
        return value == null ? 0 : value;
    }

    // ------------------------------------------------------------------
    // Отчёт по сезону
    // ------------------------------------------------------------------

    /**
     * Пишет markdown-отчёт по текущему сезону: топы, профили игроков и автоматические
     * выводы. Именно этот файл читается между сезонами.
     */
    public Path writeSeasonReport(long now) throws IOException {
        State state = state();
        Path dir = baseDir();
        Files.createDirectories(dir);
        Path file = dir.resolve("season_report_" + safeName(state.season) + ".md");

        StringBuilder md = new StringBuilder();
        md.append("# Отчёт по сезону ").append(state.season)
            .append("\n\n");
        md.append("Сформирован: ").append(java.time.Instant.ofEpochSecond(now)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime())
            .append(" UTC\n\n");
        double days = state.started > 0 ? Math.max(0.01, (now - state.started) / 86400.0) : 0;
        md.append("Длительность: **").append(round2(days)).append("** дн., событий: **")
            .append(state.events).append("**, игроков с данными: **")
            .append(state.players.size()).append("**\n\n");

        md.append("## 1. Где игроки проводили время\n\n");
        appendTable(md, "Биом", state.counters, "biome_time|", true);
        appendTable(md, "Измерение", state.counters, "dim_time|", true);

        md.append("## 2. Сессии\n\n");
        double sessionSeconds = state.counters.getOrDefault("session_seconds", 0.0);
        double sessionCount = state.counters.getOrDefault("session_count", 0.0);
        double avgSession = sessionCount > 0 ? sessionSeconds / sessionCount / 60.0 : 0;
        md.append("| Показатель | Значение |\n|---|---:|\n");
        md.append("| Сессий (дольше минуты) | ").append(Math.round(sessionCount)).append(" |\n");
        md.append("| Средняя длительность | ").append(round2(avgSession)).append(" мин |\n");
        md.append("| Всего времени в игре | ").append(round2(sessionSeconds / 3600.0)).append(" ч |\n\n");

        md.append("## 3. Бой\n\n");
        appendTable(md, "Убитые мобы", state.counters, "kill_mob|", false);
        appendTable(md, "Причины смерти игроков", state.counters, "subject_death|", false);

        md.append("## 4. Добыча и способности\n\n");
        appendTable(md, "Редкости предметов", state.counters, "subject_gear|", false);
        appendTable(md, "Редкость убитых мобов", state.counters, "loot_mob_rarity|", false);
        appendTable(md, "С каких мобов падал дроп", state.counters, "subject_loot|", false);
        appendTable(md, "Использованные способности", state.counters, "subject_spell|", false);
        appendTable(md, "Сундуки мира и данжей", state.counters, "subject_chest|", false);
        appendTable(md, "Открытые лут-сундуки", state.counters, "subject_crate|", false);
        appendTable(md, "События удержания", state.counters, "subject_retain|", false);

        md.append("## 5. Время по часам\n\n");
        md.append("| Час | Часы игры |\n|---:|---:|\n");
        for (int hour = 0; hour < 24; hour++) {
            Double value = state.counters.get("hour|" + hour);
            double hours = (value == null ? 0 : value) / 3600.0;
            md.append("| ").append(String.format("%02d:00", hour)).append(" | ")
                .append(round2(hours)).append(" |\n");
        }
        md.append('\n');

        md.append("## 6. Профили игроков\n\n");
        md.append("| Игрок | Ур. | Часов | Убийств | Смертей | Любимый биом | Доля | Стиль |\n");
        md.append("|---|---:|---:|---:|---:|---|---:|---|\n");
        for (Map.Entry<String, Map<String, Double>> entry : state.players.entrySet()) {
            Map<String, Double> map = entry.getValue();
            String name = state.names.getOrDefault(entry.getKey(), entry.getKey());
            PlayerProgress progress = gm.data()
                .get(entry.getKey());
            double playSeconds = value(map, "play_seconds");
            String biome = topKey(map, "biome_time|");
            double share = playSeconds > 0 ? topValue(map, "biome_time|") / playSeconds : 0;
            md.append("| ").append(name).append(" | ")
                .append(progress == null ? "?" : progress.level).append(" | ")
                .append(round2(playSeconds / 3600.0)).append(" | ")
                .append(Math.round(value(map, "event|kill"))).append(" | ")
                .append(Math.round(value(map, "event|death"))).append(" | ")
                .append(biome.isEmpty() ? "—" : biome).append(" | ")
                .append(Math.round(share * 100)).append("% | ")
                .append(style(map, playSeconds)).append(" |\n");
        }
        md.append('\n');

        md.append("## 7. Выводы и что менять\n\n");
        for (String line : conclusions(state)) {
            md.append("* ").append(line).append('\n');
        }
        md.append('\n');
        md.append("---\n\n");
        md.append("Сырые события за сезон лежат в `telemetry/` (JSONL, по строке на событие).\n");
        md.append("Полный срез агрегатов: `seasons/").append(safeName(state.season)).append("-*.json`.\n");

        Files.writeString(file, md.toString(), StandardCharsets.UTF_8);
        return file;
    }

    /** Автоматические выводы: правила, а не гадание. */
    private List<String> conclusions(State state) {
        List<String> lines = new ArrayList<>();
        double totalBiome = sumPrefix(state.counters, "biome_time|");
        double totalDeaths = sumPrefix(state.counters, "subject_death|");
        double totalKills = sumPrefix(state.counters, "kill_mob|");
        double totalSpells = sumPrefix(state.counters, "subject_spell|");

        if (totalBiome <= 0) {
            lines.add("Данных по биомам нет — слишком короткий сезон или телеметрия была выключена.");
        } else {
            int unused = 0;
            for (Map.Entry<String, Double> entry : state.counters.entrySet()) {
                if (entry.getKey()
                    .startsWith("biome_time|") && entry.getValue() / totalBiome < 0.01) {
                    unused++;
                }
            }
            if (unused > 0) {
                lines.add("**" + unused + " биом(ов)** дают меньше 1 % времени — контент туда не долетает "
                    + "или туда нет смысла идти. Кандидаты: направить туда квесты/награды следующего сезона.");
            }
            String topBiome = topKey(state.counters, "biome_time|");
            double topShare = totalBiome > 0 ? topValue(state.counters, "biome_time|") / totalBiome : 0;
            if (topShare > 0.5 && !topBiome.isEmpty()) {
                lines.add("**" + Math.round(topShare * 100) + " %** времени проходит в `" + topBiome
                    + "` — стоит проверить, не «дом» ли это, и дать причины выходить дальше.");
            }
        }

        if (totalDeaths > 0) {
            String cause = topKey(state.counters, "subject_death|");
            double share = topValue(state.counters, "subject_death|") / totalDeaths;
            if (share >= 0.3) {
                lines.add("**" + Math.round(share * 100) + " %** смертей от `" + cause
                    + "` — самый дешёвый способ улучшить сезон: ослабить именно этот источник.");
            }
            if (totalKills > 0 && totalDeaths / totalKills > 0.1) {
                lines.add("Смертей больше 10 % от убийств — контент слишком жёсткий для текущей аудитории.");
            } else if (totalDeaths / Math.max(1, totalKills) < 0.02) {
                lines.add("Смертей почти нет (<2 % от убийств) — сложность можно поднять, иначе нет вызова.");
            }
        }

        double sessionCount = state.counters.getOrDefault("session_count", 0.0);
        double sessionSeconds = state.counters.getOrDefault("session_seconds", 0.0);
        double avgSession = sessionCount > 0 ? sessionSeconds / sessionCount / 60.0 : 0;
        if (sessionCount >= 3) {
            if (avgSession < 20) {
                lines.add("Средняя сессия **" + Math.round(avgSession)
                    + " мин** — очень короткая. Приоритет: цель на сессию и быстрый первый результат, "
                    + "а не новый контент.");
            } else if (avgSession > 120) {
                lines.add("Средняя сессия **" + Math.round(avgSession)
                    + " мин** — люди залипают. Проверить, не держит ли их гринд, и не растёт ли усталость.");
            }
        }

        if (totalSpells <= 0 && totalKills > 0) {
            lines.add("Способности не используются — проверить стоимость маны/энергии и подсказки в интерфейсе.");
        }

        if (state.started > 0) {
            double days = (GameMaster.now() - state.started) / 86400.0;
            if (days >= 2 && state.players.size() < 4) {
                lines.add("Меньше 4 игроков с данными за " + Math.round(days)
                    + " дн. — приоритет следующего сезона не контент, а приглашение и первый час игры.");
            }
        }

        if (lines.isEmpty()) {
            lines.add("Явных проблем не найдено: топы ровные, смертность в норме.");
        }
        return lines;
    }

    private static void appendTable(StringBuilder md, String title, Map<String, Double> counters,
                                    String prefix, boolean asHours) {
        Map<String, Double> filtered = new HashMap<>();
        for (Map.Entry<String, Double> entry : counters.entrySet()) {
            if (entry.getKey()
                .startsWith(prefix)) {
                filtered.put(entry.getKey()
                    .substring(prefix.length()), entry.getValue());
            }
        }
        if (filtered.isEmpty()) {
            md.append("### ").append(title).append("\n\nнет данных\n\n");
            return;
        }
        List<Map.Entry<String, Double>> list = new ArrayList<>(filtered.entrySet());
        list.sort(Map.Entry.<String, Double>comparingByValue()
            .reversed());
        md.append("### ").append(title).append("\n\n");
        md.append("| Значение | ").append(asHours ? "Часы" : "Количество").append(" |\n");
        md.append("|---|---:|\n");
        int limit = Math.min(20, list.size());
        for (int i = 0; i < limit; i++) {
            double value = list.get(i)
                .getValue();
            md.append("| ").append(list.get(i)
                    .getKey()).append(" | ")
                .append(round2(asHours ? value / 3600.0 : value)).append(" |\n");
        }
        if (list.size() > limit) {
            md.append("| … ещё ").append(list.size() - limit).append(" | |\n");
        }
        md.append('\n');
    }

    private static double sumPrefix(Map<String, Double> counters, String prefix) {
        double sum = 0;
        for (Map.Entry<String, Double> entry : counters.entrySet()) {
            if (entry.getKey()
                .startsWith(prefix) && entry.getValue() != null) {
                sum += entry.getValue();
            }
        }
        return sum;
    }

    // ------------------------------------------------------------------
    // Утилиты
    // ------------------------------------------------------------------

    public static String dimension(Level level) {
        try {
            return level.dimension()
                .identifier()
                .toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String biome(Level level, BlockPos pos) {
        try {
            return level.getBiome(pos)
                .unwrapKey()
                .map(key -> key.location()
                    .toString())
                .orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static int levelOf(ServerPlayer player) {
        try {
            return com.robertx22.age_of_exile.uncommon.datasaving.Load.Unit(player)
                .getLevel();
        } catch (Exception e) {
            return 0;
        }
    }

    private static Path baseDir() {
        return GameMaster.baseDir();
    }

    private static Path safeDir() {
        return baseDir().resolve("telemetry");
    }

    private static Path seasonsDir() {
        return baseDir().resolve("seasons");
    }

    private static String safeName(String name) {
        if (name == null || name.isEmpty()) {
            return "season";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(' ');
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private void log(String where, Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLog < 60_000L) {
            return;
        }
        lastErrorLog = now;
        System.out.println("[Telemetry] " + where + ": " + e);
    }
}
