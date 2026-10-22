package com.robertx22.mine_and_slash.event_hooks.damage_hooks;

import com.robertx22.mine_and_slash.event_hooks.damage_hooks.util.AttackInformation;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ValidDamageUtil {

    // private static List<String> VALID_PROJECTILE_NAMES = Arrays.asList("arrow", "bolt", "ammo", "bullet", "dart", "missile");

    public static boolean isValidAttack(AttackInformation event) {
        if (!(event.getSource().getEntity() instanceof Player)) {
            return true;
        }

        LivingEntity en = (LivingEntity) event.getSource().getEntity();
        DamageSource source = event.getSource();
        GearItemData gear = event.weaponData;

        if (gear != null) {
            var type = gear.GetBaseGearType();
            return type.weaponType().damage_validity_check.isValid(source);
        }

        return source.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "player_attack")));
    }
}
