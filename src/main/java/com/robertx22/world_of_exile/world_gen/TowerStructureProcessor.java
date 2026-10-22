package com.robertx22.world_of_exile.world_gen;

import com.mojang.serialization.MapCodec;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlocks;
import com.robertx22.world_of_exile.tower.TowerTheme;
import com.robertx22.world_of_exile.tower.TowerThemes;
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

/**
 * Turns the tag signs baked into the tower templates into real content.
 *
 * <p>Sign tags understood:</p>
 * <ul>
 *   <li>{@code [boss]} – the boss cage, carries {@code [tower:<theme>]}</li>
 *   <li>{@code [spawner]} – themed mob spawner, carries {@code [tower:<theme>]}</li>
 *   <li>{@code [treasure]} / {@code [final_treasure]} – chest with
 *       {@code [loot#<table>]} and an optional {@code [chance:<percent>]}</li>
 *   <li>{@code [deployer]} – removed</li>
 * </ul>
 *
 * Legacy templates (stone brick tower, blackstone tower) still work: they only
 * carry {@code [boss]}/{@code [spawner]}/{@code [treasure]} and fall back to a
 * generic spawner pool that never contains nether mobs in the overworld.
 */
public class TowerStructureProcessor implements StructureProcessor {

    public static final MapCodec<TowerStructureProcessor> CODEC = MapCodec.unit(TowerStructureProcessor::new);

    private static final Pattern CHANCE_PATTERN = Pattern.compile("\\[chance\\s*:\\s*(\\d+)]");
    private static final Pattern THEME_PATTERN = Pattern.compile("\\[tower:([a-z0-9_]+)]");
    private static final Pattern LOOT_PATTERN = Pattern.compile("\\[loot#([a-z0-9_./:-]+)]");

    /** Overworld-only fallback pool, kept for the pre-theme tower templates. */
    private static final List<String> GENERIC_MOBS = List.of(
        "minecraft:zombie",
        "minecraft:skeleton",
        "minecraft:spider",
        "minecraft:cave_spider",
        "minecraft:witch",
        "minecraft:vindicator",
        "minecraft:pillager"
    );

    private static final List<String> NETHER_MOBS = List.of(
        "minecraft:wither_skeleton",
        "minecraft:piglin",
        "minecraft:blaze",
        "minecraft:magma_cube"
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

        // legacy beacon markers from the original tower templates
        if (processedBlockInfo.state().is(Blocks.BEACON)) {
            if (random.nextBoolean()) {
                return createChestInfo(processedBlockInfo.pos(), "minecraft:chests/simple_dungeon", random);
            }
            return createSpawnerInfo(processedBlockInfo.pos(), randomMob(null, random), random);
        }

        if (processedBlockInfo.state().is(BlockTags.ALL_SIGNS)) {
            String raw = processedBlockInfo.nbt() != null ? processedBlockInfo.nbt().toString() : "";

            Matcher chanceMatcher = CHANCE_PATTERN.matcher(raw);
            if (chanceMatcher.find()) {
                try {
                    float chance = Float.parseFloat(chanceMatcher.group(1));
                    if (random.nextFloat() * 100.0F > chance) {
                        return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), Blocks.AIR.defaultBlockState(), null);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            String theme = match(THEME_PATTERN, raw);
            if (theme == null && level instanceof net.minecraft.world.level.Level realLevel) {
                theme = TowerThemes.forDimension(realLevel).id();
            }
            String loot = match(LOOT_PATTERN, raw);

            if (raw.contains("[boss]")) {
                return createBossMarker(processedBlockInfo.pos(), theme);
            }
            if (raw.contains("[spawner]")) {
                return createSpawnerInfo(processedBlockInfo.pos(), randomMob(theme, random), random);
            }
            if (raw.contains("[final_treasure]") || raw.contains("[lock_treasure]")) {
                return createChestInfo(processedBlockInfo.pos(),
                    loot != null ? loot : "minecraft:chests/end_city_treasure", random);
            }
            if (raw.contains("[treasure]")) {
                return createChestInfo(processedBlockInfo.pos(),
                    loot != null ? loot : "minecraft:chests/simple_dungeon", random);
            }
            if (raw.contains("[deployer]")) {
                return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), Blocks.AIR.defaultBlockState(), null);
            }
        }

        return processedBlockInfo;
    }

    private static String match(Pattern pattern, String raw) {
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String randomMob(String themeId, RandomSource random) {
        if (TowerThemes.exists(themeId)) {
            TowerTheme theme = TowerThemes.byId(themeId);
            return theme.randomMob(random);
        }
        return GENERIC_MOBS.get(random.nextInt(GENERIC_MOBS.size()));
    }

    private static StructureTemplate.StructureBlockInfo createBossMarker(BlockPos pos, String themeId) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("restore_campfire", false);
        if (TowerThemes.exists(themeId)) {
            data.putString("theme", themeId);
        }
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

    private static StructureTemplate.StructureBlockInfo createSpawnerInfo(BlockPos pos, String mobId, RandomSource random) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:mob_spawner");
        tag.putShort("Delay", (short) 20);
        tag.putShort("MinSpawnDelay", (short) 200);
        tag.putShort("MaxSpawnDelay", (short) 400);
        tag.putShort("SpawnCount", (short) 2);
        tag.putShort("MaxNearbyEntities", (short) 4);
        tag.putShort("RequiredPlayerRange", (short) 24);
        tag.putShort("SpawnRange", (short) 4);

        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", mobId);
        entityTag.putBoolean("PersistenceRequired", true);
        net.minecraft.nbt.ListTag tags = new net.minecraft.nbt.ListTag();
        tags.add(net.minecraft.nbt.StringTag.valueOf(TowerBoss.MOB_TAG));
        entityTag.put("Tags", tags);

        CompoundTag light = new CompoundTag();
        light.putInt("min_inclusive", 0);
        light.putInt("max_inclusive", 15);
        CompoundTag rules = new CompoundTag();
        rules.put("block_light_limit", light);
        rules.put("sky_light_limit", light.copy());

        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", entityTag);
        spawnData.put("custom_spawn_rules", rules);
        tag.put("SpawnData", spawnData);

        CompoundTag potential = new CompoundTag();
        potential.putInt("weight", 1);
        potential.put("data", spawnData.copy());
        net.minecraft.nbt.ListTag potentials = new net.minecraft.nbt.ListTag();
        potentials.add(potential);
        tag.put("SpawnPotentials", potentials);
        return new StructureTemplate.StructureBlockInfo(pos, Blocks.SPAWNER.defaultBlockState(), tag);
    }

    /** Kept for compatibility with the legacy blackstone tower template. */
    static List<String> netherMobs() {
        return NETHER_MOBS;
    }
}
