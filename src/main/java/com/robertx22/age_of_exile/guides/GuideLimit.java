package com.robertx22.age_of_exile.guides;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.forge_compat.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Предел, до которого гайды ведут игрока (по умолчанию 20 уровень).
 *
 * Два способа настройки, оба — на стороне хоста:
 *
 *  1. Файл {@code config/age_of_exile_guides.json} в каталоге игры/сервера
 *     (создаётся сам при первом запуске). Он перекрывает датапак и читается
 *     при старте сервера, после загрузки датапаков.
 *
 *  2. Датапак: {@code data/mmorpg/game_balance/game_balance.json},
 *     поле {@code GUIDE_MAX_LEVEL}. Значение синхронизируется клиентам, поэтому
 *     этот способ работает и в мультиплеере, и в одиночной игре.
 *
 * Файл нужен для тех, кто не хочет править датапак: одно число в одном файле.
 */
public class GuideLimit {

    public static final String FILE_NAME = "age_of_exile_guides.json";
    public static final int DEFAULT_MAX_LEVEL = 20;

    /** Максимум, до которого гайд может вести игрока (жёсткий предел игры). */
    public static int hardLimit() {
        try {
            GameBalanceConfig cfg = GameBalanceConfig.get();
            if (cfg != null && cfg.MAX_LEVEL > 0) {
                return cfg.MAX_LEVEL;
            }
        } catch (Exception ignored) {
        }
        return 50;
    }

    /** Значение из файла/датапака, приведённое к допустимому диапазону. */
    public static int clamp(int level) {
        if (level <= 0) {
            return DEFAULT_MAX_LEVEL;
        }
        return Math.min(level, hardLimit());
    }

    /**
     * Чтение файла при старте сервера. Отсутствующий файл создаётся с дефолтом,
     * чтобы хост видел, что настраивать. Битый файл не ломает игру: остаётся
     * значение из датапака.
     */
    public static void loadFromDisk() {
        Path path;
        try {
            path = FMLPaths.CONFIGDIR.get()
                .resolve(FILE_NAME);
        } catch (Exception e) {
            return;
        }

        try {
            if (!Files.exists(path)) {
                writeDefault(path);
                return;
            }
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(text)
                .getAsJsonObject();
            if (!json.has("max_level")) {
                return;
            }
            int level = json.get("max_level")
                .getAsInt();
            if (level <= 0) {
                return; // «как в датапаке»
            }
            apply(level);
        } catch (Exception e) {
            System.out.println("[Age of Epoch] Не удалось прочитать " + FILE_NAME + ": " + e.getMessage()
                + " — остаётся значение из датапака.");
        }
    }

    private static void writeDefault(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String text = "{\n"
                + "  \"about\": \"До какого уровня гайды ведут игрока. Дальше подсказки и подсветка кнопок выключаются. 0 = брать значение из датапака (game_balance -> GUIDE_MAX_LEVEL).\",\n"
                + "  \"max_level\": " + DEFAULT_MAX_LEVEL + "\n"
                + "}\n";
            Files.write(path, text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    /** Записать предел в загруженный конфиг: он уйдёт клиентам при входе. */
    private static void apply(int level) {
        try {
            GameBalanceConfig cfg = GameBalanceConfig.get();
            if (cfg != null) {
                GuideMath.RUNTIME_GUIDE_MAX_LEVEL = clamp(level);
            }
        } catch (Exception ignored) {
        }
    }
}
