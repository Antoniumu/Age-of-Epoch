package com.robertx22.age_of_exile.gamemaster;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** One-time migration of the old shared GameMaster files into a world. */
final class WorldStorage {
    private static final String DONE = ".world-storage-initialized";
    private static final String IN_PROGRESS = ".legacy-migration-in-progress";
    private static final List<String> LEGACY_NAMES = List.of(
        "gamemaster.json", "gamemaster_data.json", "gamemaster_export.csv", "telemetry", "seasons");

    private WorldStorage() {
    }

    static void initialize(Path worldDir, Path legacyDir) throws IOException {
        Files.createDirectories(worldDir);
        Path done = worldDir.resolve(DONE);
        Path progress = worldDir.resolve(IN_PROGRESS);
        if (Files.exists(done)) {
            return;
        }
        // Never import shared data into an already initialized world. The progress marker
        // distinguishes a pre-existing world from an interrupted migration, which can resume.
        if (!Files.exists(progress) && hasWorldData(worldDir)) {
            Files.createFile(done);
            System.out.println("[GameMaster] Данные мира уже существуют; старые общие файлы не импортированы: " + legacyDir);
            return;
        }
        if (!Files.exists(progress)) {
            Files.createFile(progress);
        }
        for (String name : LEGACY_NAMES) {
            migrate(legacyDir.resolve(name), worldDir.resolve(name));
        }
        if (Files.isDirectory(legacyDir)) {
            try (DirectoryStream<Path> reports = Files.newDirectoryStream(legacyDir, "season_report_*.md")) {
                for (Path report : reports) {
                    migrate(report, worldDir.resolve(report.getFileName()));
                }
            }
        }
        Files.move(progress, done);
    }

    private static boolean hasWorldData(Path dir) throws IOException {
        for (String name : LEGACY_NAMES) {
            if (Files.exists(dir.resolve(name))) {
                return true;
            }
        }
        try (DirectoryStream<Path> reports = Files.newDirectoryStream(dir, "season_report_*.md")) {
            return reports.iterator().hasNext();
        }
    }

    private static void migrate(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        if (Files.exists(target)) {
            System.out.println("[GameMaster] Файл мира уже существует, старый файл оставлен без изменений: " + source);
            return;
        }
        Files.move(source, target);
        System.out.println("[GameMaster] Перенесено в мир: " + source + " -> " + target);
    }
}
