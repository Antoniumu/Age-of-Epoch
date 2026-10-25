package com.robertx22.age_of_exile.database.data.spells.components;

import com.robertx22.age_of_exile.database.data.exile_effects.ExileEffect;
import com.robertx22.age_of_exile.database.data.spells.SetAdd;
import com.robertx22.age_of_exile.database.data.spells.components.actions.AggroAction;
import com.robertx22.age_of_exile.database.data.spells.components.actions.ExileEffectAction.GiveOrTake;
import com.robertx22.age_of_exile.database.data.spells.components.actions.SummonProjectileAction;
import com.robertx22.age_of_exile.database.data.spells.components.actions.vanity.ParticleInRadiusAction;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.database.data.value_calc.ValueCalculation;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.uncommon.enumclasses.AttackType;
import com.robertx22.age_of_exile.uncommon.enumclasses.Elements;
import com.robertx22.age_of_exile.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.age_of_exile.uncommon.utilityclasses.DashUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.EntityFinder;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import com.robertx22.forge_compat.registries.ForgeRegistries;

import java.util.HashMap;

import static com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.EXILE_POTION_ID;
import static com.robertx22.age_of_exile.database.data.spells.map_fields.MapField.POTION_ID;

import net.minecraft.core.registries.BuiltInRegistries;
public class MapHolder {

    public String type;

    private HashMap<String, Object> map = new HashMap<>();

    public boolean has(MapField f) {
        return map.containsKey(f.GUID());
    }

    public <T> MapHolder put(MapField<T> field, T t) {
        if (field == MapField.VALUE_CALCULATION) {
            this.map.put(field.GUID(), ((ValueCalculation) t).GUID());
            return this;
        }
        this.map.put(field.GUID(), t);
        return this;
    }

    public <T> T get(MapField<T> field) {
        if (field == MapField.VALUE_CALCULATION) {
            return (T) ExileDB.ValueCalculations()
                .get((String) map.get(field.GUID()));
        }
        return (T) map.get(field.GUID());
    }

    public ExileEffect getExileEffect() {
        return ExileDB.ExileEffects()
            .get(get(EXILE_POTION_ID));
    }

    public AttackType getDmgEffectType() {
        return AttackType.valueOf(get(MapField.DMG_EFFECT_TYPE));
    }

    public MobEffect getPotion() {
        return BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse(get(POTION_ID)));
    }

    public Elements getElement() {
        return Elements.valueOf(get(MapField.ELEMENT));
    }

    public DashUtils.Way getPushWay() {
        return DashUtils.Way.valueOf(get(MapField.PUSH_WAY));
    }

    public AggroAction.Type getAggro() {
        return AggroAction.Type.valueOf(get(MapField.AGGRO_TYPE));
    }

    public GiveOrTake getPotionAction() {
        return GiveOrTake.valueOf(get(MapField.POTION_ACTION));
    }

    public SummonProjectileAction.ShootWay getOrDefault(SummonProjectileAction.ShootWay way) {
        String f = getOrDefault(MapField.SHOOT_DIRECTION, "");
        if (!f.isEmpty() && SummonProjectileAction.ShootWay.valueOf(f) != null) {
            return SummonProjectileAction.ShootWay.valueOf(f);
        } else {
            return way;
        }
    }

    public SummonProjectileAction.PositionSource getOrDefault(SummonProjectileAction.PositionSource way) {
        String f = getOrDefault(MapField.POS_SOURCE, "");
        if (!f.isEmpty() && SummonProjectileAction.PositionSource.valueOf(f) != null) {
            return SummonProjectileAction.PositionSource.valueOf(f);
        } else {
            return way;
        }
    }

    /**
     * Частица из данных заклинания — уже в виде опций, готовых к отрисовке.
     *
     * <p>Раньше здесь был жёсткий каст к {@link SimpleParticleType}. В MC 26.3
     * часть частиц (например {@code item_slime}, {@code item_snowball},
     * {@code dripping_water}, {@code falling_water}, {@code block}) — это типы с
     * параметром, и каст падал с {@code ClassCastException} прямо в касте
     * заклинания: клиент отменял каст, а игрок видел «умение не работает» без
     * единого партикла. Теперь параметрические типы заменяются визуально
     * близкой простой частицей — каст не падает, эффект видно.
     */
    public ParticleOptions getParticle() {
        String raw = get(MapField.PARTICLE_TYPE);
        ParticleType<?> type = raw == null || raw.isEmpty()
            ? null : BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(raw));

        if (type == null) {
            return ParticleTypes.CRIT;
        }
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        return ParticleFallback.simple(type);
    }

    public Block getBlock() {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(get(MapField.BLOCK)));
    }

    public SoundEvent getSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(Identifier.parse(get(MapField.SOUND)));
    }

    public EntityFinder.SelectionType getSelectionType() {
        return EntityFinder.SelectionType.valueOf(get(MapField.SELECTION_TYPE));
    }

    public SetAdd getSetAdd() {
        return SetAdd.valueOf(get(MapField.SET_ADD));
    }

    public ParticleInRadiusAction.Shape getParticleShape() {
        String str = get(MapField.PARTICLE_SHAPE);

        if (str != null && !str.isEmpty()) {
            return ParticleInRadiusAction.Shape.valueOf(str);
        } else {
            return ParticleInRadiusAction.Shape.CIRCLE;
        }
    }

    public AllyOrEnemy getEntityPredicate() {
        return AllyOrEnemy.valueOf(get(MapField.ENTITY_PREDICATE));
    }

    public <T> T getOrDefault(MapField<T> field, T defa) {
        return (T) map.getOrDefault(field.GUID(), defa);
    }

}
