package com.robertx22.the_harvest.configs;

import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class HarvestConfig {

    public static final ForgeConfigSpec SPEC;
    public static final HarvestConfig CONFIG;

    static {
        final Pair<HarvestConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(HarvestConfig::new);
        SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }


    public ForgeConfigSpec.DoubleValue MAP_SPAWN_CHANCE_ON_CHEST_LOOT;

    public ForgeConfigSpec.DoubleValue LOOT_TABLE_CHANCE_PER_MOB;

    public ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_CHANCE_MULTI;


    public ForgeConfigSpec.IntValue MAX_TOTAL_MOB_SPAWNS;


    public static HarvestConfig get() {
        return CONFIG;
    }


    public HashMap<String, Float> dimChanceMap = new HashMap<>();

    public HashMap<String, Float> getDimChanceMap() {
        if (dimChanceMap.isEmpty()) {
            for (String s : DIMENSION_CHANCE_MULTI.get()) {
                String dim = s.split("-")[0];
                Float multi = Float.parseFloat(s.split("-")[1]);
                dimChanceMap.put(dim, multi);
            }
        }

        return dimChanceMap;
    }


    public float getDimChanceMulti(Level level) {
        String dimid = level.dimension().identifier().toString();
        var map = getDimChanceMap();
        return map.getOrDefault(dimid, 1F);
    }

    HarvestConfig(ForgeConfigSpec.Builder b) {
        b.comment("The Harvest Configs")
                .push("general");

        DIMENSION_CHANCE_MULTI = b
                .comment("Harvest map spawn chance multi per dimension")
                .defineList("DIMENSION_CHANCE_MULTI", () -> Arrays.asList("mmorpg:dungeon-0"), x -> true);

        MAX_TOTAL_MOB_SPAWNS = b.comment("Max mobs spawned in a harvest arena")
                .defineInRange("MAX_TOTAL_MOB_SPAWNS", 200, 20, 2000);


        MAP_SPAWN_CHANCE_ON_CHEST_LOOT = b
                .comment("When you loot new chests with loot tables, harvest maps have a chance to spawn as extra loot")
                .defineInRange("MAP_SPAWN_CHANCE_ON_CHEST_LOOT", 5F, 0, 100);

        LOOT_TABLE_CHANCE_PER_MOB = b
                .comment("Mobs inside the harvest have a chance to drop this loot table as a bonus")
                .defineInRange("LOOT_TABLE_CHANCE_PER_MOB", 5F, 0, 100);


        b.pop();
    }


}
