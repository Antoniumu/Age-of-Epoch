package com.robertx22.age_of_exile.a_libraries.mob_plates;

import com.robertx22.age_of_exile.config.forge.ClientConfigs;
import com.robertx22.age_of_exile.config.forge.MobPlateStyle;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * User-editable settings for the mob plates.
 *
 * <p>The {@code forge_compat} config layer keeps its values in memory only, so the
 * plate options would not survive a restart. This class reads and writes a small
 * {@code .properties} file instead:</p>
 *
 * <pre>config/age_of_epoch/mob_plates.properties</pre>
 *
 * <p>The file is created on first launch. The in-game keybind only rewrites the
 * {@code style} entry, leaving everything else untouched.</p>
 */
public final class MobPlateSettings {

    private static final String FILE_NAME = "mob_plates.properties";

    private MobPlateSettings() {
    }

    public static Path path() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve("age_of_epoch")
            .resolve(FILE_NAME);
    }

    public static void load() {
        ClientConfigs cfg = ClientConfigs.getConfig();
        Properties props = new Properties();
        Path path = path();

        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (Exception ignored) {
            }
        }

        cfg.MOB_PLATE_STYLE.set(MobPlateStyle.byName(props.getProperty("style", cfg.MOB_PLATE_STYLE.get()
            .name())));
        cfg.MOB_PLATE_SCALE.set(clampDouble(props.getProperty("scale"), cfg.MOB_PLATE_SCALE.get(), 0.6D, 1.8D));
        cfg.MOB_PLATE_MAX_DISTANCE.set((int) clampDouble(props.getProperty("max_distance"),
            cfg.MOB_PLATE_MAX_DISTANCE.get(), 8, 64));
        cfg.MOB_PLATE_SHOW_LEVEL.set(readBool(props.getProperty("show_level"), cfg.MOB_PLATE_SHOW_LEVEL.get()));
        cfg.MOB_PLATE_SHOW_HP_TEXT.set(readBool(props.getProperty("show_hp_text"), cfg.MOB_PLATE_SHOW_HP_TEXT.get()));
        cfg.MOB_PLATE_ONLY_LOOKED_AT.set(readBool(props.getProperty("only_looked_at"),
            cfg.MOB_PLATE_ONLY_LOOKED_AT.get()));
        cfg.MOB_PLATE_HIDE_FULL_HEALTH.set(readBool(props.getProperty("hide_full_health"),
            cfg.MOB_PLATE_HIDE_FULL_HEALTH.get()));
        cfg.MOB_PLATE_SHOW_PLAYERS.set(readBool(props.getProperty("show_players"), cfg.MOB_PLATE_SHOW_PLAYERS.get()));

        // Rewrite the file so a missing entry (or a hand-edited typo) is repaired
        // and the defaults become discoverable.
        save();
    }

    public static void save() {
        ClientConfigs cfg = ClientConfigs.getConfig();
        Properties props = new Properties();
        props.setProperty("style", cfg.MOB_PLATE_STYLE.get()
            .name());
        props.setProperty("scale", String.valueOf(cfg.MOB_PLATE_SCALE.get()));
        props.setProperty("max_distance", String.valueOf(cfg.MOB_PLATE_MAX_DISTANCE.get()));
        props.setProperty("show_level", String.valueOf(cfg.MOB_PLATE_SHOW_LEVEL.get()));
        props.setProperty("show_hp_text", String.valueOf(cfg.MOB_PLATE_SHOW_HP_TEXT.get()));
        props.setProperty("only_looked_at", String.valueOf(cfg.MOB_PLATE_ONLY_LOOKED_AT.get()));
        props.setProperty("hide_full_health", String.valueOf(cfg.MOB_PLATE_HIDE_FULL_HEALTH.get()));
        props.setProperty("show_players", String.valueOf(cfg.MOB_PLATE_SHOW_PLAYERS.get()));

        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "Age of Epoch - mob plates. Styles: CLASSIC, MODERN, RUNE (key J in game)");
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean readBool(String raw, boolean fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return fallback;
    }

    private static double clampDouble(String raw, double fallback, double min, double max) {
        if (raw == null) {
            return fallback;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
