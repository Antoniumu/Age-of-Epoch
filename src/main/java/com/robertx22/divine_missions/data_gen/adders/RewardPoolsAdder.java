package com.robertx22.divine_missions.data_gen.adders;

import com.robertx22.divine_missions.data_gen.builders.PoolBuilder;
import com.robertx22.divine_missions.database.db_types.Pool;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

import java.util.Arrays;

public class RewardPoolsAdder implements ExileRegistryInit {
    public static String GENERAL_REWARDS = "general_rewards";
    public static String HARVEST_REWARDS = "harvest_rewards";
    public static String OCEAN_REWARDS = "ocean_rewards";
    public static String WISDOM_REWARDS = "wisdom_rewards";
    public static String HUNT_REWARDS = "hunt_rewards";
    public static String FORGE_REWARDS = "forge_rewards";

    @Override
    public void registerAll() {

        PoolBuilder.of(FORGE_REWARDS, 1000, Pool.PoolType.REWARDS, Pool.PickType.ALWAYS_INCLUDE,
                Arrays.asList(
                    RewardsAdder.DIAMONDS,
                    RewardsAdder.IRON,
                    RewardsAdder.ENCHANTED_DIAMOND_PICKAXE,
                    RewardsAdder.ENCHANTED_DIAMOND_AXE
                )
            )
            .buildForGod(GodsAdder.FORGE);

        PoolBuilder.of(HUNT_REWARDS, 1000, Pool.PoolType.REWARDS, Pool.PickType.ALWAYS_INCLUDE,
                Arrays.asList(
                    RewardsAdder.LEATHER
                )
            )
            .buildForGod(GodsAdder.HUNT);

        PoolBuilder.of(HARVEST_REWARDS, 1000, Pool.PoolType.REWARDS, Pool.PickType.ALWAYS_INCLUDE,
                Arrays.asList(
                    RewardsAdder.GOLD_APPLE
                )
            )
            .buildForGod(GodsAdder.HARVEST);

        PoolBuilder.of(OCEAN_REWARDS, 1000, Pool.PoolType.REWARDS, Pool.PickType.ALWAYS_INCLUDE,
                Arrays.asList(
                    RewardsAdder.ENCHANTED_FISHING_ROD_LOOT_TABLE
                )
            )
            .buildForGod(GodsAdder.OCEAN);

        PoolBuilder.of(WISDOM_REWARDS, 1000, Pool.PoolType.REWARDS, Pool.PickType.ALWAYS_INCLUDE,
                Arrays.asList(
                    RewardsAdder.BIG_EXP,
                    RewardsAdder.ENCHANTED_BOOK_LOOT_TABLE
                )
            )
            .buildForGod(GodsAdder.WISDOM);

        PoolBuilder.of(GENERAL_REWARDS, 1000, Pool.PoolType.REWARDS, Pool.PickType.PICK_ONE,
                Arrays.asList(
                    RewardsAdder.BURIED_TREASURE_LOOT_TABLE,
                    RewardsAdder.END_CITY_CHEST_LOOT_TABLE,
                    RewardsAdder.DUNGEON_CHEST_LOOT_TABLE,
                    RewardsAdder.EXPERIENCE,
                    RewardsAdder.EMERALD,
                    RewardsAdder.DIAMONDS,
                    RewardsAdder.IRON,
                    RewardsAdder.ENDER_PEARL,
                    RewardsAdder.GOLD
                )
            )
            .buildForGods();
    }
}
