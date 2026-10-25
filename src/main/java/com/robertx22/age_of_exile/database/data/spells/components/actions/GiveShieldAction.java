package com.robertx22.age_of_exile.database.data.spells.components.actions;

import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.database.data.value_calc.ValueCalculation;
import com.robertx22.age_of_exile.uncommon.effectdatas.GiveShieldEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.Collection;

import static com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.SECONDS;
import static com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.VALUE_CALCULATION;

public class GiveShieldAction extends SpellAction {

    public GiveShieldAction() {
        super(Arrays.asList(VALUE_CALCULATION));
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {

        if (!ctx.world.isClientSide()) {
            ValueCalculation calc = data.get(VALUE_CALCULATION);

            int value = calc.getCalculatedValue(ctx.levelProvider);

            int seconds = data.get(SECONDS)
                .intValue();

            // Диагностика аудита: видно и «никому не досталось» (пустой список
            // целей), и «досталось, но щит не появился».
            com.robertx22.age_of_exile.uncommon.testing.SpellAuditProbe.diag(
                "diag_giveshield targets=" + targets.size() + " value=" + value + " seconds=" + seconds
                    + " caster=" + ctx.caster.getClass()
                    .getSimpleName());

            targets.forEach(x -> {
                GiveShieldEvent effect = new GiveShieldEvent(value, seconds, ctx.caster, x);
                effect.Activate();
                com.robertx22.age_of_exile.uncommon.testing.SpellAuditProbe.diag(
                    "diag_giveshield_after target=" + x.getClass()
                        .getSimpleName() + " total=" + com.robertx22.age_of_exile.uncommon.datasaving.Load.Unit(x)
                        .getResources()
                        .getShield());
            });

        }

    }

    public MapHolder create(ValueCalculation calc, Double seconds) {
        MapHolder dmg = new MapHolder();
        dmg.type = GUID();
        dmg.put(SECONDS, seconds);
        dmg.put(VALUE_CALCULATION, calc);
        return dmg;
    }

    @Override
    public String GUID() {
        return "give_absorb";
    }
}
