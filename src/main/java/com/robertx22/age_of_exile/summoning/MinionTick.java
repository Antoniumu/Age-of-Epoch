package com.robertx22.age_of_exile.summoning;

import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Тик прислужников и их хозяев (регистрируется в {@code CommonEvents}).
 * <p>
 * У прислужника проверяется срок жизни, хозяин и цель; у хозяина — не нужно ли призвать ещё зверя.
 */
public class MinionTick extends EventConsumer<ExileEvents.OnEntityTick> {

    @Override
    public void accept(ExileEvents.OnEntityTick event) {

        LivingEntity entity = event.entity;

        if (entity == null || entity.level()
            .isClientSide()) {
            return;
        }

        try {
            if (MinionUtils.isMinion(entity)) {
                MinionUtils.onMinionTick(entity);
                return;
            }

            if (entity instanceof Player) {
                MinionUtils.onPlayerTick((Player) entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
