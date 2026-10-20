package com.robertx22.age_of_exile.event_hooks.player;

import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.EntityTypeUtils;
import com.robertx22.library_of_exile.main.ForgeEvents;
import net.minecraft.world.entity.player.Player;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;

public class ScalingDifficultyEvents {

    public static void register() {

        ForgeEvents.registerForgeEvent(TickEvent.PlayerTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END) {
                if (event.player.tickCount % 20 == 0) {
                    if (!event.player.level().isClientSide()) {
                        Load.playerRPGData(event.player).scalingDifficulty.add(ServerContainer.get().DIFFICULTY_PER_SECOND.get()
                            .floatValue());
                        Load.playerRPGData(event.player).scalingDifficulty.tickDeathCooldown(20);
                    }
                }
            }
        });

        ForgeEvents.registerForgeEvent(LivingDeathEvent.class, event -> {
            try {
                if (event.getEntityLiving().level().isClientSide()) {
                    return;
                }
                if (EntityTypeUtils.isMob(event.getEntity())) {
                    if (event.getSource() != null && event.getSource()
                        .getEntity() instanceof Player) {
                        Load.playerRPGData((Player) event.getSource()
                            .getEntity()).scalingDifficulty.add(ServerContainer.get().DIFFICULTY_PER_HOSTILE_KILL.get()
                            .floatValue());

                    }
                }
                if (event.getEntityLiving() instanceof Player) {
                    if (Load.playerRPGData((Player) event.getEntityLiving()).scalingDifficulty.canAddDeathDifficulty()) {
                        Load.playerRPGData((Player) event.getEntityLiving()).scalingDifficulty.add(ServerContainer.get().DIFFICULTY_PER_DEATH.get()
                            .floatValue());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}
