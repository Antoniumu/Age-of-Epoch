package com.robertx22.infinite_dungeons.database.db_types.layout;

import com.robertx22.infinite_dungeons.database.CompletitionScore;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.resources.Identifier;

public class ScoreConfig {

    public CompletitionScore score;
    public String loot_table = BuiltInLootTables.END_CITY_TREASURE.identifier().toString(); // TODO
    public int reward_time_seconds = 60 * 3;
    public float loot_multi = 3;

    public Identifier getLootTable() {
        return Identifier.parse(loot_table);
    }

    public static ScoreConfig defaultGold(Identifier lootTable) {
        ScoreConfig c = new ScoreConfig();
        c.score = CompletitionScore.GOLD;
        c.loot_table = lootTable.toString();
        c.reward_time_seconds = 60 * 3;
        c.loot_multi = 3;
        return c;
    }

    public static ScoreConfig defaultSilver(Identifier lootTable) {
        ScoreConfig c = new ScoreConfig();
        c.score = CompletitionScore.SILVER;
        c.loot_table = lootTable.toString();
        c.reward_time_seconds = 60 * 5;
        c.loot_multi = 2;
        return c;
    }

    public static ScoreConfig defaultBronze(Identifier lootTable) {
        ScoreConfig c = new ScoreConfig();
        c.score = CompletitionScore.BRONZE;
        c.loot_table = lootTable.toString();
        c.reward_time_seconds = 60 * 25;
        c.loot_multi = 1;
        return c;
    }

}
