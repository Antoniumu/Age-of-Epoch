package com.robertx22.age_of_exile.mmorpg.registers.common;

import com.robertx22.age_of_exile.dimension.spawner.ModSpawnerBlockEntity;
import com.robertx22.age_of_exile.dimension.teleporter.TeleportedBlockEntity;
import com.robertx22.age_of_exile.dimension.teleporter.portal_block.PortalBlockEntity;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station.RuneWordStationTile;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SlashBlockEntities {

    public static void init() {

    }

    public static RegObj<BlockEntityType<RuneWordStationTile>> RUNEWORD = Def.blockEntity("runeword", () -> BlockEntityType.Builder.of(RuneWordStationTile::new, SlashBlocks.RUNEWORD.get())
        .build(null));
    public static RegObj<BlockEntityType<TeleportedBlockEntity>> TELEPORTER = Def.blockEntity("teleporter", () -> BlockEntityType.Builder.of(TeleportedBlockEntity::new, SlashBlocks.TELEPORTER.get())
        .build(null));
    public static RegObj<BlockEntityType<PortalBlockEntity>> PORTAL = Def.blockEntity("portal", () -> BlockEntityType.Builder.of(PortalBlockEntity::new, SlashBlocks.PORTAL.get())
        .build(null));
    public static RegObj<BlockEntityType<ModSpawnerBlockEntity>> SPAWNER = Def.blockEntity("spawner", () -> BlockEntityType.Builder.of(ModSpawnerBlockEntity::new, SlashBlocks.SPAWNER.get())
        .build(null));

}
