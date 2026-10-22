package com.robertx22.mine_and_slash.database.data.exile_effects;

import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

public class VanillaStatData {

    float val;
    String uuid;
    String id;
    ModType type;

    public static VanillaStatData create(net.minecraft.core.Holder<Attribute> attr, float val, ModType type, UUID uuid) { return create(attr.value(), val, type, uuid); }
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
        return BuiltInRegistries.ATTRIBUTE.getValue(Identifier.parse(String.valueOf(id)));
    }

    public void applyVanillaStats(LivingEntity en, int stacks) {

        AttributeModifier mod = com.robertx22.forge_compat.common.CompatHelper.attrMod(UUID.fromString(uuid), "", val * stacks, type.operation);
        Attribute attri = getAttribute();

        this.removeVanillaStats(en);

        if (com.robertx22.forge_compat.common.CompatHelper.getAttribute(en, attri) != null) {
            if (!com.robertx22.forge_compat.common.CompatHelper.getAttribute(en, attri)
                    .hasModifier(mod.id())) {
                com.robertx22.forge_compat.common.CompatHelper.getAttribute(en, attri)
                        .addTransientModifier(mod);
            }
        }

    }

    public void removeVanillaStats(LivingEntity en) {
        AttributeModifier mod = com.robertx22.forge_compat.common.CompatHelper.attrMod(UUID.fromString(uuid), "", val, type.operation);
        Attribute attri = getAttribute();

        if (com.robertx22.forge_compat.common.CompatHelper.getAttribute(en, attri) != null) {
            if (com.robertx22.forge_compat.common.CompatHelper.getAttribute(en, attri)
                    .hasModifier(mod.id())) {
                com.robertx22.forge_compat.common.CompatHelper.getAttribute(en, attri)
                        .removeModifier(mod.id());
            }
        }
    }
}
