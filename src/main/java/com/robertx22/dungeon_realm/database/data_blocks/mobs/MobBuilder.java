package com.robertx22.dungeon_realm.database.data_blocks.mobs;

import com.robertx22.dungeon_realm.api.DungeonExileEvents;
import com.robertx22.dungeon_realm.api.DungeonMobSpawnedEvent;
import com.robertx22.dungeon_realm.api.PrepareDungeonMobEditsEvent;
import com.robertx22.dungeon_realm.capability.DungeonEntityCapability;
import com.robertx22.dungeon_realm.capability.DungeonEntityData;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MobBuilder {

    private EntityType type;
    public int amount = 1;
    public DungeonEntityData mobEntityData;

    public Optional<MapDataBlock> dataBlock;
    public List<Consumer<LivingEntity>> mobEdits = new ArrayList<>();

    private MobBuilder() {
    }

    public static MobBuilder of(EntityType type, MapDataBlock block, Consumer<MobBuilder> co) {
        MobBuilder b = new MobBuilder();
        b.type = type;
        b.dataBlock = Optional.ofNullable(block);
        co.accept(b);
        return b;
    }

    public <T extends LivingEntity> List<T> summonMobs(Level world, BlockPos p) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            T mob = (T) summon(type, world, p);
            list.add(mob);
        }

        DungeonMain.ifMapData(world, p).ifPresent(x -> {
            if(this.mobEntityData.isDungeonMob) {
                x.mobSpawnCount += amount;
                if(this.mobEntityData.isPackMob) {
                    x.processedPackDataBlockCount++;
                } else {
                    x.processedMobDataBlockCount++;
                }
            } else if (this.mobEntityData.isDungeonEliteMob) {
                x.eliteSpawnCount += amount;
                if(this.mobEntityData.isPackMob) {
                    x.processedElitePackDataBlockCount++;
                } else {
                    x.processedEliteDataBlockCount++;
                }
            } else if (this.mobEntityData.isMiniBossMob) {
                x.miniBossSpawnCount += amount;
                x.processedMiniBossDataBlockCount++;
            }
        });

        return list;
    }

    private <T extends Mob> T summon(EntityType<T> type, Level world, BlockPos p) {
        // p = SpawnPointHelper.getBestSpawnPosition(world, p);

        MyPosition vec = new MyPosition(p);

        T mob = (T) type.create(world, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);

        mob.finalizeSpawn((ServerLevelAccessor) world, ((net.minecraft.server.level.ServerLevel) world).getCurrentDifficultyAt(p), net.minecraft.world.entity.EntitySpawnReason.REINFORCEMENT, null);
        mob.setPos(vec.x(), vec.y(), vec.z());


        var prepare = new PrepareDungeonMobEditsEvent(mob, dataBlock);
        DungeonExileEvents.PREPARE_DUNGEON_MOB_SPAWN.callEvents(prepare);
        this.mobEdits.addAll(prepare.edits);

        // todo should i do edits before or after mob is spawned?

        world.addFreshEntity(mob);

        for (Consumer<LivingEntity> edit : this.mobEdits) {
            edit.accept(mob);
        }
        DungeonEntityCapability.get(mob).data = this.mobEntityData;

        var afterSpawn = new DungeonMobSpawnedEvent(mob);
        DungeonExileEvents.DUNGEON_MOB_SPAWNED.callEvents(afterSpawn);

        return mob;
    }

}
