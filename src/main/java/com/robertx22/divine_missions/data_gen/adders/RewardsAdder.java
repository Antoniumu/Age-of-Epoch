package com.robertx22.divine_missions.data_gen.adders;

import com.robertx22.divine_missions.data_gen.builders.RewardBuilder;
import com.robertx22.divine_missions.database.db_types.Reward;
import com.robertx22.divine_missions.main.DivineMissions;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTables;

public class RewardsAdder {

    public static Reward DIAMONDS = RewardBuilder.item(500)
        .build(Items.DIAMOND, 1, 3);
    public static Reward IRON = RewardBuilder.item(100)
        .build(Items.IRON_INGOT, 3, 5);
    public static Reward GOLD = RewardBuilder.item(150)
        .build(Items.GOLD_INGOT, 2, 4);
    public static Reward ENDER_PEARL = RewardBuilder.item(300)
        .build(Items.ENDER_PEARL, 1, 3);
    public static Reward GOLD_APPLE = RewardBuilder.item(500)
        .build(Items.GOLDEN_APPLE, 1, 2);
    public static Reward EMERALD = RewardBuilder.item(250)
        .build(Items.EMERALD, 2, 6);
    public static Reward LEATHER = RewardBuilder.item(250)
        .build(Items.LEATHER, 2, 6);

    public static Reward EXPERIENCE = RewardBuilder.exp(2, "exp", 1, 1000);
    public static Reward BIG_EXP = RewardBuilder.exp(1, "big_exp", 200, 1500);

    public static Reward ENCHANTED_BOOK_LOOT_TABLE = RewardBuilder.lootTable(1000, 100, DivineMissions.id("enchanted_book"))
        .build();
    public static Reward ENCHANTED_DIAMOND_PICKAXE = RewardBuilder.lootTable(1500, 50, DivineMissions.id("enchanted_diamond_pickaxe"))
        .build();
    public static Reward ENCHANTED_DIAMOND_AXE = RewardBuilder.lootTable(1500, 50, DivineMissions.id("enchanted_diamond_axe"))
        .build();

    public static Reward ENCHANTED_FISHING_ROD_LOOT_TABLE = RewardBuilder.lootTable(1000, 100, DivineMissions.id("enchanted_rod"))
        .build();
    public static Reward DUNGEON_CHEST_LOOT_TABLE = RewardBuilder.lootTable(1000, 50, LootTables.SIMPLE_DUNGEON)
        .build();
    public static Reward BURIED_TREASURE_LOOT_TABLE = RewardBuilder.lootTable(2000, 25, LootTables.BURIED_TREASURE)
        .build();
    public static Reward END_CITY_CHEST_LOOT_TABLE = RewardBuilder.lootTable(2500, 15, LootTables.END_CITY_TREASURE)
        .build();

    public static void init() {

    }
}
