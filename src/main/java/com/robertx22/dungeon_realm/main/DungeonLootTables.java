package com.robertx22.dungeon_realm.main;

import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.forge_compat.data.LootTableSubProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public class DungeonLootTables {

    public static final Identifier TIER_1_DUNGEON_CHEST = Identifier.fromNamespaceAndPath(DungeonMain.MODID, "chests/tier_1_dungeon");
    public static final Identifier TIER_2_DUNGEON_CHEST = Identifier.fromNamespaceAndPath(DungeonMain.MODID, "chests/tier_2_dungeon");
    public static final Identifier TIER_3_DUNGEON_CHEST = Identifier.fromNamespaceAndPath(DungeonMain.MODID, "chests/tier_3_dungeon");
    public static final Identifier TIER_4_DUNGEON_CHEST = Identifier.fromNamespaceAndPath(DungeonMain.MODID, "chests/tier_4_dungeon");
    public static final Identifier TIER_5_DUNGEON_CHEST = Identifier.fromNamespaceAndPath(DungeonMain.MODID, "chests/tier_5_dungeon");


    public static Identifier randomMapLoot() {
        return RandomUtils.randomFromList(tieredMapLoot());
    }

    public static List<Identifier> tieredMapLoot() {
        return Arrays.asList(
                TIER_1_DUNGEON_CHEST,
                TIER_2_DUNGEON_CHEST,
                TIER_3_DUNGEON_CHEST,
                TIER_4_DUNGEON_CHEST,
                TIER_5_DUNGEON_CHEST
        );
    }

    public static class DungeonLootTableProvider implements LootTableSubProvider {

        @Override
        public void generate(BiConsumer<Identifier, LootTable.Builder> output) {


        }
    }

}
