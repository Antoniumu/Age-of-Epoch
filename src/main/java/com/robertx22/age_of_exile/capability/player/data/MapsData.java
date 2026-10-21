package com.robertx22.age_of_exile.capability.player.data;

import com.robertx22.addon.divine_missions.types.CompleteDungeonTask;
import com.robertx22.age_of_exile.capability.PlayerDamageChart;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.dimension.DungeonDimensionJigsawFeature;
import com.robertx22.age_of_exile.dimension.PopulateDungeonChunks;
import com.robertx22.age_of_exile.dimension.dungeon_data.*;
import com.robertx22.age_of_exile.dimension.teleporter.portal_block.PortalBlockEntity;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.SignUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TeamUtils;
import com.robertx22.divine_missions.mission_gen.MissionUtil;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.utils.Watch;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.*;

import net.minecraft.core.registries.Registries;
@Storable
public class MapsData {

    public static Block TELEPORT_TO_PLACEHOLDER_BLOCK = Blocks.PLAYER_HEAD;
    static int cachedBorder = 0;

    @Store
    public int ticksinPortal = 0;
    @Store
    public int highestTierDone = 0;

    @Store
    public DungeonData dungeonData = new DungeonData();
    @Store
    public BlockPos tel_pos = new BlockPos(0, 0, 0);
    @Store
    public String tp_b_dim = "";
    @Store
    public boolean isEmpty = true;
    @Store
    public String orig_gamemode = "";

    static int getBorder(Level world) {
        if (cachedBorder == 0) {
            cachedBorder = (int) (world.getWorldBorder()
                .getSize() / 2);
        }
        return cachedBorder;
    }

    public void onDungeonCompletedAdvanceProgress(Player player) {

        TeamUtils.getOnlineMembers(player)
            .forEach(e -> {
                MissionUtil.tryDoMissions(e, x -> {
                    if (x.getTaskEntry().task_type.equals(CompleteDungeonTask.ID)) {
                        x.increaseProgress();
                        return;
                    }
                });
            });

        int tier = 0;
        try {
            tier = dungeonData
                .getDifficulty().rank;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tier > highestTierDone) {
            highestTierDone = tier;
        }

    }

    public void onStartDungeon(Player player, TeamSize teamSize, BlockPos teleporterPos, String uuid) {

        try {

            TeamUtils.getOnlineTeamMembersInRange(player)
                .forEach(x -> PlayerDamageChart.clear(x));

            Watch total = new Watch();

            Watch first = new Watch();

            tel_pos = teleporterPos;

            // set the dungeon data for the chunk
            Level dimWorld = player.level().getServer()
                .getLevel(ResourceKey.create(Registries.DIMENSION, dungeonData.dun_type.DIMENSION_ID));

            int border = MapsData.getBorder(dimWorld);

            int X = RandomUtils.RandomRange(-border, border);
            int Z = RandomUtils.RandomRange(-border, border);

            ChunkPos cp = DungeonDimensionJigsawFeature.getSpawnChunkOf(new BlockPos(X, 0, Z));

            Watch ww = new Watch();

            if (dimWorld instanceof net.minecraft.server.level.ServerLevel serverDim) {
                ensureDungeonGenerated(serverDim, cp);
            }

            ww.print("loading first chunk ");

            BlockPos tpPos = cp.getWorldPosition()
                .offset(8, DungeonDimensionJigsawFeature.HEIGHT, 8); // todo, seems to not point to correct location?

            first.print("first part ");

            String moblist = "";

            if (true) {

                Watch w = new Watch();

                List<ChunkPos> check = PopulateDungeonChunks.getChunksAround(cp);

                boolean foundSpawn = false;
                boolean foundportalback = false;

                for (ChunkPos x : check) {

                    Set<Map.Entry<BlockPos, BlockEntity>> bes = new HashSet<>(dimWorld.getChunk(x.x(), x.z())
                        .getBlockEntities()
                        .entrySet());

                    for (Map.Entry<BlockPos, BlockEntity> e : bes) {

                        if (e.getValue() instanceof SignBlockEntity) {
                            if (SignUtils.has("[moblist]", (SignBlockEntity) e.getValue())) {
                                moblist = SignUtils.removeBraces(SignUtils.getText((SignBlockEntity) e.getValue())
                                    .get(1));
                            }
                            if (SignUtils.has("[portal]", (SignBlockEntity) e.getValue())) {
                                dimWorld.setBlockAndUpdate(e.getKey(), SlashBlocks.PORTAL.get()
                                    .defaultBlockState());
                                foundportalback = true;
                            }
                        } else if (dimWorld.getBlockState(e.getKey())
                            .getBlock() == MapsData.TELEPORT_TO_PLACEHOLDER_BLOCK) {
                            tpPos = e.getKey();
                            dimWorld.destroyBlock(tpPos, false);
                            foundSpawn = true;
                        }
                    }
                }

                if (!foundSpawn) {
                    player.sendSystemMessage(Component.literal("Ошибка: не удалось найти точку появления."));
                }
                if (!foundportalback) {
                    player.sendSystemMessage(Component.literal("Ошибка: не удалось найти позицию обратного портала."));
                }

                w.print("finding spawn");

            }

            List<BlockPos> list = new ArrayList<>();
            list.add(teleporterPos.south(2));
            list.add(teleporterPos.north(2));
            list.add(teleporterPos.east(2));
            list.add(teleporterPos.west(2));

            for (BlockPos x : list) {
                if (player.level().getBlockState(x)
                    .isAir() || player.level().getBlockState(x)
                    .getBlock() == SlashBlocks.PORTAL.get()) {
                    player.level().destroyBlock(x, false);
                    player.level().setBlockAndUpdate(x, SlashBlocks.PORTAL.get()
                        .defaultBlockState());
                    PortalBlockEntity be = (PortalBlockEntity) player.level().getBlockEntity(x);
                    be.data.dungeonPos = tpPos;
                    be.data.tpbackpos = teleporterPos.above();
                    be.data.dungeonType = dungeonData.dun_type;

                }
            }

            // first set the data so the mob levels can be set on spawn
            WorldDungeonCap data = Load.dungeonData(dimWorld);
            SingleDungeonData single = new SingleDungeonData(dungeonData, new QuestProgression(dungeonData.uuid, 20), player.getUUID()
                .toString());
            if (ExileDB.DungeonMobLists()
                .isRegistered(moblist)) {
                single.data.setMobList(moblist);
            }
            data.data.set(player, tpPos, single);

            single.data.team = teamSize;

            single.pop.startPopulating(cp);

            total.print("Total dungeon init");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void ensureDungeonGenerated(net.minecraft.server.level.ServerLevel serverDim, ChunkPos cp) {
        try {
            BlockPos startPos = new BlockPos(cp.getMinBlockX() + 8, DungeonDimensionJigsawFeature.HEIGHT, cp.getMinBlockZ() + 8);
            Registry<net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool> poolReg =
                serverDim.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
            ResourceKey<net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool> poolKey =
                ResourceKey.create(Registries.TEMPLATE_POOL, com.robertx22.age_of_exile.mmorpg.SlashRef.id("dungeon/starts"));
            Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool>> poolOpt =
                poolReg.get(poolKey);
            if (poolOpt.isEmpty()) {
                return;
            }
            net.minecraft.world.level.chunk.ChunkGenerator generator = serverDim.getChunkSource().getGenerator();
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templateManager = serverDim.getStructureManager();
            net.minecraft.world.level.StructureManager structureManager = serverDim.structureManager();
            net.minecraft.util.RandomSource random = serverDim.getRandom();
            net.minecraft.world.level.levelgen.structure.Structure.GenerationContext ctx =
                new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(
                    serverDim.registryAccess(),
                    generator,
                    generator.getBiomeSource(),
                    serverDim.getChunkSource().randomState(),
                    templateManager,
                    serverDim.getSeed() + cp.toLong(),
                    cp,
                    serverDim,
                    b -> true
                );
            Optional<net.minecraft.world.level.levelgen.structure.Structure.GenerationStub> stub =
                net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement.addPieces(
                    ctx,
                    poolOpt.get(),
                    Optional.empty(),
                    3,
                    startPos,
                    false,
                    Optional.empty(),
                    new net.minecraft.world.level.levelgen.structure.structures.JigsawStructure.MaxDistance(128),
                    net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup.EMPTY,
                    net.minecraft.world.level.levelgen.structure.structures.JigsawStructure.DEFAULT_DIMENSION_PADDING,
                    net.minecraft.world.level.levelgen.structure.structures.JigsawStructure.DEFAULT_LIQUID_SETTINGS
                );
            if (stub.isPresent()) {
                net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
                for (net.minecraft.world.level.levelgen.structure.StructurePiece piece : builder.build().pieces()) {
                    if (piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece poolPiece) {
                        poolPiece.place(
                            serverDim,
                            structureManager,
                            generator,
                            random,
                            net.minecraft.world.level.levelgen.structure.BoundingBox.infinite(),
                            startPos,
                            false
                        );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // todo add to RPG CAP

}
