package com.robertx22.dungeon_realm.structure;


import com.robertx22.dungeon_realm.database.holders.DungeonBonusContents;
import com.robertx22.dungeon_realm.database.holders.DungeonRelicStats;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.library_of_exile.components.LibMapData;
import com.robertx22.library_of_exile.database.extra_map_content.MapContent;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.database.relic.stat.ContentWeightRS;
import com.robertx22.library_of_exile.database.relic.stat.ExtraContentRS;
import com.robertx22.library_of_exile.database.relic.stat.RelicStat;
import com.robertx22.library_of_exile.dimension.structure.MapStructure;
import com.robertx22.library_of_exile.util.PointData;
import com.robertx22.library_of_exile.util.Weighted;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MapBonusContentsData {

    HashMap<String, BonusContentData> map = new HashMap<>();

    public int totalGenDungeonChunks = 0;
    public int processedChunks = 0;

    // chunks mechanics were tried to spawn or succeded in (limit 1 per chunk, both attempts and successes)
    public Set<PointData> mechsChunks = new HashSet<>();

    // calc chance in such a way that if a mechanic should spawn 5 times, and the dungeon has 20 chunks left, it scatters the spawn chunks but never accidentally not generates
    public float calcSpawnChance(BlockPos pos) {

        var cp = new ChunkPos(pos);
        var point = new PointData(cp.x, cp.z);

        if (mechsChunks.contains(point)) {
            return 0;
        }

        int remaining = getTotalSpawnsRemainingFromAllContents();
        int chunksLeft = totalGenDungeonChunks - processedChunks;

        if (chunksLeft < remaining) {
            return 100;
        }

        float chance = remaining / (float) chunksLeft * 100F;

        return chance;

    }


    public int getTotalSpawnsRemainingFromAllContents() {
        return (int) map.values().stream().mapToInt(x -> x.remainingSpawns).sum();
    }


    void addContent(MapContent c, LibMapData libdata) {
        int amount = RandomUtils.RandomRange(c.min_blocks, c.max_blocks);

        for (RelicStat stat : LibDatabase.RelicStats().getList()) {
            if (stat instanceof ExtraContentRS extra && extra.data.type() == ExtraContentRS.Type.ADDITION) {
                if (extra.data.map_content_id().equals(c.GUID())) {
                    if (RandomUtils.roll(libdata.relicStats.get(stat))) {
                        amount += extra.data.extra();
                    }
                }
            }
        }
        for (RelicStat stat : LibDatabase.RelicStats().getList()) {
            if (stat instanceof ExtraContentRS extra && extra.data.type() == ExtraContentRS.Type.MULTIPLY) {
                if (extra.data.map_content_id().equals(c.GUID())) {
                    if (RandomUtils.roll(libdata.relicStats.get(stat))) {
                        amount *= extra.data.extra();
                    }
                }
            }
        }

        var data = new BonusContentData(amount);
        this.map.put(c.GUID(), data);
    }

    public void setupOnMapStart(ItemStack stack, LibMapData libdata, Player p) {

        var map = DungeonItemNbt.DUNGEON_MAP.loadFrom(stack);

        for (MapContent c : LibDatabase.MapContent().getFiltered(x -> x.always_spawn)) {
            addContent(c, libdata);
        }

        int bonus = map.bonus_contents;

        if (RandomUtils.roll(libdata.relicStats.get(DungeonRelicStats.INSTANCE.BONUS_CONTENT_CHANCE))) {
            bonus++;
        }

        var possible = LibDatabase.MapContent().getFiltered(x -> !x.always_spawn && x.Weight() > 0).stream().map(e -> {
            int weight = e.weight;
            for (RelicStat stat : LibDatabase.RelicStats().getList()) {
                if (stat instanceof ContentWeightRS cw && cw.map_content_id.equals(e.GUID())) {
                    weight *= 1F + libdata.relicStats.get(cw) / 100F;
                }
            }
            return new Weighted<MapContent>(e, weight);
        }).collect(Collectors.toList());

        if (bonus > possible.size()) {
            bonus = possible.size();
        }

        for (int i = 0; i < bonus; i++) {
            var c = RandomUtils.weightedRandom(possible).obj;
            addContent(c, libdata);
            possible.removeIf(x -> x.obj.GUID().equals(c.GUID()));
        }

        if (map.uber) {
            addContent(DungeonBonusContents.INSTANCE.UBER_BOSS.get(), libdata);
        }
    }

    public BonusContentData get(MapStructure m) {
        return map.getOrDefault(m.guid(), BonusContentData.EMPTY);
    }

}
