package com.robertx22.world_of_exile.world_gen;

import com.mojang.serialization.MapCodec;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TowerStructureProcessor implements StructureProcessor {

    public static final MapCodec<TowerStructureProcessor> CODEC = MapCodec.unit(TowerStructureProcessor::new);

    private static final Pattern CHANCE_PATTERN = Pattern.compile("chance\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final List<String> TOWER_MOBS = List.of(
        "minecraft:zombie",
        "minecraft:skeleton",
        "minecraft:spider",
        "minecraft:cave_spider",
        "minecraft:witch",
        "minecraft:vindicator",
        "minecraft:pillager",
        "minecraft:blaze",
        "minecraft:wither_skeleton"
    );

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return CODEC;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
        LevelReader level,
        BlockPos targetPosition,
        BlockPos referencePos,
        BlockPos templateRelativePos,
        StructureTemplate.StructureBlockInfo processedBlockInfo,
        StructurePlaceSettings settings
    ) {
        if (processedBlockInfo == null) {
            return null;
        }

        RandomSource random = settings.getRandom(processedBlockInfo.pos());

        if (processedBlockInfo.state().is(Blocks.IRON_BARS)) {
            if (random.nextFloat() < 0.25F) {
                return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), Blocks.AIR.defaultBlockState(), null);
            }
            return processedBlockInfo;
        }

        if (processedBlockInfo.state().is(Blocks.BEACON)) {
            if (random.nextBoolean()) {
                return createChestInfo(processedBlockInfo.pos(), "minecraft:chests/simple_dungeon", random);
            } else {
                return createSpawnerInfo(processedBlockInfo.pos(), random);
            }
        }

        if (processedBlockInfo.state().is(Blocks.SOUL_CAMPFIRE)) {
            return createBossMarker(processedBlockInfo.pos(), true);
        }

        if (processedBlockInfo.state().is(BlockTags.ALL_SIGNS)) {
            String raw = processedBlockInfo.nbt() != null ? processedBlockInfo.nbt().toString() : "";

            Matcher matcher = CHANCE_PATTERN.matcher(raw);
            if (matcher.find()) {
                try {
                    float chance = Float.parseFloat(matcher.group(1));
                    if (random.nextFloat() * 100.0F > chance) {
                        return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), Blocks.AIR.defaultBlockState(), null);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (raw.contains("[boss]")) {
                return createBossMarker(processedBlockInfo.pos(), false);
            }
            if (raw.contains("[spawner]")) {
                return createSpawnerInfo(processedBlockInfo.pos(), random);
            }
            if (raw.contains("[final_treasure]") || raw.contains("[lock_treasure]")) {
                return createChestInfo(processedBlockInfo.pos(), "minecraft:chests/end_city_treasure", random);
            }
            if (raw.contains("[treasure]")) {
                return createChestInfo(processedBlockInfo.pos(), "minecraft:chests/simple_dungeon", random);
            }
            if (raw.contains("[deployer]")) {
                return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), Blocks.AIR.defaultBlockState(), null);
            }
        }

        return processedBlockInfo;
    }

    private static StructureTemplate.StructureBlockInfo createBossMarker(BlockPos pos, boolean restoreCampfire) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("restore_campfire", restoreCampfire);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "mmorpg:tower_boss_marker");
        tag.put("AgeOfEpochData", data);
        return new StructureTemplate.StructureBlockInfo(pos, SlashBlocks.TOWER_BOSS_MARKER.get().defaultBlockState(), tag);
    }

    private static StructureTemplate.StructureBlockInfo createChestInfo(BlockPos pos, String lootTable, RandomSource random) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:chest");
        tag.putString("LootTable", lootTable);
        tag.putLong("LootTableSeed", random.nextLong());
        return new StructureTemplate.StructureBlockInfo(pos, Blocks.CHEST.defaultBlockState(), tag);
    }

    private static StructureTemplate.StructureBlockInfo createSpawnerInfo(BlockPos pos, RandomSource random) {
        String mobId = TOWER_MOBS.get(random.nextInt(TOWER_MOBS.size()));
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:mob_spawner");
        CompoundTag spawnData = new CompoundTag();
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", mobId);
        spawnData.put("entity", entityTag);
        tag.put("SpawnData", spawnData);
        return new StructureTemplate.StructureBlockInfo(pos, Blocks.SPAWNER.defaultBlockState(), tag);
    }

    private static StructureTemplate.StructureBlockInfo createBossSpawnerInfo(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:mob_spawner");
        tag.putShort("Delay", (short) 20);
        tag.putShort("MinSpawnDelay", (short) 800);
        tag.putShort("MaxSpawnDelay", (short) 800);
        tag.putShort("SpawnCount", (short) 1);
        tag.putShort("MaxNearbyEntities", (short) 1);
        tag.putShort("RequiredPlayerRange", (short) 14);
        tag.putShort("SpawnRange", (short) 2);

        CompoundTag spawnData = new CompoundTag();
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", "minecraft:wither_skeleton");
        entityTag.putBoolean("PersistenceRequired", true);
        net.minecraft.nbt.ListTag tagsList = new net.minecraft.nbt.ListTag();
        tagsList.add(net.minecraft.nbt.StringTag.valueOf("woe_tower_boss"));
        entityTag.put("Tags", tagsList);
        spawnData.put("entity", entityTag);
        tag.put("SpawnData", spawnData);
        return new StructureTemplate.StructureBlockInfo(pos, Blocks.SPAWNER.defaultBlockState(), tag);
    }
}
