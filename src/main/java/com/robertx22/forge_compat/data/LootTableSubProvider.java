package com.robertx22.forge_compat.data;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootTable;
import java.util.function.BiConsumer;

public interface LootTableSubProvider {
    void generate(BiConsumer<Identifier, LootTable.Builder> output);
}
