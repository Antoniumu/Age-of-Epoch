package com.robertx22.age_of_exile.dimension;

import com.robertx22.age_of_exile.dimension.dungeon_data.DungeonData;
import com.robertx22.age_of_exile.dimension.dungeon_data.DungeonPopulateData;
import com.robertx22.age_of_exile.dimension.spawner.ModSpawnerBlockEntity;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.SignUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.server.level.ServerLevel;
public class PopulateDungeonChunks {

    public static void tryPopulateChunksAroundPlayer(Level world, Player player) {

        if (WorldUtils.isMapWorldClass(world)) {

            // Watch watch = new Watch();

            int populated = 0;
            List<ChunkPos> list = getChunksAround(ChunkPos.containing(player.blockPosition()));

            for (ChunkPos cp : list) {
                LevelChunk chunk = world.getChunk(cp.x(), cp.z());

                if (!Load.chunkPopulated(chunk).populated) {
                    DungeonData data = Load.dungeonData(world).data.get(chunk.getPos()
                        .getWorldPosition()).data;

                    if (!data.isEmpty()) {
                        PopulateDungeonChunks.populateChunk(world, chunk, data, new DungeonPopulateData());
                        Load.chunkPopulated(chunk).populated = true;
                        populated++;
                        if (populated > 5) {
                            //watch.print("Populating chunk around player ");
                            break;
                        }

                    }
                }
            }

        }

    }

    public static List<ChunkPos> getChunksAround(ChunkPos cp) {
        List<ChunkPos> list = new ArrayList<>();
        list.add(cp);

        list.add(new ChunkPos(cp.x() + 1, cp.z()));
        list.add(new ChunkPos(cp.x(), cp.z() + 1));

        list.add(new ChunkPos(cp.x() - 1, cp.z()));
        list.add(new ChunkPos(cp.x(), cp.z() - 1));

        list.add(new ChunkPos(cp.x() + 1, cp.z() + 1));
        list.add(new ChunkPos(cp.x() - 1, cp.z() - 1));

        list.add(new ChunkPos(cp.x() - 1, cp.z() + 1));
        list.add(new ChunkPos(cp.x() + 1, cp.z() - 1));

        return list;

    }

    public static void populate(Set<DungeonPopulateData.CP> toPopulate, Set<DungeonPopulateData.CP> populated, Level world, ChunkAccess chunk, DungeonData dungeon, DungeonPopulateData data) {

        boolean has = populateChunk(world, chunk, dungeon, data);

        if (has) {
            populated.add(new DungeonPopulateData.CP(chunk.getPos()));

            getChunksAround(chunk.getPos()).forEach(x -> toPopulate.add(new DungeonPopulateData.CP(x)));
        }
        toPopulate.removeIf(x -> x.getChunkPos()
            .equals(chunk.getPos()));
    }

    public static boolean populateChunk(Level world, ChunkAccess chunk, DungeonData dungeon, DungeonPopulateData data) {

        Set<BlockPos> list = new java.util.HashSet<>(chunk.getBlockEntitiesPos());

        boolean has = !list.isEmpty();

        for (BlockPos blockPos : list) {
            try {
                BlockEntity be = world.getBlockEntity(blockPos);
                if (be instanceof BeaconBlockEntity) {
                    populate(world, blockPos, dungeon, data);
                } else if (be instanceof SignBlockEntity) {
                    SignBlockEntity sign = (SignBlockEntity) be;
                    if (SignUtils.has("[chest]", sign)) {
                        setChest(world, blockPos);
                    } else if (SignUtils.has("[portal]", sign)) {
                        world.setBlockAndUpdate(blockPos, SlashBlocks.PORTAL.get()
                            .defaultBlockState());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return has;

    }

    static void populate(Level world, BlockPos pos, DungeonData dungeonData, DungeonPopulateData data) {

        data.pieces++;

        List<BlockPos> list = new ArrayList<>();
        for (int x = -5; x < 5; x++) {
            for (int z = -5; z < 5; z++) {
                list.add(pos.offset(x, 0, z));
            }
        }

        int chests = RandomUtils.roll(20) ? 1 : 0;
        int spawners = RandomUtils.roll(20) ? 1 : 0;
        int instantSpawners = RandomUtils.roll(10) ? 2 : 1;

        int tries = 0;

        /*
        for (int i = 0; i < mobs; i++) {
            BlockPos p = RandomUtils.randomFromList(list);

            tries++;
            if (tries > 50) {
                break;
            }

            EntityType type = dungeonData.getMobList()
                .getRandomMob();

            if (!SpawnUtil.canPlaceMob(world, type, p)) {
                i--;
                continue;
            }
            data.mobs++;

            dungeonData.getMobList()
                .spawMob((ServerLevel) world, type, p, dungeonData.t);

            list.remove(p);
        }

         */

        tries = 0;

        for (int i = 0; i < chests; i++) {
            BlockPos p = RandomUtils.randomFromList(list);
            tries++;
            if (tries > 50) {
                break;
            }

            if (!SpawnUtil.canPlaceBlock(world, p)) {
                i--;
                continue;
            }
            data.chests++;

            setChest(world, p);

            list.remove(p);
        }

        tries = 0;

        for (int i = 0; i < spawners; i++) {
            BlockPos p = RandomUtils.randomFromList(list);
            tries++;
            if (tries > 50) {
                break;
            }
            if (!SpawnUtil.canPlaceBlock(world, p)) {
                i--;
                continue;
            }
            data.mobs += ModSpawnerBlockEntity.DEFAULT_SPAWNS;

            world.setBlock(p, SlashBlocks.SPAWNER.get()
                .defaultBlockState(), 2);

            list.remove(p);
        }

        tries = 0;

        for (int i = 0; i < instantSpawners; i++) {
            BlockPos p = RandomUtils.randomFromList(list);
            tries++;
            if (tries > 50) {
                break;
            }
            if (!SpawnUtil.canPlaceBlock(world, p)) {
                i--;
                continue;
            }
            int mobs = RandomUtils.RandomRange(2, 5);

            data.mobs += mobs;

            world.setBlock(p, SlashBlocks.SPAWNER.get()
                .defaultBlockState(), 2);

            BlockEntity be = world.getBlockEntity(p);

            if (be instanceof ModSpawnerBlockEntity) {
                ModSpawnerBlockEntity ms = (ModSpawnerBlockEntity) be;
                ms.spawnAllAtOnce = true;
                ms.spawnsLeft = mobs;
                ms.requiredPlayerRange = 25;
                if (i == 0 && (data.pieces == 2 || RandomUtils.roll(20))) {
                    ms.spawnBoss = true;
                }
            }
            list.remove(p);
        }

        world.destroyBlock(pos, false);

    }

    public static void setChest(Level world, BlockPos p) {
        world.setBlock(p, Blocks.CHEST.defaultBlockState(), 2);
        ChestBlockEntity chest = (ChestBlockEntity) world.getBlockEntity(p);
        chest.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON, world.getRandom().nextLong());

    }
}
