package com.robertx22.the_harvest.main;

import com.robertx22.forge_compat.data.LootTableSubProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class HarvestLootTables {

    public static Identifier LOOT = HarvestMain.id("harvest_loot");


    public static class Provider implements LootTableSubProvider {

        @Override
        public void generate(BiConsumer<Identifier, LootTable.Builder> output) {

            var essB = LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 2))
                            .add(LootItem.lootTableItem(HarvestEntries.GREEN.get()))
                            .add(LootItem.lootTableItem(HarvestEntries.BLUE.get()))
                            .add(LootItem.lootTableItem(HarvestEntries.PURPLE.get()))
                    ).withPool(
                            LootPool.lootPool().setRolls(UniformGenerator.between(2, 4))
                                    .add(LootItem.lootTableItem(Items.WHEAT))
                                    .add(LootItem.lootTableItem(Items.BEETROOT))
                                    .add(LootItem.lootTableItem(Items.MELON))
                    );

            output.accept(LOOT, essB);


        }
    }

}
