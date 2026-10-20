package com.robertx22.divine_missions.data_gen.adders;

import com.robertx22.divine_missions.data_gen.builders.PoolBuilder;
import com.robertx22.divine_missions.database.condition_types.IsModLoaded;
import com.robertx22.divine_missions.database.db_types.Pool;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

import java.util.Arrays;

public class TaskPoolsAdder implements ExileRegistryInit {

    public static String AGE_OF_EXILE_GENERAL_TASKS = "general_aoe_tasks";

    public static String GENERAL_TASKS = "general_tasks";
    public static String HARVEST_TASKS = "harvest_tasks";
    public static String WISDOM_TASKS = "wisdom_tasks";
    public static String HUNT_TASKS = "hunt_tasks";
    public static String OCEAN_TASKS = "ocean_tasks";

    @Override
    public void registerAll() {

        PoolBuilder.of(AGE_OF_EXILE_GENERAL_TASKS, 1000, Pool.PoolType.TASKS, Pool.PickType.PICK_ONE,
                Arrays.asList(
                    TaskEntryAdder.LOOT_LOTS_OF_CHESTS,
                    TaskEntryAdder.LOOT_A_CHEST,
                    TaskEntryAdder.KILL_ANY
                )
            )
            .addCondition(IsModLoaded.of("mmorpg"))
            .buildForGods();

        PoolBuilder.of(GENERAL_TASKS, 1000, Pool.PoolType.TASKS, Pool.PickType.PICK_ONE,
                Arrays.asList(
                    TaskEntryAdder.KILL_ANY,
                    TaskEntryAdder.LOOT_A_CHEST,

                    TaskEntryAdder.GET_DIAMONDS,
                    TaskEntryAdder.GET_IRON,
                    TaskEntryAdder.GET_GOLD,
                    TaskEntryAdder.GET_LAPIS,

                    TaskEntryAdder.KILL_SPIDER,
                    TaskEntryAdder.KILL_ENDERMAN,
                    TaskEntryAdder.KILL_ZOMBIE,
                    TaskEntryAdder.KILL_WITHER
                )
            )
            .buildForGods();

        PoolBuilder.of(HARVEST_TASKS, 1000, Pool.PoolType.TASKS, Pool.PickType.ALWAYS_INCLUDE,
                Arrays.asList(
                    TaskEntryAdder.GET_BEETROOT,
                    TaskEntryAdder.GET_PUMPKIN,
                    TaskEntryAdder.GET_COOKIES,
                    TaskEntryAdder.GET_CARROTS,
                    TaskEntryAdder.GET_HONEYCOMB,
                    TaskEntryAdder.GET_POTATOES,
                    TaskEntryAdder.GET_MELONS,
                    TaskEntryAdder.GET_CAKE,
                    TaskEntryAdder.GET_BREAD
                )
            )
            .buildForGod(GodsAdder.HARVEST);

        PoolBuilder.of(WISDOM_TASKS, 1000, Pool.PoolType.TASKS, Pool.PickType.ALWAYS_INCLUDE,
                Arrays.asList(
                    TaskEntryAdder.GET_WRITABLE_BOOK,
                    TaskEntryAdder.GET_PAPER,
                    TaskEntryAdder.GET_INK,
                    TaskEntryAdder.GET_BOOKS
                )
            )
            .buildForGod(GodsAdder.WISDOM);

        PoolBuilder.of(OCEAN_TASKS, 1000, Pool.PoolType.TASKS, Pool.PickType.ALWAYS_INCLUDE,
                Arrays.asList(
                    TaskEntryAdder.KILL_GUARDIAN,
                    TaskEntryAdder.GET_TURTLE_EGG,
                    TaskEntryAdder.GET_COD,
                    TaskEntryAdder.GET_PUFFER,
                    TaskEntryAdder.GET_TROPICAL,
                    TaskEntryAdder.GET_SALMON,
                    TaskEntryAdder.GET_NAUT_SHELL
                )
            )
            .buildForGod(GodsAdder.OCEAN);

        PoolBuilder.of(HUNT_TASKS, 1000, Pool.PoolType.TASKS, Pool.PickType.ALWAYS_INCLUDE,
                Arrays.asList(
                    TaskEntryAdder.GET_TOTEM_OF_UDNYING,
                    TaskEntryAdder.KILL_MANY,
                    TaskEntryAdder.KILL_GHAST,
                    TaskEntryAdder.KILL_SLIME,
                    TaskEntryAdder.KILL_WITCH,
                    TaskEntryAdder.KILL_PIGMAN,
                    TaskEntryAdder.KILL_PHANTOM,
                    TaskEntryAdder.KILL_WITHER_SKELE,
                    TaskEntryAdder.KILL_PILLAGER
                )
            )
            .buildForGod(GodsAdder.HUNT);
    }
}
