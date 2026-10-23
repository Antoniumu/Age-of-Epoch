package com.robertx22.age_of_exile.database.data.stats.types.professions.all;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;

public enum BonusRequirement {

    NONE("none", "None") {
        @Override
        public boolean isAllowed(Level world, BlockPos pos) {
            return true;
        }
    },
    RAINING("rain", "Raining") {
        @Override
        public boolean isAllowed(Level world, BlockPos pos) {
            return world.isRaining();
        }
    },
    NIGHT("night", "Night Time") {
        @Override
        public boolean isAllowed(Level world, BlockPos pos) {
            return (world.getOverworldClockTime() % 24000L) >= 12000L;
        }
    },
    DAY("day", "Day Time") {
        @Override
        public boolean isAllowed(Level world, BlockPos pos) {
            return (world.getOverworldClockTime() % 24000L) < 12000L;
        }
    },
    MOUNTAIN_BIOME("mountain_biome", "Mountain Biome") {
        @Override
        public boolean isAllowed(Level world, BlockPos pos) {
            return world.getBiome(pos).is(BiomeTags.IS_MOUNTAIN);
        }
    },
    SWAMP_BIOME("swamp_biome", "Swamp Biome") {
        @Override
        public boolean isAllowed(Level world, BlockPos pos) {
            return world.getBiome(pos).is(BiomeTags.HAS_SWAMP_HUT);
        }
    },
    COLD_BIOME("cold_biome", "Cold Biome") {
        @Override
        public boolean isAllowed(Level world, BlockPos pos) {
            return world.getBiome(pos).value().getBaseTemperature() <= 0.15F;
        }
    },
    HOT_BIOME("hot_biome", "Hot Biome") {
        @Override
        public boolean isAllowed(Level world, BlockPos pos) {
            return world.getBiome(pos).value().getBaseTemperature() >= 0.95F;
        }
    };

    public String id;
    public String name;

    BonusRequirement(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public abstract boolean isAllowed(Level world, BlockPos pos);
}
