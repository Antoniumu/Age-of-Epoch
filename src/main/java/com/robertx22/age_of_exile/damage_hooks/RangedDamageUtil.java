package com.robertx22.age_of_exile.damage_hooks;

import com.robertx22.age_of_exile.damage_hooks.util.AttackInformation;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.List;

public class RangedDamageUtil {

    private static List<String> VALID_PROJECTILE_NAMES = Arrays.asList("arrow", "bolt", "ammo", "bullet", "dart", "missile", "trident", "fireworks");

    public static boolean isValidAttack(AttackInformation event) {

        if (!(event.getSource()
            .getEntity() instanceof Player)) {
            return true;
        }
        LivingEntity en = (LivingEntity) event.getSource()
            .getEntity();
        DamageSource source = event.getSource();
        Item item = en.getMainHandItem()
            .getItem();
        GearItemData gear = event.weaponData != null ? event.weaponData : Gear.Load(en.getMainHandItem());

        boolean isProjectileWeapon = false;
        if (gear != null && gear.GetBaseGearType() != null) {
            isProjectileWeapon = gear.GetBaseGearType().weaponType().isProjectile;
        } else {
            GearSlot slot = GearSlot.getSlotOf(item);
            if (slot != null && ("bow".equals(slot.GUID()) || "crossbow".equals(slot.GUID()))) {
                isProjectileWeapon = true;
            }
        }

        boolean isIndirectOrProjectile = !source.isDirect() || source.is(DamageTypeTags.IS_PROJECTILE);

        if (isProjectileWeapon) {
            if (!isIndirectOrProjectile && VALID_PROJECTILE_NAMES.stream()
                .noneMatch(x -> source.getMsgId().contains(x))) {
                return false;
            }
        } else {
            if (isIndirectOrProjectile) {
                if (source.getDirectEntity() instanceof ThrownTrident) {
                    return true;
                }
                return false;
            }
        }
        return true;
    }
}
