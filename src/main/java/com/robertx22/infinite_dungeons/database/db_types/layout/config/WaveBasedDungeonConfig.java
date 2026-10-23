package com.robertx22.infinite_dungeons.database.db_types.layout.config;

public class WaveBasedDungeonConfig {

    public int total_waves = 5;

    public int mobs_per_wave = 3;

    public int extra_mobs_per_each_wave = 2;

    public int between_wave_cooldown_seconds = 5;

    public int getMobsToSpawnForWave(int wave) {

        int num = mobs_per_wave + (wave * extra_mobs_per_each_wave);

        return num;

    }

}
