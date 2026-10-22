package com.robertx22.ancient_obelisks.main;

import com.robertx22.forge_compat.data.LootTableSubProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;

import java.util.function.BiConsumer;

public class ObeliskLootTables {

    public static Identifier LOOT = ObelisksMain.id("obelisk_loot");

    private static LootPool.Builder essencePool() {
        return LootPool.lootPool()
                .add(LootItem.lootTableItem(ObeliskEntries.ENVY.get()))
                .add(LootItem.lootTableItem(ObeliskEntries.WRATH.get()))
                .add(LootItem.lootTableItem(ObeliskEntries.GREED.get()));
    }

    private static LootPool.Builder orePool() {
        return LootPool.lootPool()
                .add(LootItem.lootTableItem(Items.DIAMOND))
                .add(LootItem.lootTableItem(Items.GOLD_INGOT))
                .add(LootItem.lootTableItem(Items.IRON_INGOT))
                .add(LootItem.lootTableItem(Items.LAPIS_LAZULI))
                .add(LootItem.lootTableItem(Items.REDSTONE))
                .add(LootItem.lootTableItem(Items.COAL))
                .add(LootItem.lootTableItem(Items.COPPER_INGOT));
    }

    public static class ObeliskLootTableProvider implements LootTableSubProvider {
        @Override
        public void generate(BiConsumer<Identifier, LootTable.Builder> output) {
            var essB = LootTable.lootTable()
                    .withPool(essencePool())
                    .withPool(essencePool())
                    .withPool(essencePool())
                    .withPool(orePool())
                    .withPool(orePool())
                    .withPool(orePool());
            output.accept(LOOT, essB);
        }
    }
}
