package com.robertx22.age_of_exile.vanilla_mc.items.gearitems.bases;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import com.robertx22.forge_compat.common.Tier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class SingleTargetWeapon extends Item implements IAutoLocName {

    public SingleTargetWeapon(Tier mat, Properties settings, String locname) {

        super(settings);
        this.locname = locname;
    }

    String locname;
    public float attackSpeed = -2.4F;

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public final String locNameForLangFile() {
        return locname;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Gear_Items;
    }

    @Override
    public String locNameLangFileGUID() {
        return BuiltInRegistries.ITEM.getKey(this)
            .toString();
    }

    @Override
    public String GUID() {
        return "";
    }

    public Multimap<Holder<Attribute>, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
        if (slot == EquipmentSlot.MAINHAND) {
            map.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 6, AttributeModifier.Operation.ADD_VALUE)
            );
            map.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_ID, (double) this.attackSpeed, AttributeModifier.Operation.ADD_VALUE)
            );
        }

        return map;
    }

}
