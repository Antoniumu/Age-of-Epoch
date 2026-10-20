package com.robertx22.forge_compat.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

public class ArmorItem extends Item {

    protected final EquipmentSlot slot;
    protected final ArmorMaterial material;

    public ArmorItem(ArmorMaterial material, EquipmentSlot slot, Item.Properties properties) {
        super(properties);
        this.material = material;
        this.slot = slot;
    }

    public EquipmentSlot getSlot() {
        return this.slot;
    }

    public ArmorMaterial getMaterial() {
        return this.material;
    }
}
