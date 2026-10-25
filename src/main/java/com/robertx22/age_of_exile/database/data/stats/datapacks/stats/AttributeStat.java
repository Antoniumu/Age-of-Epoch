package com.robertx22.age_of_exile.database.data.stats.datapacks.stats;

import com.robertx22.age_of_exile.database.data.stats.StatScaling;
import com.robertx22.age_of_exile.database.data.stats.datapacks.base.BaseDatapackStat;
import com.robertx22.age_of_exile.saveclasses.unit.StatData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

public class AttributeStat extends BaseDatapackStat {

    public static String SER_ID = "vanilla_attribute_stat_ser";

    transient String locname;

    public UUID uuid;
    public String attributeId;
    public Attribute attribute;

    public AttributeStat(String id, String locname, UUID uuid, Holder<Attribute> attribute, boolean perc) {
        this(id, locname, uuid, attribute.value(), perc);
    }

    public AttributeStat(String id, String locname, UUID uuid, Attribute attribute, boolean perc) {
        super(SER_ID);
        this.id = id;
        this.locname = locname;
        this.uuid = uuid;
        this.attributeId = BuiltInRegistries.ATTRIBUTE.getKey(attribute)
            .toString();
        this.attribute = attribute;
        this.is_perc = perc;
        this.scaling = StatScaling.NONE;
    }

    @Override
    public String locDescForLangFile() {
        return "Increase vanilla attribute.";
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

    public void addToEntity(LivingEntity en, StatData data) {

        float val = data.getValue();

        AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_VALUE;

        if (IsPercent()) {
            operation = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

            val = (0F + val) / 100F;
        }

        Identifier modId = Identifier.fromNamespaceAndPath("mmorpg", uuid.toString());
        AttributeModifier mod = new AttributeModifier(
            modId,
            val,
            operation
        );

        AttributeInstance atri = en.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));

        if (atri != null) {
            if (atri.hasModifier(modId)) {
                atri.removeModifier(modId);
            }
            atri.addPermanentModifier(mod);
        }

    }
}
