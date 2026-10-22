package com.robertx22.dungeon_realm.database.data_blocks.mobs;

import com.robertx22.dungeon_realm.capability.DungeonEntityData;
import com.robertx22.dungeon_realm.database.holders.DungeonRelicStats;
import com.robertx22.dungeon_realm.main.DataBlockTags;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.components.LibMapCap;
import com.robertx22.library_of_exile.database.map_data_block.MapBlockCtx;
import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.util.wiki.WikiEntry;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class MapBossMB extends MapDataBlock {
    public MapBossMB(String id) {
        super(id, id);
        this.tags.add(DataBlockTags.CAN_SPAWN_LEAGUE);
    }

    @Override
    public void processImplementationINTERNAL(String s, BlockPos pos, Level level, CompoundTag nbt, MapBlockCtx ctx) {

        var arena = DungeonMain.ARENA.getArena(new ChunkPos(pos));

        EntityType<? extends LivingEntity> type = arena.getRandomBoss();

        int amount = 1;

        var data = LibMapCap.getData(level, pos);
        if (data != null) {
            float chance = data.relicStats.get(DungeonRelicStats.INSTANCE.EXTRA_MAP_BOSS_CHANCE.get());
            if (RandomUtils.roll(chance)) {
                amount++;
            }
        }

        int finalAmount = amount;
        for (LivingEntity en : MobBuilder.of(type, this, x -> {
            x.amount = finalAmount;

            DungeonEntityData d = new DungeonEntityData();
            d.isFinalMapBoss = true;

            x.mobEntityData = d;
        }).summonMobs(level, pos)) {
            if (en instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
        }
    }

    @Override
    public WikiEntry getWikiEntry() {
        return WikiEntry.of("Spawns the map boss, 1 of these is required per every Boss Arena");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return MapBossMB.class;
    }
}
