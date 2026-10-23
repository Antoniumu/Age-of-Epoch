package com.robertx22.age_of_exile.uncommon.enumclasses;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Locale;

public enum ModType {

    FLAT("flat", AttributeModifier.Operation.ADD_VALUE),
    PERCENT("percent", AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
    GLOBAL_INCREASE("global_increase", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    ModType(String id, AttributeModifier.Operation op) {
        this.id = id;
        this.operation = op;
    }

    public AttributeModifier.Operation operation;
    public String id;

    public boolean isFlat() {
        return this == FLAT;
    }

    public boolean isLocalIncrease() {
        return this == PERCENT;
    }

    public boolean isGlobalIncrease() {
        return this == GLOBAL_INCREASE;
    }

    public static ModType fromString(String str) {

        for (ModType type : ModType.values()) {
            if (type.id.toLowerCase(Locale.ROOT)
                .equals(str.toLowerCase(Locale.ROOT))) {
                return type;
            }
        }

        try {
            ModType TYPE = valueOf(str);

            if (TYPE != null) {
                return TYPE;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return FLAT;

    }

}
