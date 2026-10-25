package com.robertx22.age_of_exile.database.data.spells.components.actions;

import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.database.data.value_calc.ValueCalculation;
import com.robertx22.age_of_exile.saveclasses.unit.ResourceType;
import com.robertx22.age_of_exile.uncommon.effectdatas.EventBuilder;
import com.robertx22.age_of_exile.uncommon.effectdatas.RestoreResourceEvent;
import com.robertx22.age_of_exile.uncommon.effectdatas.rework.RestoreType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.Collection;

import static com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.VALUE_CALCULATION;

public class RestoreHealthAction extends SpellAction {

    public RestoreHealthAction() {
        super(Arrays.asList(VALUE_CALCULATION));
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {

        try {
            if (!ctx.world.isClientSide()) {
                ValueCalculation calc = data.get(VALUE_CALCULATION);

                int value = calc.getCalculatedValue(ctx.levelProvider);

                for (LivingEntity t : targets) {
                    float before = t.getHealth();

                    RestoreResourceEvent restore = EventBuilder.ofRestore(ctx.caster, t, ResourceType.health, RestoreType.heal, value)
                        .setSpell(ctx.calculatedSpellData.getSpell())
                        .build();

                    restore.Activate();
                    com.robertx22.age_of_exile.uncommon.testing.SpellAuditProbe.diag(String.format(
                        java.util.Locale.ROOT,
                        "diag_heal value=%d target=%s before=%.2f after=%.2f max=%.2f",
                        value, t.getClass()
                            .getSimpleName(), before, t.getHealth(), t.getMaxHealth()));

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public MapHolder create(ValueCalculation calc) {
        MapHolder dmg = new MapHolder();
        dmg.type = GUID();
        dmg.put(VALUE_CALCULATION, calc);
        return dmg;
    }

    @Override
    public String GUID() {
        return "restore_health";
    }
}
