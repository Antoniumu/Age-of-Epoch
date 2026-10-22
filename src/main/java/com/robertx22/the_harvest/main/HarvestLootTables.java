package com.robertx22.the_harvest.main;

import com.robertx22.forge_compat.data.LootTableSubProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;

import java.util.function.BiConsumer;

public class HarvestLootTables {

    public static Identifier LOOT = HarvestMain.id("harvest_loot");

    private static LootPool.Builder seedPool() {
        return LootPool.lootPool()
                .add(LootItem.lootTableItem(HarvestEntries.GREEN.get()))
                .add(LootItem.lootTableItem(HarvestEntries.BLUE.get()))
                .add(LootItem.lootTableItem(HarvestEntries.PURPLE.get()));
    }

    private static LootPool.Builder cropPool() {
        return LootPool.lootPool()
                .add(LootItem.lootTableItem(Items.WHEAT))
                .add(LootItem.lootTableItem(Items.BEETROOT))
                .add(LootItem.lootTableItem(Items.MELON));
    }

    public static class Provider implements LootTableSubProvider {
        @Override
        public void generate(BiConsumer<Identifier, LootTable.Builder> output) {
            var essB = LootTable.lootTable()
                    .withPool(seedPool())
                    .withPool(cropPool())
                    .withPool(cropPool());
            output.accept(LOOT, essB);
        }
    }
}
