package com.robertx22.infinite_dungeons.data_gen.builders;

import com.robertx22.infinite_dungeons.database.CompletitionScore;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonLayout;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonSpawn;
import com.robertx22.infinite_dungeons.database.db_types.layout.ScoreConfig;
import com.robertx22.infinite_dungeons.main.MainID;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class DungeonLayoutBuilder {

    DungeonLayout layout = new DungeonLayout();

    public static DungeonLayoutBuilder of(String id, String type, String group) {

        DungeonLayoutBuilder b = new DungeonLayoutBuilder();

        b.layout.id = id;
        b.layout.dungeon_type = type;
        b.layout.dungeon_group = group;

        // todo
        b.layout.structure_file = ResourceLocation.fromNamespaceAndPath(MainID.MODID, "dungeons/" + id).toString();

        return b;

    }

    public DungeonLayoutBuilder addSpawn(EntityType<?> type, int weight) {
        layout.spawns.add(new DungeonSpawn(type, weight, 0));
        return this;
    }

    public DungeonLayoutBuilder edit(Consumer<DungeonLayout> cons) {
        cons.accept(layout);
        return this;
    }

    public DungeonLayout build() {
        if (layout.score_configs.isEmpty()) {
            layout.score_configs.put(CompletitionScore.GOLD, ScoreConfig.defaultGold(LootTables.END_CITY_TREASURE));
            layout.score_configs.put(CompletitionScore.SILVER, ScoreConfig.defaultSilver(LootTables.UNDERWATER_RUIN_BIG));
            layout.score_configs.put(CompletitionScore.BRONZE, ScoreConfig.defaultBronze(LootTables.VILLAGE_PLAINS_HOUSE));
        }
        layout.addToSerializables();
        return layout;
    }

}
