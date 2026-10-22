package com.robertx22.mine_and_slash.mmorpg.registers.common;

import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.mine_and_slash.database.data.profession.ProfessionBlock;
import com.robertx22.mine_and_slash.database.data.profession.ProfessionBlockEntity;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.world_of_exile.delve.DelveMarkerBlockEntity;
import com.robertx22.world_of_exile.world_gen.TowerBossMarkerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.stream.Collectors;

public class SlashBlockEntities {

    public static void init() {
    }

    public static RegObj<BlockEntityType<ProfessionBlockEntity>> PROFESSION = Def.blockEntity("profession", () -> {
        SlashBlocks.ensureStations();
        return FabricBlockEntityTypeBuilder.create(
                ProfessionBlockEntity::new,
                SlashBlocks.STATIONS.values().stream().map(RegObj::get).collect(Collectors.toList()).toArray(new ProfessionBlock[0])
        ).build();
    });

    public static RegObj<BlockEntityType<TowerBossMarkerBlockEntity>> TOWER_BOSS_MARKER = Def.blockEntity("tower_boss_marker", () ->
        FabricBlockEntityTypeBuilder.create(TowerBossMarkerBlockEntity::new, SlashBlocks.TOWER_BOSS_MARKER.get()).build()
    );

    public static RegObj<BlockEntityType<DelveMarkerBlockEntity>> DELVE_MARKER = Def.blockEntity("delve_marker", () ->
        FabricBlockEntityTypeBuilder.create(DelveMarkerBlockEntity::new, SlashBlocks.DELVE_MARKER.get()).build()
    );
}
