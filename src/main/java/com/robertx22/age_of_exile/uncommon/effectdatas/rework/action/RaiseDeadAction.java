package com.robertx22.age_of_exile.uncommon.effectdatas.rework.action;

import com.robertx22.age_of_exile.database.data.stats.Stat;
import com.robertx22.age_of_exile.saveclasses.unit.StatData;
import com.robertx22.age_of_exile.summoning.MinionUtils;
import com.robertx22.age_of_exile.uncommon.effectdatas.EffectEvent;
import com.robertx22.age_of_exile.uncommon.interfaces.EffectSides;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Шанс поднять убитого врага в виде нежити-прислужника (ветка некроманта древа «Божественный»).
 * <p>
 * Значение стата — шанс в процентах. Работает на событии убийства ({@code on_mob_kill}), поднимает
 * только враждебных существ и только если не превышен лимит
 * {@link com.robertx22.age_of_exile.database.data.stats.types.summoning.SummonStat#MAX_UNDEAD_MINIONS}.
 */
public class RaiseDeadAction extends StatEffect {

    public RaiseDeadAction() {
        super("raise_dead", "raise_dead");
    }

    @Override
    public void activate(EffectEvent event, EffectSides statSource, StatData data, Stat stat) {

        LivingEntity source = event.getSide(statSource);
        LivingEntity dead = event.target;

        if (!(source instanceof Player)) {
            return;
        }
        if (source.level()
            .isClientSide()) {
            return;
        }
        if (dead == null || !(dead instanceof Enemy)) {
            return;
        }

        float chance = data.getValue();

        if (chance <= 0) {
            return;
        }
        if (!RandomUtils.roll(chance)) {
            return;
        }

        MinionUtils.summonUndead((Player) source, dead.blockPosition());
    }

    @Override
    public Class<? extends StatEffect> getSerClass() {
        return RaiseDeadAction.class;
    }
}
