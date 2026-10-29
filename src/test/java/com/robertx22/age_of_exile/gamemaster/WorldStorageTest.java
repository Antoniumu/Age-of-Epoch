package com.robertx22.age_of_exile.gamemaster;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorldStorageTest {
    @TempDir Path root;

    @Test
    void migratesLegacyFilesOnceAndKeepsClientPreferences() throws Exception {
        Path legacy = Files.createDirectories(root.resolve("config/age_of_epoch"));
        Path world = root.resolve("world/age_of_epoch");
        Files.writeString(legacy.resolve("gamemaster.json"), "settings");
        Files.writeString(legacy.resolve("gamemaster_data.json"), "data");
        Files.writeString(legacy.resolve("party_hud.properties"), "client");
        Files.writeString(legacy.resolve("season_report_one.md"), "report");
        Files.writeString(legacy.resolve("gamemaster_export.csv"), "export");
        Files.writeString(Files.createDirectories(legacy.resolve("telemetry")).resolve("events.jsonl"), "events");
        Files.writeString(Files.createDirectories(legacy.resolve("seasons")).resolve("one.json"), "archive");
        WorldStorage.initialize(world, legacy);
        assertEquals("settings", Files.readString(world.resolve("gamemaster.json")));
        assertEquals("data", Files.readString(world.resolve("gamemaster_data.json")));
        assertEquals("events", Files.readString(world.resolve("telemetry/events.jsonl")));
        assertEquals("archive", Files.readString(world.resolve("seasons/one.json")));
        assertEquals("report", Files.readString(world.resolve("season_report_one.md")));
        assertEquals("export", Files.readString(world.resolve("gamemaster_export.csv")));
        assertFalse(Files.exists(legacy.resolve("gamemaster_data.json")));
        assertTrue(Files.exists(legacy.resolve("party_hud.properties")));
        Files.writeString(legacy.resolve("gamemaster_data.json"), "other");
        WorldStorage.initialize(world, legacy);
        assertEquals("data", Files.readString(world.resolve("gamemaster_data.json")));
        assertEquals("other", Files.readString(legacy.resolve("gamemaster_data.json")));
    }

    @Test
    void separateWorldsDoNotShareMigratedData() throws Exception {
        Path legacy = Files.createDirectories(root.resolve("config"));
        Files.writeString(legacy.resolve("gamemaster_data.json"), "first world");
        Path first = root.resolve("first/age_of_epoch");
        Path second = root.resolve("second/age_of_epoch");
        WorldStorage.initialize(first, legacy);
        WorldStorage.initialize(second, legacy);
        assertEquals("first world", Files.readString(first.resolve("gamemaster_data.json")));
        assertFalse(Files.exists(second.resolve("gamemaster_data.json")));
    }

    @Test
    void existingWorldIsNotContaminatedByLegacyData() throws Exception {
        Path legacy = Files.createDirectories(root.resolve("config"));
        Path world = Files.createDirectories(root.resolve("world"));
        Files.writeString(world.resolve("gamemaster_data.json"), "world");
        Files.writeString(legacy.resolve("gamemaster_data.json"), "legacy");
        Files.writeString(legacy.resolve("gamemaster.json"), "legacy settings");
        WorldStorage.initialize(world, legacy);
        assertEquals("world", Files.readString(world.resolve("gamemaster_data.json")));
        assertFalse(Files.exists(world.resolve("gamemaster.json")));
        assertEquals("legacy", Files.readString(legacy.resolve("gamemaster_data.json")));
    }

    @Test
    void interruptedMigrationResumesWithoutOverwriting() throws Exception {
        Path legacy = Files.createDirectories(root.resolve("config"));
        Path world = Files.createDirectories(root.resolve("world"));
        Files.createFile(world.resolve(".legacy-migration-in-progress"));
        Files.writeString(world.resolve("gamemaster_data.json"), "already moved");
        Files.writeString(legacy.resolve("gamemaster_data.json"), "conflict");
        Files.writeString(legacy.resolve("gamemaster.json"), "settings");
        WorldStorage.initialize(world, legacy);
        assertEquals("already moved", Files.readString(world.resolve("gamemaster_data.json")));
        assertEquals("conflict", Files.readString(legacy.resolve("gamemaster_data.json")));
        assertEquals("settings", Files.readString(world.resolve("gamemaster.json")));
        assertFalse(Files.exists(world.resolve(".legacy-migration-in-progress")));
    }

    @Test
    void restoresTelemetryAndSocialStateFromJson() {
        GameMasterData parsed = new Gson().fromJson("""
            {"telemetry":{"season":"spring","events":42,"counters":{"kill":3}},
             "hour_activity":[1,2],"firsts":{"kill":"Alice"},"week_index":9,
             "week_kills":7,"week_goal_epoch":123,"week_claimed":["Alice"],"last_goal_hint":456}
            """, GameMasterData.class);
        GameMasterData restored = new GameMasterData();
        restored.copyFrom(parsed);
        assertEquals("spring", restored.telemetry.season);
        assertEquals(42, restored.telemetry.events);
        assertEquals(3.0, restored.telemetry.counters.get("kill").doubleValue());
        assertEquals(2, restored.hour_activity[1]);
        assertEquals("Alice", restored.firsts.get("kill"));
        assertEquals(9, restored.week_index);
        assertEquals(7, restored.week_kills);
        assertEquals(123, restored.week_goal_epoch);
        assertEquals("Alice", restored.week_claimed.get(0));
        assertEquals(456, restored.last_goal_hint);
    }
}
