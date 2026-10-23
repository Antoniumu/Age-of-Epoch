package com.robertx22.age_of_exile.database.data.exile_effects;

import com.robertx22.age_of_exile.uncommon.enumclasses.ModType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

public class VanillaStatData {

    float val;
    String uuid;
    String id;
    ModType type;

    public static VanillaStatData create(Holder<Attribute> attri, float val, ModType type, UUID uuid) {
        return create(attri.value(), val, type, uuid);
    }

    public static VanillaStatData create(Attribute attri, float val, ModType type, UUID uuid) {
        VanillaStatData data = new VanillaStatData();
        data.id = BuiltInRegistries.ATTRIBUTE.getKey(attri)
            .toString();
        data.uuid = uuid.toString();
        data.type = type;
        data.val = val;
        return data;
    }

    public Attribute getAttribute() {
        return BuiltInRegistries.ATTRIBUTE.getValue(Identifier.parse(id));
    }

    public Holder<Attribute> getAttributeHolder() {
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(getAttribute());
    }

    public void applyVanillaStats(LivingEntity en, int stacks) {
        Identifier modId = Identifier.fromNamespaceAndPath("mmorpg", uuid);
        AttributeModifier mod = new AttributeModifier(modId, val * stacks, type.operation);
        Holder<Attribute> attri = getAttributeHolder();

        this.removeVanillaStats(en);

        AttributeInstance inst = en.getAttribute(attri);
        if (inst != null) {
            if (!inst.hasModifier(modId)) {
                inst.addTransientModifier(mod);
            }
        }
    }

    public void removeVanillaStats(LivingEntity en) {
        Identifier modId = Identifier.fromNamespaceAndPath("mmorpg", uuid);
        Holder<Attribute> attri = getAttributeHolder();

        AttributeInstance inst = en.getAttribute(attri);
        if (inst != null) {
            if (inst.hasModifier(modId)) {
                inst.removeModifier(modId);
            }
        }
    }
}
