package com.robertx22.age_of_exile.event_hooks.entity;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.rarities.MobRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.unit.Unit;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

public class OnMobSpawn {

    public static void onLoad(Entity entity) {

        if (!(entity instanceof LivingEntity)) {
            return;
        }
        if (entity instanceof Player) {
            return;
        }

        setupNewMobOnSpawn((LivingEntity) entity);

    }

    public static void setupNewMobOnSpawn(LivingEntity entity) {

        if (entity.level().isClientSide()) {
            return;
        }

        EntityData endata = Load.Unit(entity);

        if (endata != null) {

            endata.setType();

            Player nearestPlayer = null;

            nearestPlayer = PlayerUtils.nearestPlayer((ServerLevel) entity.level(), entity);

            if (endata.needsToBeGivenStats()) {
                setupNewMob(entity, endata, nearestPlayer);
                entity.heal(Integer.MAX_VALUE);
            } else {
                if (endata.getUnit() == null) {
                    endata.setUnit(new Unit());
                }

                endata.getUnit()
                    .initStats(); // give new stats to mob on spawn
                endata.forceRecalculateStats();
            }

            try {
                if (Unit.shouldSendUpdatePackets(entity)) {
                    com.robertx22.library_of_exile.main.Packets.sendToTracking(Unit.getUpdatePacketFor(entity, endata), entity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    public static Unit setupNewMob(LivingEntity entity, EntityData endata,
                                   Player nearestPlayer) {

        Unit mob = new Unit();
        mob.initStats();

        endata.SetMobLevelAtSpawn(nearestPlayer);

        String rar = endata.getRarity();

        if (com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity.BOSS_ID.equals(rar)
            || entity.entityTags().contains("woe_tower_boss")) {
            rar = com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity.BOSS_ID;
        } else {
            rar = mob.randomRarity(entity, endata);
        }
        endata.setRarity(rar);

        if (entity.entityTags().contains("woe_tower_boss") && entity.level() instanceof ServerLevel sw) {
            net.minecraft.core.BlockPos center = entity.blockPosition();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        net.minecraft.core.BlockPos p = center.offset(dx, dy, dz);
                        if (sw.getBlockState(p).is(net.minecraft.world.level.block.Blocks.SPAWNER)) {
                            sw.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }

        MobRarity rarity = ExileDB.MobRarities()
            .get(rar);
        endata.getAffixData()
            .randomizeAffixes(rarity);

        endata.setUnit(mob);

        endata.mobStatsAreSet();
        endata.forceRecalculateStats();

        return mob;

    }

}
