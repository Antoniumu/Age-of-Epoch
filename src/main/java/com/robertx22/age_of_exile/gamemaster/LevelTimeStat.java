package com.robertx22.age_of_exile.gamemaster;

/**
 * Агрегированное по всем игрокам время, проведённое на уровне.
 * Нужно, чтобы измерить «базовую» скорость развития сборки и откалибровать темп.
 */
public class LevelTimeStat {

    /** Суммарные базовые секунды (при множителе 1), проведённые на этом уровне. */
    public double seconds = 0;

    /** Сколько раз этот уровень был пройден. */
    public int completions = 0;

    public LevelTimeStat() {
    }

    public void add(double seconds, int completions) {
        this.seconds += Math.max(0, seconds);
        this.completions += Math.max(0, completions);
    }
}
