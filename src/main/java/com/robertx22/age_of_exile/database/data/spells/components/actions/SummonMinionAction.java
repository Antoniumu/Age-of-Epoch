package com.robertx22.age_of_exile.database.data.spells.components.actions;

import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.summoning.MinionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.Collection;

/**
 * Призыв миньона навыком: нежить или зверь (ID вида из MinionUtils.Kind).
 * Лимиты по количеству, срок жизни и характеристики задаёт MinionUtils.
 */
public class SummonMinionAction extends SpellAction {

    public SummonMinionAction() {
        super(Arrays.asList(MapField.ENTITY_NAME));
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {
        if (!(ctx.caster instanceof Player owner)) {
            return;
        }
        String kindId = data.get(MapField.ENTITY_NAME);
        MinionUtils.Kind kind = MinionUtils.Kind.fromId(kindId);
        if (kind == null) {
            return;
        }
        BlockPos pos = owner.blockPosition().offset(2, 0, 0);
        if (kind == MinionUtils.Kind.UNDEAD) {
            MinionUtils.summonUndead(owner, pos);
        } else {
            MinionUtils.summonBeast(owner, kind, pos);
        }
    }

    public MapHolder create(String kindId) {
        MapHolder d = new MapHolder();
        d.type = GUID();
        d.put(MapField.ENTITY_NAME, kindId);
        return d;
    }

    @Override
    public String GUID() {
        return "summon_minion";
    }
}
