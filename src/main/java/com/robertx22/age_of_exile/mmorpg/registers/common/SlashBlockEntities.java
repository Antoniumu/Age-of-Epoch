package com.robertx22.age_of_exile.mmorpg.registers.common;

import com.robertx22.age_of_exile.dimension.spawner.ModSpawnerBlockEntity;
import com.robertx22.world_of_exile.world_gen.TowerBossMarkerBlockEntity;
import com.robertx22.age_of_exile.dimension.teleporter.TeleportedBlockEntity;
import com.robertx22.age_of_exile.dimension.teleporter.portal_block.PortalBlockEntity;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station.RuneWordStationTile;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SlashBlockEntities {

    public static void init() {

    }

    public static RegObj<BlockEntityType<RuneWordStationTile>> RUNEWORD = Def.blockEntity("runeword",
        () -> FabricBlockEntityTypeBuilder.create(RuneWordStationTile::new, SlashBlocks.RUNEWORD.get()).build());
    public static RegObj<BlockEntityType<TeleportedBlockEntity>> TELEPORTER = Def.blockEntity("teleporter",
        () -> FabricBlockEntityTypeBuilder.create(TeleportedBlockEntity::new, SlashBlocks.TELEPORTER.get()).build());
    public static RegObj<BlockEntityType<PortalBlockEntity>> PORTAL = Def.blockEntity("portal",
        () -> FabricBlockEntityTypeBuilder.create(PortalBlockEntity::new, SlashBlocks.PORTAL.get()).build());
    public static RegObj<BlockEntityType<ModSpawnerBlockEntity>> SPAWNER = Def.blockEntity("spawner",
        () -> FabricBlockEntityTypeBuilder.create(ModSpawnerBlockEntity::new, SlashBlocks.SPAWNER.get()).build());
    public static RegObj<BlockEntityType<TowerBossMarkerBlockEntity>> TOWER_BOSS_MARKER = Def.blockEntity("tower_boss_marker",
        () -> FabricBlockEntityTypeBuilder.create(TowerBossMarkerBlockEntity::new, SlashBlocks.TOWER_BOSS_MARKER.get()).build());

    public static RegObj<BlockEntityType<com.robertx22.world_of_exile.delve.DelveMarkerBlockEntity>> DELVE_MARKER = Def.blockEntity("delve_marker",
        () -> FabricBlockEntityTypeBuilder.create(com.robertx22.world_of_exile.delve.DelveMarkerBlockEntity::new, SlashBlocks.DELVE_MARKER.get()).build());

}
