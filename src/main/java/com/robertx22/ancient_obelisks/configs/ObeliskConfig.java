package com.robertx22.ancient_obelisks.configs;

import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ObeliskConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ObeliskConfig CONFIG;

    static {
        final Pair<ObeliskConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ObeliskConfig::new);
        SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }

    // todo these could be reused for other mods maybe
    public ForgeConfigSpec.IntValue MAX_OBELISK_TIER;
    public ForgeConfigSpec.IntValue MAX_CHEST_REWARDS;
    public ForgeConfigSpec.DoubleValue MOB_HP_PER_TIER;
    public ForgeConfigSpec.DoubleValue MOB_DMG_PER_TIER;
    public ForgeConfigSpec.DoubleValue LOOT_MULTI_PER_TIER;
    public ForgeConfigSpec.DoubleValue LOOT_MULTI_PER_AFFIX;
    public ForgeConfigSpec.DoubleValue LOOT_CHANCE_PER_MOB_KILL;
    public ForgeConfigSpec.DoubleValue OBELISK_SPAWN_CHANCE_ON_CHEST_LOOT;

    public ForgeConfigSpec.BooleanValue SKIP_COOLDOWN_IF_NO_MORE_MOBS;

    public ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_CHANCE_MULTI;


    public ForgeConfigSpec.IntValue WAVE_COOLDOWN_SECONDS;
    public ForgeConfigSpec.IntValue MOB_SPAWNS_PER_SECOND;
    public ForgeConfigSpec.IntValue MOB_SPAWN_CHANCE;
    public ForgeConfigSpec.IntValue TOTAL_MOBS_PER_WAVE;


    public static ObeliskConfig get() {
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

    ObeliskConfig(ForgeConfigSpec.Builder b) {
        b.comment("Ancient Obelisk Configs")
                .push("general");

        DIMENSION_CHANCE_MULTI = b
                .comment("Obelisk spawn chance multi per dimension")
                .defineList("DIMENSION_CHANCE_MULTI", () -> Arrays.asList("mmorpg:dungeon-0"), x -> true);

        WAVE_COOLDOWN_SECONDS = b
                .comment("When new wave starts, this cooldown has to pass before a new wave can start")
                .defineInRange("WAVE_COOLDOWN_SECONDS", 60, 1, 1000);

        MOB_SPAWNS_PER_SECOND = b
                .defineInRange("MOB_SPAWNS_PER_SECOND", 1, 1, 50);

        MOB_SPAWN_CHANCE = b.comment("You can use low mob spawn chance with high mob spawns config to more randomly spawn mobs, or keep chance high and spawn count low to spawn them consistently over time.\n" +
                        "Do note, there's usually around 4 spawners in the arena")
                .defineInRange("MOB_SPAWN_CHANCE", 10, 1, 100);

        TOTAL_MOBS_PER_WAVE = b.comment("Each wave will have this many mobs. When the mobs are done spawning, the new wave will start, unless wave cooldown is there.")
                .defineInRange("TOTAL_MOBS_PER_WAVE", 15, 1, 100);

        MAX_OBELISK_TIER = b
                .comment("Each obelisk tier increases the Mob Stats and rewards")
                .defineInRange("MAX_OBELISK_TIER", 10, 1, 1000);

        MAX_CHEST_REWARDS = b
                .comment("Maximum amount of chests that can spawn at end of obelisk fight\n" +
                        "Useful as a safety net in case you set your loot multipliers too high so you don't spawn.. 1000 chests accidentally")
                .defineInRange("MAX_CHEST_REWARDS", 10, 1, 1000);

        MOB_HP_PER_TIER = b
                .comment("Mob hp multiplier per obelisk tier")
                .defineInRange("MOB_HP_PER_TIER", 0.2F, 0, 1000);

        MOB_DMG_PER_TIER = b
                .comment("Mob attack damage multiplier per obelisk tier")
                .defineInRange("MOB_DMG_PER_TIER", 0.05F, 0, 1000);

        LOOT_MULTI_PER_TIER = b
                .comment("Loot Multi x tier for obelisk end of fight reward")
                .defineInRange("LOOT_MULTI_PER_TIER", 0.05F, 0, 1);

        LOOT_MULTI_PER_AFFIX = b
                .comment("Loot Multi x tier for obelisk end of fight reward")
                .defineInRange("LOOT_MULTI_PER_AFFIX", 0.1F, 0, 1);

        LOOT_CHANCE_PER_MOB_KILL = b
                .comment("Every mob you kill inside the obelisk will add x chance to spawn loot chests at the end.\n" +
                        "If the total is say 50, it means 50% chance to spawn a chest. If it's 150, then it's 1 chest + 50% chance for another.\n" +
                        "Make sure this value isn't too big because this chance is multiplied by tier and affix counts")
                .defineInRange("LOOT_CHANCE_PER_MOB_KILL", 2F, 0, 100);

        OBELISK_SPAWN_CHANCE_ON_CHEST_LOOT = b
                .comment("When you loot new chests with loot tables, obelisk maps have a chance to spawn as extra loot")
                .defineInRange("OBELISK_SPAWN_CHANCE_ON_CHEST_LOOT", 5F, 0, 100);

        SKIP_COOLDOWN_IF_NO_MORE_MOBS = b
                .comment("Starts new wave right away if all mobs are dead")
                .define("SKIP_COOLDOWN_IF_NO_MORE_MOBS", true);

        b.pop();
    }


}
