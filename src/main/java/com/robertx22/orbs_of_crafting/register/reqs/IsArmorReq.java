package com.robertx22.orbs_of_crafting.register.reqs;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public class IsArmorReq extends ItemRequirement {
    public IsArmorReq(String id) {
        super("is_armor", id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsArmorReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(Ref.MODID).desc(ExileTranslation.registry(this, "Must be Armor"));
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        var eq = obj.stack.get(DataComponents.EQUIPPABLE);
        if (eq == null) return false;
        EquipmentSlot s = eq.slot();
        return s == EquipmentSlot.HEAD || s == EquipmentSlot.CHEST || s == EquipmentSlot.LEGS || s == EquipmentSlot.FEET;
    }
}
