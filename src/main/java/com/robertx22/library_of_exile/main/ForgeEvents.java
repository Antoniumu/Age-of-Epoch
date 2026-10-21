package com.robertx22.library_of_exile.main;

import com.robertx22.library_of_exile.components.PlayerCapabilities;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.events.base.StaticServerPlayerTickEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.AddReloadListenerEvent;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDamageEvent;
import com.robertx22.forge_compat.event.entity.living.LivingDeathEvent;
import com.robertx22.forge_compat.event.entity.living.LivingEvent;
import com.robertx22.forge_compat.event.entity.player.PlayerEvent;
import com.robertx22.forge_compat.eventbus.api.Event;
import com.robertx22.forge_compat.eventbus.api.EventPriority;
import com.robertx22.forge_compat.fml.event.lifecycle.IModBusEvent;
import com.robertx22.forge_compat.fml.event.server.FMLServerStartedEvent;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Consumer;

public class ForgeEvents {

    public static <T extends Event> void registerForgeEvent(Class<T> clazz, Consumer<T> event, EventPriority priority) {
        if (IModBusEvent.class.isAssignableFrom(clazz)) {
            FMLJavaModLoadingContext.get()
                .getModEventBus()
                .addListener(priority, clazz, event);
        } else {
            MinecraftForge.EVENT_BUS.addListener(priority, clazz, event);
        }
    }

    public static <T extends Event> void registerForgeEvent(Class<T> clazz, Consumer<T> event) {
        registerForgeEvent(clazz, event, EventPriority.NORMAL);
    }

    public static void register() {

        // ВАЖНО: подписываемся на серверный тик явно.
        // @Mod.EventBusSubscriber в этом порте не сканирует экземплярные методы,
        // поэтому раньше watchdog телепорта не работал и флаг неуязвимости
        // ("Invulnerable") оставался на игроке навсегда.
        registerForgeEvent(TickEvent.ServerTickEvent.class, StaticServerPlayerTickEvent::onServerTick);

        registerForgeEvent(LivingDamageEvent.class, event -> {
            ExileEvents.OnDamageEntity after = ExileEvents.DAMAGE_BEFORE_CALC.callEvents(
                new ExileEvents.OnDamageEntity(event.getSource(), event.getAmount(), event.getEntityLiving())
            );
            event.setAmount(after.damage);
        }, EventPriority.HIGHEST);

        registerForgeEvent(LivingDamageEvent.class, event -> {
            ExileEvents.OnDamageEntity after = ExileEvents.DAMAGE_AFTER_CALC.callEvents(
                new ExileEvents.OnDamageEntity(event.getSource(), event.getAmount(), event.getEntityLiving())
            );
            event.setAmount(after.damage);
        }, EventPriority.LOWEST);

        registerForgeEvent(LivingEvent.LivingUpdateEvent.class, event -> {
            LivingEntity entity = event.getEntityLiving();
            ExileEvents.LIVING_ENTITY_TICK.callEvents(new ExileEvents.OnEntityTick(entity));
        });

        registerForgeEvent(LivingDeathEvent.class, event -> {
            if (event.getEntityLiving() instanceof Player == false && event.getSource()
                .getEntity() instanceof LivingEntity) {
                ExileEvents.MOB_DEATH.callEvents(new ExileEvents.OnMobDeath(event.getEntityLiving(), (LivingEntity) event.getSource()
                    .getEntity()));
            }
        });

        registerForgeEvent(PlayerEvent.Clone.class, event -> {
            PlayerCapabilities.saveAllOnDeath(event);
            PlayerCapabilities.syncAllToClient(event.getPlayer());
        });

        registerForgeEvent(FMLServerStartedEvent.class, event -> {
            CommonInit.onDatapacksReloaded();
        });

        registerForgeEvent(AddReloadListenerEvent.class, event -> {
            CommonInit.onDatapacksReloaded();
        });
    }
}