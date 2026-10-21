package com.robertx22.world_of_exile.tower;

import java.util.List;

/**
 * A tower family: one biome group, one palette, one mob pool and one boss
 * roster.  The ids match the data-driven structures under
 * {@code data/world_of_exile/worldgen/structure/<id>_t*.json} and the
 * {@code [tower:<id>]} tag written into the generated structure templates.
 */
public final class TowerTheme {

    private final String id;
    private final String name;
    private final int baseRank;
    private final boolean nether;
    private final List<String> mobs;
    private final String foundation;

    TowerTheme(String id, String name, int baseRank, boolean nether, String foundation, List<String> mobs) {
        this.id = id;
        this.name = name;
        this.baseRank = baseRank;
        this.nether = nether;
        this.foundation = foundation;
        this.mobs = List.copyOf(mobs);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** Biome danger of the theme, 1 (plains) .. 4 (dark forest / nether). */
    public int baseRank() {
        return baseRank;
    }

    public boolean nether() {
        return nether;
    }

    /** Block used to patch holes the boss digs or falls through. */
    public String foundation() {
        return foundation;
    }

    public List<String> mobs() {
        return mobs;
    }

    public String randomMob(net.minecraft.util.RandomSource random) {
        if (mobs.isEmpty()) {
            return nether ? "minecraft:wither_skeleton" : "minecraft:zombie";
        }
        return mobs.get(random.nextInt(mobs.size()));
    }

    @Override
    public String toString() {
        return id;
    }
}
