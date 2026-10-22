package com.robertx22.the_harvest.structure;

import com.robertx22.library_of_exile.components.LibMapCap;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.the_harvest.capability.HarvestEntityCap;
import com.robertx22.the_harvest.configs.HarvestConfig;
import com.robertx22.the_harvest.database.holders.HarvestRelicStats;
import com.robertx22.the_harvest.item.HarvestItemMapData;
import com.robertx22.the_harvest.main.HarvestMain;
import com.robertx22.the_harvest.main.HarvestWords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HarvestMapData {

    public HarvestItemMapData item = new HarvestItemMapData();

    public int mobsLeft = 0;

    public int ticksLeft = 20 * 60 * 2;

    public int x = 0;
    public int z = 0;

    public Long tileTicker = 0L;


    public void onStartMap(Player p) {

        this.mobsLeft = HarvestConfig.get().MAX_TOTAL_MOB_SPAWNS.get();
        this.ticksLeft = 20 * 60 * 2;
    }

    public List<LivingEntity> getAllLivingMobs(Level world, BlockPos pos) {
        return world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(pos.getCenter(), 30, 30, 30)).stream().filter(x -> HarvestEntityCap.get(x).data.isHarvestSpawn).collect(Collectors.toList());
    }


    // todo this isnt the best idea
    void tickSecond(Level world, BlockPos pos) {

        if (mobsLeft < 1 && ticksLeft > 20) {
            ticksLeft = 10;
        }

        if (tileTicker == 0L) {
            tileTicker = pos.asLong();
        }

        if (ticksLeft > 0) {
            if (pos.asLong() == tileTicker) {
                ticksLeft -= 20;

                if (ticksLeft < 0) {
                    for (Player p : HarvestMain.HARVEST_MAP_STRUCTURE.getAllPlayersInMap(world, pos)) {
                        p.sendSystemMessage(HarvestWords.HARVEST_END.get().withStyle(ChatFormatting.GREEN));
                    }
                } else {
                    for (Player p : HarvestMain.HARVEST_MAP_STRUCTURE.getAllPlayersInMap(world, pos)) {
                        int secleft = (ticksLeft) / 20;
                        actionBar((ServerPlayer) p, HarvestWords.HARVEST_TIME_REMAINING.get(secleft).withStyle(ChatFormatting.GREEN));
                    }
                }
            }
        }


    }

    public static void actionBar(ServerPlayer p, MutableComponent title) {
        p.connection.send(new ClientboundSetActionBarTextPacket(title));
    }

    public void waveLogicSecond(Level world, BlockPos pos) {

        if (HarvestMain.HARVEST_MAP_STRUCTURE.getAllPlayersInMap(world, pos).isEmpty()) {
            return; // we dont do anything if players arent here
        }

        tickSecond(world, pos);

        if (ticksLeft < 1) {
            return;
        }
        var list = getAllLivingMobs(world, pos);

        if (list.size() < 3) {

            var mobs = HarvestMain.DUNGEON_MOB_SPAWNS.getPredeterminedRandom(world, pos);

            int toSpawn = 15;

            var data = LibMapCap.getData(world, pos);
            if (data != null) {
                float multi = 1F + data.relicStats.get(HarvestRelicStats.INSTANCE.MOBS_SPAWNED.get()) / 100F;
                toSpawn *= multi;
            }

            if (toSpawn > mobsLeft) {
                toSpawn = mobsLeft;
            }

            List<Direction> dirs = Arrays.asList(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH);
            for (int i = 0; i < toSpawn; i++) {
                var mob = RandomUtils.weightedRandom(mobs.mobs);
                spawnMob(world, pos.offset(RandomUtils.randomFromList(dirs).getUnitVec3i().multiply(2)), mob.getType());
            }
        }

    }

    public void spawnMob(Level world, BlockPos pos, EntityType type) {

        this.mobsLeft--;

        LivingEntity en = (LivingEntity) type.create(world, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        en.setPos(pos.getX(), pos.getY(), pos.getZ());

        if (en instanceof Mob mob) {
            mob.finalizeSpawn((ServerLevel) world, world.getCurrentDifficultyAt(pos), net.minecraft.world.entity.EntitySpawnReason.COMMAND, (SpawnGroupData) null);
        }

        world.addFreshEntity(en);

        HarvestEntityCap.get(en).data.isHarvestSpawn = true;

    }
}
