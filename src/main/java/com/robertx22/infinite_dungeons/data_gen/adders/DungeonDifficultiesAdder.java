package com.robertx22.infinite_dungeons.data_gen.adders;

import com.robertx22.infinite_dungeons.data_gen.builders.DifficultyBuilder;
import com.robertx22.infinite_dungeons.database.ids.DungeonDifficultiesIds;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class DungeonDifficultiesAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {

        DifficultyBuilder.of(DungeonDifficultiesIds.DIFF_0, 0, 1, 1, 0, -15)
            .build();
        DifficultyBuilder.of(DungeonDifficultiesIds.DIFF_1, 1, 1.25F, 1.25F, 0, -10)
            .build();
        DifficultyBuilder.of(DungeonDifficultiesIds.DIFF_2, 2, 1.5F, 1.5F, 0, -5)
            .build();
        DifficultyBuilder.of(DungeonDifficultiesIds.DIFF_3, 3, 1.75F, 1.75F, 0.2F, 0)
            .build();
        DifficultyBuilder.of(DungeonDifficultiesIds.DIFF_4, 4, 2F, 2F, 0.4F, 5)
            .build();
        DifficultyBuilder.of(DungeonDifficultiesIds.DIFF_5, 5, 2.5F, 2.5F, 0.6F, 10)
            .build();
        DifficultyBuilder.of(DungeonDifficultiesIds.DIFF_6, 6, 3F, 3F, 0.8F, 15)
            .build();

    }
}
