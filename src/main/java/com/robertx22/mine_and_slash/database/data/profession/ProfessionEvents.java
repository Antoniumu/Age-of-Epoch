package com.robertx22.mine_and_slash.database.data.profession;

import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.robertx22.forge_compat.event.entity.living.BabyEntitySpawnEvent;
import com.robertx22.forge_compat.event.entity.player.ItemFishedEvent;

import java.util.Arrays;

public class ProfessionEvents {

    public static void init() {

        ExileEvents.PLAYER_MINE_ORE.register(new EventConsumer<ExileEvents.PlayerMineOreEvent>() {
            @Override
            public void accept(ExileEvents.PlayerMineOreEvent e) {
                Player p = e.player;
                if (p != null) {
                    if (!p.level().isClientSide()) {

                        if (PlayerUTIL.isFake(p)) {
                            return;
                        }

                        var drops = ExileDB.Professions().get(Professions.MINING).onMineGetBonusDrops(p, Arrays.asList(), e.state);
                        e.itemsToAddToDrop.addAll(drops);
                    }
                }
            }
        });

        ExileEvents.PLAYER_MINE_FARMABLE.register(new EventConsumer<ExileEvents.PlayerMineFarmableBlockEvent>() {
            @Override
            public void accept(ExileEvents.PlayerMineFarmableBlockEvent e) {
                Player p = e.player;
                if (p != null) {
                    if (!p.level().isClientSide()) {
                        if (PlayerUTIL.isFake(p)) {
                            return;
                        }
                        var drops = ExileDB.Professions().get(Professions.FARMING).onMineGetBonusDrops(p, e.droppedItems, e.state);
                        e.itemsToAddToDrop.addAll(drops);
                    }
                }
            }
        });

        ForgeEvents.registerForgeEvent(ItemFishedEvent.class, x -> {
            Player p = x.getEntity();
            if (p != null) {
                if (!p.level().isClientSide()) {
                    if (PlayerUTIL.isFake(p)) {
                        return;
                    }
                    var drops = ExileDB.Professions().get(Professions.FISHING).onFish(p);

                    if (!drops.isEmpty()) {
                        // p.sendSystemMessage(Chats.CAUGHT_SOMETHING.locName().withStyle(ChatFormatting.GREEN));
                    }
                    for (ItemStack drop : drops) {
                        var en = com.robertx22.forge_compat.common.CompatHelper.dropStack(p, drop);
                        //en.setDeltaMovement(x.getHookEntity().getDeltaMovement());
                    }
                }
            }
        });


        ForgeEvents.registerForgeEvent(BabyEntitySpawnEvent.class, x -> {
            Player p = x.getCausedByPlayer();
            if (p != null) {
                if (!p.level().isClientSide()) {
                    if (PlayerUTIL.isFake(p)) {
                        return;
                    }
                    if (x.getChild() != null) {
                        var drops = ExileDB.Professions().get(Professions.HUSBANDRY).onBreedAnimal(p, x.getChild());
                        for (ItemStack drop : drops) {
                            if (x.getParentA().level() instanceof net.minecraft.server.level.ServerLevel sl) x.getParentA().spawnAtLocation(sl, drop);
                        }
                    }
                }
            }
        });

    }
}
