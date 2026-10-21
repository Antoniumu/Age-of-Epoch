package com.robertx22.age_of_exile.database.data.spells.components.actions;

import com.robertx22.age_of_exile.database.data.spells.components.MapHolder;
import com.robertx22.age_of_exile.database.data.spells.components.Spell;
import com.robertx22.age_of_exile.database.data.spells.map_fields.MapField;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.age_of_exile.database.data.spells.spell_classes.SpellUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class SummonAtSightAction extends SpellAction {

    public SummonAtSightAction() {
        super(Arrays.asList(MapField.ENTITY_NAME, MapField.PROJECTILE_ENTITY, MapField.LIFESPAN_TICKS, MapField.HEIGHT));
    }

    @Override
    public void tryActivate(Collection<LivingEntity> targets, SpellCtx ctx, MapHolder data) {

        Optional<EntityType<?>> projectile = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.parse(data.get(MapField.PROJECTILE_ENTITY)));

        Double distance = data.getOrDefault(MapField.DISTANCE, 10D);
        Double height = data.getOrDefault(MapField.HEIGHT, 10D);

        HitResult ray = ctx.caster.pick(distance, 0.0F, false);
        Vec3 pos = ray.getLocation();

        double spawnY = pos.y + height;

        // when the ray misses (aiming at the sky / past max range), the raw point can
        // end up far above the caster and the spell lands somewhere unnoticeable.
        // Clamp it to caster eye level so it always drops on terrain in view.
        if (ray.getType() == HitResult.Type.MISS) {
            spawnY = Math.min(spawnY, ctx.caster.getEyeY() + height);
        }

        // if the spawn point ended up inside terrain (e.g. aimed at a cliff face),
        // lift it up until there's open air, otherwise the summoned entity
        // instantly expires inside the ground and the spell looks like it fizzled
        int tries = 0;
        while (tries < 32 && !ctx.world.isEmptyBlock(BlockPos.containing(pos.x, spawnY, pos.z))) {
            spawnY += 1.0D;
            tries++;
        }

        // still buried inside terrain (deep mountainside etc.):
        // fall back to just above the column's surface so it lands visibly on top
        if (!ctx.world.isEmptyBlock(BlockPos.containing(pos.x, spawnY, pos.z))) {
            BlockPos column = BlockPos.containing(pos.x, 0, pos.z);
            spawnY = ctx.world.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()) + 2;
        }

        Entity en = projectile.get()
            .create(ctx.world, EntitySpawnReason.MOB_SUMMONED);
        SpellUtils.initSpellEntity(en, ctx.caster, ctx.calculatedSpellData, data);
        en.setPos(pos.x, spawnY, pos.z);

        ctx.caster.level().addFreshEntity(en);

    }

    public MapHolder create(EntityType type, Double lifespan, Double height) {
        MapHolder c = new MapHolder();
        c.put(MapField.LIFESPAN_TICKS, lifespan);
        c.put(MapField.GRAVITY, false);
        c.put(MapField.ENTITY_NAME, Spell.DEFAULT_EN_NAME);
        c.put(MapField.HEIGHT, height);
        c.put(MapField.PROJECTILE_ENTITY, EntityType.getKey(type)
            .toString());
        c.type = GUID();
        this.validate(c);
        return c;
    }

    @Override
    public String GUID() {
        return "summon_at_sight";
    }
}
