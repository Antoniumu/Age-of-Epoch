package com.robertx22.age_of_exile.database.data.spells.components.conditions;

import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;

import java.util.Arrays;

import net.minecraft.core.registries.BuiltInRegistries;
public class TargetHasEffectCondition extends EffectCondition {

    public TargetHasEffectCondition() {
        super(Arrays.asList(MapField.POTION_ID));
    }

    @Override
    public boolean canActivate(SpellCtx ctx, MapHolder data) {
        MobEffect potion = BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse(data.get(MapField.POTION_ID)));
        return ctx.target != null && ctx.target.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(potion));
    }

    public MapHolder create(net.minecraft.core.Holder<MobEffect> effect) {
        return create(effect.value());
    }

    public MapHolder create(MobEffect effect) {
        MapHolder d = new MapHolder();
        d.type = GUID();
        d.put(MapField.POTION_ID, BuiltInRegistries.MOB_EFFECT.getKey(effect)
            .toString());
        return d;
    }

    @Override
    public String GUID() {
        return "target_has_potion";
    }
}

