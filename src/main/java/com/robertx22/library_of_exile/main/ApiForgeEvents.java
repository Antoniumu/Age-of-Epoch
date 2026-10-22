package com.robertx22.library_of_exile.main;

import com.robertx22.library_of_exile.components.EntityInfoComponent;
import com.robertx22.library_of_exile.components.PlayerCapabilities;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.AddReloadListenerEvent;
import com.robertx22.forge_compat.event.entity.EntityJoinLevelEvent;
import com.robertx22.forge_compat.event.entity.living.LivingAttackEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDamageEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.forge_compat.event.entity.living.LivingEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;
import com.robertx22.forge_compat.event.server.ServerStartedEvent;
import com.robertx22.forge_compat.eventbus.api.Event;
import com.robertx22.forge_compat.eventbus.api.EventPriority;
import com.robertx22.forge_compat.fml.event.lifecycle.IModBusEvent;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Consumer;

public class ApiForgeEvents {

    public static <T extends Event> void registerForgeEvent(Class<T> clazz, Consumer<T> event, EventPriority priority) {
        if (IModBusEvent.class.isAssignableFrom(clazz) || clazz.isAssignableFrom(IModBusEvent.class)) {
            FMLJavaModLoadingContext.get()
                    .getModEventBus()
                    .addListener(priority, event);
        } else {
            MinecraftForge.EVENT_BUS.addListener(priority, event);
        }
    }

    public static <T extends Event> void registerForgeEvent(Class<T> clazz, Consumer<T> event) {
        registerForgeEvent(clazz, event, EventPriority.NORMAL);
    }

    public static void register() {

        registerForgeEvent(LivingAttackEvent.class, event -> {
            ExileEvents.OnDamageEntity after = ExileEvents.DAMAGE_BEFORE_CALC.callEvents(
                    new ExileEvents.OnDamageEntity(event.getSource(), event.amount(), event.getEntity())
            );
            if (after.canceled) {
                event.setCanceled(true);
            }
            // todo is needed? event.setAmount(after.damage);
        }, EventPriority.HIGHEST);


        registerForgeEvent(LivingDamageEvent.class, event -> {
            ExileEvents.OnDamageEntity after = ExileEvents.DAMAGE_AFTER_CALC.callEvents(
                    new ExileEvents.OnDamageEntity(event.getSource(), event.amount(), event.getEntity())
            );
            event.setAmount(after.damage);
        }, EventPriority.LOWEST);


        registerForgeEvent(EntityJoinLevelEvent.class, event -> {
            if (event.getEntity() instanceof LivingEntity en) {
                EntityInfoComponent.get(en).spawnInit(en);
            }
        });

        registerForgeEvent(LivingEvent.LivingTickEvent.class, event -> {
            LivingEntity entity = event.getEntity();
            if (entity.tickCount == 20) {
                EntityInfoComponent.get(entity).spawnInit(entity);
            }
            ExileEvents.LIVING_ENTITY_TICK.callEvents(new ExileEvents.OnEntityTick(entity));
        });


        registerForgeEvent(LivingDeathEvent.class, event -> {
            if (event.getEntity() instanceof Player == false && event.getSource()
                    .getEntity() instanceof LivingEntity) {
                ExileEvents.MOB_DEATH.callEvents(new ExileEvents.OnMobDeath(event.getEntity(), (LivingEntity) event.getSource()
                        .getEntity()));
            }
        });

        registerForgeEvent(PlayerEvent.Clone.class, event -> {
            PlayerCapabilities.saveAllOnDeath(event);
            PlayerCapabilities.syncAllToClient(event.getEntity());
        });

        registerForgeEvent(ServerStartedEvent.class, event -> {
            CommonInit.onDatapacksReloaded();
        });


        registerForgeEvent(AddReloadListenerEvent.class, event -> {
            CommonInit.onDatapacksReloaded();
        });
    }
}