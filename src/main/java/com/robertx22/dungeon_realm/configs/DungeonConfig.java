package com.robertx22.dungeon_realm.configs;

import com.robertx22.dungeon_realm.main.DungeonMain;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DungeonConfig {

    public static final ForgeConfigSpec SPEC;
    public static final DungeonConfig CONFIG;

    static {
        final Pair<DungeonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(DungeonConfig::new);
        SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }


    public ForgeConfigSpec.IntValue MIN_MAP_ROOMS;
    public ForgeConfigSpec.IntValue MAX_MAP_ROOMS;

    public ForgeConfigSpec.DoubleValue DUNGEON_MAP_SPAWN_CHANCE_ON_CHEST_LOOT;
    public ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_CHANCE_MULTI;

    public ForgeConfigSpec.IntValue MOB_MIN;
    public ForgeConfigSpec.IntValue MOB_MAX;

    public ForgeConfigSpec.IntValue PACK_MOB_MIN;
    public ForgeConfigSpec.IntValue PACK_MOB_MAX;

    public ForgeConfigSpec.DoubleValue UBER_FRAG_DROPRATE;

    public ForgeConfigSpec.IntValue ELITE_MOB_COMPLETION_WEIGHT;
    public ForgeConfigSpec.IntValue MINI_BOSS_COMPLETION_WEIGHT;

    public ForgeConfigSpec.IntValue KILL_COMPLETION_DATA_BLOCK_LEEWAY;

    public static DungeonConfig get() {
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

    DungeonConfig(ForgeConfigSpec.Builder b) {
        b.comment("Dungeon Realm Configs")
                .push("general");

        DIMENSION_CHANCE_MULTI = b
                .comment("Dungeon map spawn chance multi per dimension")
                .defineList("DIMENSION_CHANCE_MULTI", () -> Arrays.asList(DungeonMain.DIMENSION_ID + "-1.5"), x -> true);


        DUNGEON_MAP_SPAWN_CHANCE_ON_CHEST_LOOT = b
                .comment("Dungeon Maps have a chance to spawn when you loot chests outside of map dimensions")
                .defineInRange("DUNGEON_MAP_SPAWN_CHANCE_ON_CHEST_LOOT", 5F, 0, 100);

        MIN_MAP_ROOMS = b.defineInRange("MIN_MAP_ROOMS", 12, 1, 100);
        MAX_MAP_ROOMS = b.defineInRange("MAX_MAP_ROOMS", 20, 1, 100);

        MOB_MIN = b.defineInRange("mob_min", 1, 0, 20);
        MOB_MAX = b.defineInRange("mob_max", 2, 0, 20);

        PACK_MOB_MIN = b.defineInRange("pack_mob_min", 3, 0, 20);
        PACK_MOB_MAX = b.defineInRange("pack_mob_max", 6, 0, 20);

        UBER_FRAG_DROPRATE = b.comment("Uber fragments can drop from A map boss, default chance is 10%").defineInRange("UBER_FRAG_DROP_RATE", 10D, 0, 100);

        ELITE_MOB_COMPLETION_WEIGHT = b.comment("How many mobs should an elite mob count for in terms of map completion? Default is 10, so 10x a normal mob death. Max 9999")
                                       .defineInRange("elite_mob_completion_weight", 10, 1, 9999);

        MINI_BOSS_COMPLETION_WEIGHT = b.comment("How many mobs should a miniboss mob count for in terms of map completion? Default is 20, so 20x a normal mob death. Max 9999")
                                       .defineInRange("mini_boss_completion_weight", 20, 1, 9999);

        KILL_COMPLETION_DATA_BLOCK_LEEWAY = b.comment("Number of data blocks allowed to be potentially miscounted to transition to accurate kill counts. Defaults to 2, between 0-20.")
                .comment("For example, if you have 20 data blocks, and the player has come into range of 18, with a leeway of 2, this means 18 + 2 >= 20, so assume all mobs have been spawned.")
                .comment("This matters *mostly* for the kill completion assumptions. Until all mobs are spawned, it assumes remaining data blocks (command blocks or structure blocks)")
                .comment("will spawn either the MOB_MAX or PACK_MOB_MAX number of mobs. When you cross the threshold + leeway, it assumes all mobs have been spawned, so the")
                .comment("denominator for the calculation shrinks, and kill % goes up. 2 seems to be roughly the right number from testing, but if this proves to be insanely accurate")
                .comment("feel free to crank this back down to 1 or 0.")
                        .defineInRange("kill_completion_data_block_leeway", 2, 0, 20);

        b.pop();
    }


}
