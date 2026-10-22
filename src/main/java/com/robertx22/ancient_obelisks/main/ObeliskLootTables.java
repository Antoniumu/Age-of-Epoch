package com.robertx22.ancient_obelisks.main;

import com.robertx22.forge_compat.data.LootTableSubProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.function.BiConsumer;

public class ObeliskLootTables {

    public static Identifier LOOT = ObelisksMain.id("obelisk_loot");
  

    public static class ObeliskLootTableProvider implements LootTableSubProvider {

        @Override
        public void generate(BiConsumer<Identifier, LootTable.Builder> output) {

            var essB = LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(3, 6))
                            .add(LootItem.lootTableItem(ObeliskEntries.ENVY.get()))
                            .add(LootItem.lootTableItem(ObeliskEntries.WRATH.get()))
                            .add(LootItem.lootTableItem(ObeliskEntries.GREED.get()))
                    ).withPool(
                            LootPool.lootPool().setRolls(ContextIntProviders.between(3, 6))
                                    .add(LootItem.lootTableItem(Items.DIAMOND))
                                    .add(LootItem.lootTableItem(Items.GOLD_INGOT))
                                    .add(LootItem.lootTableItem(Items.IRON_INGOT))
                                    .add(LootItem.lootTableItem(Items.LAPIS_LAZULI))
                                    .add(LootItem.lootTableItem(Items.REDSTONE))
                                    .add(LootItem.lootTableItem(Items.COAL))
                                    .add(LootItem.lootTableItem(Items.COPPER_INGOT))
                    );

            output.accept(LOOT, essB);


        }
    }

}
