package com.robertx22.age_of_exile.uncommon.effectdatas.rework.action;

import com.robertx22.age_of_exile.database.data.stats.Stat;
import com.robertx22.age_of_exile.saveclasses.unit.StatData;
import com.robertx22.age_of_exile.summoning.MinionUtils;
import com.robertx22.age_of_exile.uncommon.effectdatas.DamageEvent;
import com.robertx22.age_of_exile.uncommon.effectdatas.EffectEvent;
import com.robertx22.age_of_exile.uncommon.effectdatas.EventBuilder;
import com.robertx22.age_of_exile.uncommon.enumclasses.AttackType;
import com.robertx22.age_of_exile.uncommon.enumclasses.PlayStyle;
import com.robertx22.age_of_exile.uncommon.enumclasses.WeaponTypes;
import com.robertx22.age_of_exile.uncommon.interfaces.EffectSides;
import com.robertx22.age_of_exile.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.age_of_exile.uncommon.utilityclasses.EntityFinder;
import com.robertx22.age_of_exile.uncommon.utilityclasses.HealthUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Взрыв трупа: убитый враг наносит физический урон всем врагам вокруг себя в размере процента от
 * своего здоровья (ветка некроманта древа «Божественный»).
 * <p>
 * Значение стата — процент от здоровья трупа. Урон идёт от хозяина, поэтому проходит через обычную
 * цепочку урона мода (криты, пробивание, статы на урон по области и т.д.).
 */
public class ExplodeCorpseAction extends StatEffect {

    public static float RADIUS = 4;

    public ExplodeCorpseAction() {
        super("explode_corpse", "explode_corpse");
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
        if (dead == null) {
            return;
        }

        float perc = data.getValue();

        if (perc <= 0) {
            return;
        }

        float dmg = HealthUtils.getMaxHealth(dead) * perc / 100F;

        if (dmg <= 0) {
            return;
        }

        List<LivingEntity> targets = EntityFinder.start(source, LivingEntity.class, dead.blockPosition())
            .finder(EntityFinder.SelectionType.RADIUS)
            .searchFor(AllyOrEnemy.enemies)
            .radius(RADIUS)
            .build();

        for (LivingEntity en : targets) {

            if (en == null || en == dead) {
                continue;
            }
            if (MinionUtils.isMinion(en)) {
                continue; // свои прислужники не страдают
            }

            DamageEvent dmgEvent = EventBuilder.ofDamage(source, en, dmg)
                .setupDamage(AttackType.spell, WeaponTypes.none, PlayStyle.magic)
                .build();

            dmgEvent.Activate();
        }
    }

    @Override
    public Class<? extends StatEffect> getSerClass() {
        return ExplodeCorpseAction.class;
    }
}
