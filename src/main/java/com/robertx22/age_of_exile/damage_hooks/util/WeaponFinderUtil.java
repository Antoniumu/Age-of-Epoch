package com.robertx22.age_of_exile.damage_hooks.util;

import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class WeaponFinderUtil {

    public static ItemStack getWeapon(DamageSource source) {

        if (source.getEntity() instanceof LivingEntity == false) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = ((LivingEntity) source.getEntity()).getMainHandItem();
        GearItemData gear = Gear.Load(stack);

        if (gear == null) {

            try {
                Entity sourceEntity = source
                    .getDirectEntity();
                Entity attacker = source
                    .getEntity();

                if (sourceEntity != null && !(sourceEntity instanceof LivingEntity)) {
                    if (attacker instanceof LivingEntity) {

                        stack = getWeaponStackFromThrownEntity(sourceEntity);
                        gear = Gear.Load(stack);

                        if (gear == null) {
                            stack = ItemStack.EMPTY;
                        } else {
                            Load.Unit(attacker)
                                .setEquipsChanged(true);
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (gear != null) {
            return stack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack getWeaponStackFromThrownEntity(Entity en) {

        if (en instanceof AbstractArrow arrow) {
            ItemStack pickup = arrow.getPickupItemStackOrigin();
            if (pickup != null && !pickup.isEmpty() && Gear.Load(pickup) != null) {
                return pickup;
            }
        }

        try {
            List<SynchedEntityData.DataValue<?>> entries = en.getEntityData().getNonDefaultValues();
            if (entries != null) {
                for (SynchedEntityData.DataValue<?> entry : entries) {
                    if (entry.value() instanceof ItemStack stack) {
                        GearItemData gear = Gear.Load(stack);
                        if (gear != null) {
                            return stack;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack tryGetStackFromNbt(Tag nbt) {
        if (nbt instanceof CompoundTag tag) {
            ItemStack s = StackNBT.loadStack(tag);
            if (s != null && !s.isEmpty()) {
                return s;
            }
        }

        return ItemStack.EMPTY;
    }
}
