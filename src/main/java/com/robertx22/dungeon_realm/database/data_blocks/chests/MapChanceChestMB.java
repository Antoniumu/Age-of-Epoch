package com.robertx22.dungeon_realm.database.data_blocks.chests;

import com.robertx22.dungeon_realm.database.holders.DungeonMapBlocks;
import com.robertx22.library_of_exile.database.map_data_block.MapBlockCtx;
import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.util.wiki.WikiEntry;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class MapChanceChestMB extends MapDataBlock {

    public MapChanceChestMB(String id) {
        super(id, id);
        this.aliases.add("chest_chance");

    }

    @Override
    public Class<?> getClassForSerialization() {
        return MapChanceChestMB.class;
    }

    @Override
    public void processImplementationINTERNAL(String key, BlockPos pos, Level world, CompoundTag nbt, MapBlockCtx ctx) {

        if (!nbt.getBooleanOr("chance_chest", false) && RandomUtils.roll(25)) {
            nbt.putBoolean("chance_chest", true);
            DungeonMapBlocks.INSTANCE.MAP_CHEST.get().process(key, pos, world, nbt, ctx);
        } else {
            world.removeBlock(pos, false);
            //world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public WikiEntry getWikiEntry() {
        return WikiEntry.of("Has 25% chance to spawn the chest, but only spawns it once inside a single room. Use this to randomize chest positions easily by placing a few of these in different spots");
    }
}
