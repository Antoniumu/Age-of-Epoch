package com.robertx22.mine_and_slash.database.data.spells.components;

import com.robertx22.mine_and_slash.database.data.spells.components.selectors.AoeSelector;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellUtils;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.EntityFinder;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ProjectileCastHelper {

    LivingEntity caster;

    public boolean silent = false;
    public float apart = 3;
    public float shootSpeed = 1;
    public int projectilesAmount = 1;
    public float randomSpreadDegrees = 0f;
    public boolean gravity = true;
    EntityType projectile;
    CalculatedSpellData data;
    MapHolder holder;
    Vec3 pos;

    public CastType castType = CastType.SPREAD_OUT_IN_RADIUS;

    public enum CastType {
        SPREAD_OUT_IN_RADIUS, SPREAD_OUT_HORIZONTAL
    }

    public float pitch;

    public float yaw;

    public boolean fallDown = false;
    public boolean targetEnemy = false;

    SpellCtx ctx;

    public ProjectileCastHelper(SpellCtx ctx, Vec3 pos, MapHolder holder, LivingEntity caster, EntityType projectile, CalculatedSpellData data) {
        this.ctx = ctx;
        this.projectile = projectile;
        this.caster = caster;
        this.data = data;
        this.holder = holder;
        this.pos = pos;

        this.pitch = caster.getXRot();
        this.yaw = caster.getYRot();
    }

    public void cast() {

        if (data.data.getBoolean(EventData.BARRAGE)) {
            this.castType = CastType.SPREAD_OUT_HORIZONTAL;
        }

        Level world = caster.level();

        for (int i = 0; i < projectilesAmount; i++) {
            float addYaw = 0;
            Vec3 posAdd = new Vec3(0, 0, 0);

            if (projectilesAmount > 1) {
                float offset = i - (float)(projectilesAmount - 1) / 2;

                if (this.castType == CastType.SPREAD_OUT_IN_RADIUS) {
                    // total cone is apart * (projectilesAmount - 1) / projectilesAmount
                    addYaw = offset * apart / projectilesAmount;
                } else if (this.castType == CastType.SPREAD_OUT_HORIZONTAL) {
                    // 1m between each projectile
                    posAdd = getSideVelocity(caster).multiply(offset, offset, offset);
                }
            }

            // Apply random spread offsets to yaw and pitch
            float randomYawOffset = 0f;
            float randomPitchOffset = 0f;
            if (randomSpreadDegrees > 0f) {
                randomYawOffset = (float) ((Math.random() * 2 - 1) * randomSpreadDegrees);
                randomPitchOffset = (float) ((Math.random() * 2 - 1) * randomSpreadDegrees);
            }

            // copied from multishot crossbow code
            Vec3 vec31 = new Vec3(0, 1, 0);

            // Convert pitch and yaw (including offsets) to radians
            float totalPitch = pitch + randomPitchOffset;
            float totalYaw = yaw + addYaw + randomYawOffset;

            float pitchRad = (float) Math.toRadians(totalPitch);
            float yawRad = (float) Math.toRadians(totalYaw);

            // Calculate direction vector from pitch and yaw
            float x = -Mth.sin(yawRad) * Mth.cos(pitchRad);
            float y = -Mth.sin(pitchRad);
            float z = Mth.cos(yawRad) * Mth.cos(pitchRad);

            Vector3f finalVel = new Vector3f(x, y, z);

            //posAdd = new MyPosition(finalVel);

            AbstractArrow en = (AbstractArrow) projectile.create(world, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            SpellUtils.shootProjectile(pos.add(posAdd), en, ctx.getPositionEntity(), shootSpeed, pitch + randomPitchOffset, yaw + addYaw + randomYawOffset);
            SpellUtils.initSpellEntity(en, caster, data, holder);

            en.shoot(finalVel.x, finalVel.y, finalVel.z, shootSpeed, 1);

            if (fallDown) {
                en.setDeltaMovement(0, -1, 0);
            }

            en.setSilent(silent);

            if (targetEnemy) {

                BlockPos pos = en.blockPosition();

                EntityFinder.Setup<LivingEntity> finder = EntityFinder.start(caster, LivingEntity.class, pos)
                        .finder(EntityFinder.SelectionType.RADIUS)
                        .searchFor(AllyOrEnemy.enemies)
                        .predicate(e -> AoeSelector.canHit(ctx.getPos(), e))
                        .radius(15);


                LivingEntity target = finder.getClosest();

                if (target != null) {
                    Vec3 vel = positionToVelocity(new MyPosition(en.position()), new MyPosition(target.getEyePosition()));
                    vel = vel.multiply(shootSpeed, shootSpeed, shootSpeed);
                    //en.setDeltaMovement(vel);


                    en.shoot(vel.x, vel.y, vel.z, 1, 0);

                    caster.level().addFreshEntity(en);
                    break;
                }


            } else {
                caster.level().addFreshEntity(en);
            }
        }

    }

    public static Vec3 positionToVelocity(MyPosition current, MyPosition destination) {
        return destination.subtract(current).normalize();
    }

    public Vec3 getSideVelocity(Entity shooter) {
        float yaw = shooter.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
    }

}

