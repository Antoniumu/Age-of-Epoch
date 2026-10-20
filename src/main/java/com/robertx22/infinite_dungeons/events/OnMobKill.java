package com.robertx22.infinite_dungeons.events;

import com.robertx22.infinite_dungeons.components.MobIDCap;
import com.robertx22.infinite_dungeons.configs.IDConfig;
import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.group.DungeonGroup;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class OnMobKill extends EventConsumer<ExileEvents.OnMobDeath> {

    @Override
    public void accept(ExileEvents.OnMobDeath event) {

        if (MobIDCap.get(event.mob)
            .isDungeonMob()) {

            MobIDCap.get(event.mob)
                .getControlBlock()
                .ifPresent(x -> x.data.getModifiers()
                    .forEach(m -> m.onMobDeath(event)));

        }

        if (event.killer instanceof Player) {

            if (event.mob instanceof Mob || event.mob instanceof Enemy || !event.mob.getType()
                .getCategory()
                .isFriendly()) {

                boolean validKill = ExileEvents.IS_KILLED_ENTITY_VALID.callEvents(new ExileEvents.IsEntityKilledValid(event.mob, event.killer)).isValid;
                // Age of Exile can call and say " you killed mob too low level, false! "

                // todo need better spawn mechanics

                // only non dungeon mobs can drop keys
                if (!MobIDCap.get(event.mob)
                    .isDungeonMob()) {
                    float chance = IDConfig.get().KEY_DROP_CHANCE.get()
                        .floatValue();
                    if (event.mob instanceof Slime) {
                        chance *= 0.1F;
                    }

                    if (validKill && RandomUtils.roll(chance)) {
                        DungeonGroup group = DungeonsDB.Groups()
                            .random();
                        ItemStack stack = new ItemStack(group.getKeyItem());
                        if (event.mob.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                            event.mob.spawnAtLocation(sl, stack, 0.0F);
                        }
                    }
                }

                try {

                    if (validKill) {
                        MobIDCap.get(event.mob)
                            .getControlBlock()
                            .ifPresent(x -> {

                                float chance = 5 * x.data.getDifficulty().coin_drop_multi;

                                if (RandomUtils.roll(chance)) {
                                    ItemStack stack = new ItemStack(RandomUtils.weightedRandom(x.data.getDungeonLayout()
                                            .getGroup().coin_drops)
                                        .getCoinItem());
                                    event.mob.spawnAtLocation(stack);
                                }
                            });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
