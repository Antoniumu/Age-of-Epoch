package com.robertx22.age_of_exile.event_hooks.my_events;

import com.robertx22.age_of_exile.a_libraries.trinkets.MyTrinketUtils;
import com.robertx22.age_of_exile.capability.bases.EntityGears;
import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.dimension.rules.OnTickCancelTargettingOtherMobs;
import com.robertx22.age_of_exile.dimension.rules.OnTickDespawnIfFailedOrEmpty;
import com.robertx22.age_of_exile.dimension.rules.OnTickRegenerate;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.world_of_exile.world_gen.TowerBoss;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class OnEntityTick extends EventConsumer<ExileEvents.OnEntityTick> {

    @Override
    public void accept(ExileEvents.OnEntityTick onEntityTick) {
        LivingEntity entity = onEntityTick.entity;

        if (entity.level().isClientSide()) {
            return;
        }
        try {
            TowerBoss.contain(entity);

            if (Load.Unit(entity) == null) {
                return; // it shouldnt be though
            }

            Load.Unit(entity)
                .getCooldowns()
                .onTicksPass(1);

            if (entity instanceof Mob) {
                OnTickCancelTargettingOtherMobs.cancelTarget((Mob) entity);
                OnTickDespawnIfFailedOrEmpty.despawn((Mob) entity);
            }

            OnTickRegenerate.regen(40, entity);

            if (entity instanceof Player == false) {
                if (entity.tickCount % 40 != 0) {
                    return; // dont check gear of mobs as often as players
                }
            }

            checkGearChanged(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkGearChanged(LivingEntity entity) {

        if (entity.level().isClientSide()) {
            return;
        }

        EntityData data = Load.Unit(entity);

        EntityGears gears = data.getCurrentGears();

        boolean calc = false;

        // Сравниваем по ОТПЕЧАТКУ СОДЕРЖИМОГО (предмет + NBT), а не по ссылке:
        // иначе прокрутка/апгрейд уже надетого предмета не считалась изменением,
        // и макс. HP не пересчитывался до перезахода (а снятие — пересчитывался).
        for (EquipmentSlot s : EquipmentSlot.values()) {
            ItemStack now = entity.getItemBySlot(s);

            if (!gears.matches(s, now)) {
                calc = true;
            }
            gears.put(s, now);
        }

        if (entity instanceof Player player) {
            // Кольца и ожерелья живут вне EquipmentSlot, но их статы участвуют
            // в пересчёте — следим и за ними.
            List<ItemStack> trinkets = MyTrinketUtils.getAllSlots(player);
            if (!gears.matchesTrinkets(trinkets)) {
                calc = true;
            }
            gears.putTrinkets(trinkets);

            if (entity.tickCount % 20 == 0) {
                calc = true; // страховка: полный пересчёт раз в секунду
            }
        }

        if (calc) {
            on$change(entity);
        }

    }

    private static void on$change(LivingEntity entity) {
        if (entity != null) {

            EntityData data = Load.Unit(entity);
            data.setEquipsChanged(true);
            data.tryRecalculateStats();

            if (entity instanceof Player) {
                data.syncToClient((Player) entity);
            }
        }

    }

}
