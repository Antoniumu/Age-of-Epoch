package com.robertx22.dungeon_realm.api;

import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.events.base.ExileEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class PrepareDungeonMobEditsEvent extends ExileEvent {

    public LivingEntity mob;

    public Optional<MapDataBlock> dataBlock;

    public List<Consumer<LivingEntity>> edits = new ArrayList<>();

    public PrepareDungeonMobEditsEvent(LivingEntity mob, Optional<MapDataBlock> dataBlock) {
        this.mob = mob;
        this.dataBlock = dataBlock;
    }


}
