package com.robertx22.dungeon_realm.structure;

import com.robertx22.dungeon_realm.configs.DungeonConfig;
import com.robertx22.dungeon_realm.item.DungeonItemMapData;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.database.map_finish_rarity.MapFinishRarity;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.robertx22.forge_compat.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.stream.Collectors;

public class DungeonMapData {
    // todo

    public MapBonusContentsData bonusContents = new MapBonusContentsData();
    public HashMap<String, Long> spawnPositions = new HashMap<>();

    public DungeonItemMapData item = new DungeonItemMapData();

    public int x = 0;
    public int z = 0;

    public String current_mob_kill_rarity = "common";

    // Separating these out into specific counts is potentially overkill, but it makes handling specifics related to
    // elites a lot easier to manage. For now, elites count 10x toward map completion as normal mobs
    public int mobDataBlockCount = 0;
    public int packDataBlockCount = 0;
    public int eliteDataBlockCount = 0;
    public int elitePackDataBlockCount = 0;
    public int miniBossDataBlockCount = 0;

    public int processedMobDataBlockCount = 0;
    public int processedPackDataBlockCount = 0;
    public int processedEliteDataBlockCount = 0;
    public int processedElitePackDataBlockCount = 0;
    public int processedMiniBossDataBlockCount = 0;

    private int mobBlocksLeftToProcess() {
        return mobDataBlockCount - processedMobDataBlockCount;
    }

    private int packBlocksLeftToProcess() {
        return packDataBlockCount - processedPackDataBlockCount;
    }

    private int eliteBlocksLeftToProcess() {
        return eliteDataBlockCount - processedEliteDataBlockCount;
    }

    private int elitePackBlocksLeftToProcess() {
        return elitePackDataBlockCount - processedElitePackDataBlockCount;
    }

    private int miniBossBlocksLeftToProcess() {
        return miniBossDataBlockCount - processedMiniBossDataBlockCount;
    }

    public int mobSpawnCount = 0;
    public int eliteSpawnCount = 0;
    public int miniBossSpawnCount = 0;

    public int mobKills = 0;
    public int eliteKills = 0;
    public int miniBossKills = 0;

    public int totalChests = 0;
    public int lootedChests = 0;

    public MapFinishRarity getFinishRarity() {
        return LibDatabase.MapFinishRarity().get(current_mob_kill_rarity);
    }

    public void incrementSpawnBlockCountByKind(MobSpawnBlockKind kind) {
        kind.incrementDataBlockCount(this);
    }

    public void spawnBonusMapContent(Level level, BlockPos pos) {
        // bonus content should only spawn inside the main dungeon structure
        if (DungeonMain.MAIN_DUNGEON_STRUCTURE.isInside((ServerLevel) level, pos)) {
            var list = bonusContents.map.entrySet().stream().filter(x -> x.getValue().remainingSpawns > 0).collect(Collectors.toList());

            if (list.size() > 0) {
                var en = RandomUtils.randomFromList(list);
                var mc = LibDatabase.MapContent().get(en.getKey());

                var block = ForgeRegistries.BLOCKS.getValue(Identifier.parse(String.valueOf(mc.block_id)));
                level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
                bonusContents.map.get(en.getKey()).remainingSpawns--;
            }
        }
    }

    private String showMapData() {
        String result = "";
        result += "mobCommandBlockCount: (" + processedMobDataBlockCount + "/" + mobDataBlockCount + ")\n";
        result += "packCommandBlockCount: (" + processedPackDataBlockCount + "/" + packDataBlockCount + ")\n";
        result += "eliteCommandBlockCount: (" + processedEliteDataBlockCount + "/" + eliteDataBlockCount + ")\n";
        result += "elitePackCommandBlockCount: (" + processedElitePackDataBlockCount + "/" + elitePackDataBlockCount + ")\n";
        result += "miniBossCommandBlockCount: (" + processedMiniBossDataBlockCount + "/" + miniBossDataBlockCount + ")\n";

        result += "mobSpawnCount: " + mobSpawnCount + "\n";
        result += "eliteSpawnCount: " + eliteSpawnCount + "\n";
        result += "mobKills: " + mobKills + "\n";
        result += "eliteKills: " + eliteKills + "\n";
        result += "miniBossKills: " + miniBossKills + "\n";

        result += "totalChests: " + totalChests + "\n";
        result += "lootedChests: " + lootedChests + "\n";

        return result;
    }

    private int calculateKillCompletionPercent() {
        int ELITE_KILL_WEIGHT = DungeonConfig.get().ELITE_MOB_COMPLETION_WEIGHT.get();
        int MINI_BOSS_KILL_WEIGHT = DungeonConfig.get().MINI_BOSS_COMPLETION_WEIGHT.get();
        int KILL_COMPLETION_DATA_BLOCK_LEEWAY = DungeonConfig.get().KILL_COMPLETION_DATA_BLOCK_LEEWAY.get();

        // the addition here accounts for cases where I'm marking blocks as the wrong type
        // for example, if we have 43/40 mob blocks processed
        // and 0/3 elite blocks, we know all blocks have *actually* been processed, but I got the type counts wrong
        // so consider it to be complete if the total of "blocks left" equates to 0
        if (mobBlocksLeftToProcess()
                + packBlocksLeftToProcess()
                + eliteBlocksLeftToProcess()
                + elitePackBlocksLeftToProcess()
                + miniBossBlocksLeftToProcess()
                - KILL_COMPLETION_DATA_BLOCK_LEEWAY <= 0) {
            // All blocks processed - simple calculation with weighted kills
            int totalPossibleWeightedKills = mobSpawnCount + (eliteSpawnCount * ELITE_KILL_WEIGHT) + (miniBossSpawnCount * MINI_BOSS_KILL_WEIGHT);
            int actualWeightedKills = mobKills + (eliteKills * ELITE_KILL_WEIGHT) + (miniBossKills * MINI_BOSS_KILL_WEIGHT);

            if (totalPossibleWeightedKills == 0) return 0; // Avoid division by zero

            float percentage = (actualWeightedKills / (float) totalPossibleWeightedKills) * 100f;
            int rounded = Math.round(percentage);
            return Math.min(rounded, 100);
        } else {
            // Blocks still being processed - assume the worst
            int maxMobsLeft = (mobBlocksLeftToProcess() * DungeonConfig.get().MOB_MAX.get()) +
                    (packBlocksLeftToProcess() * DungeonConfig.get().PACK_MOB_MAX.get()) +
                    (eliteBlocksLeftToProcess() * ELITE_KILL_WEIGHT) +
                    (elitePackBlocksLeftToProcess() * DungeonConfig.get().PACK_MOB_MAX.get() * ELITE_KILL_WEIGHT) +
                    (miniBossBlocksLeftToProcess() * MINI_BOSS_KILL_WEIGHT);

            int totalPossibleWeightedKills = mobSpawnCount + (eliteSpawnCount * ELITE_KILL_WEIGHT) + maxMobsLeft;
            int actualWeightedKills = mobKills + (eliteKills * ELITE_KILL_WEIGHT);

            if (totalPossibleWeightedKills == 0) return 0; // Avoid division by zero

            float percentage = (actualWeightedKills / (float) totalPossibleWeightedKills) * 100f;
            int rounded = Math.round(percentage);
            return Math.min(rounded, 100);
        }
    }

    // precondition: totalChests > 0, so at least 1 has spawned
    private int calculateLootCompletionPercent() {
        if (totalChests == 0) return 0;
        float percentage = (lootedChests / (float) totalChests) * 100f;
        int rounded = Math.round(percentage);
        return Math.min(rounded, 100);
    }

    public void updateMapCompletionRarity(ServerPlayer player) {

        int killCompletionPercent = calculateKillCompletionPercent();
        ExileLog.get().debug(showMapData());

        var rar = LibDatabase.MapFinishRarity().get(current_mob_kill_rarity);
        if (rar.getHigher().isPresent()) {
            var higher = rar.getHigher().get();

            if (killCompletionPercent >= higher.perc_to_unlock) {
                current_mob_kill_rarity = higher.GUID();

                for (Player p : DungeonMain.MAIN_DUNGEON_STRUCTURE.getAllPlayersInMap(player.level(), player.blockPosition())) {
                    var rartext = getFinishRarity().getTranslation(TranslationType.NAME).getTranslatedName()
                            .withStyle(getFinishRarity().textFormatting());

                    p.sendSystemMessage(DungeonWords.MAP_COMPLETE_RARITY_UPGRADE.get(rartext).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }

    }
}
