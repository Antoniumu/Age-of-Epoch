package com.robertx22.mine_and_slash.database.data.spells.summons.entity;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.library_of_exile.utils.geometry.MyPosition;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.database.data.spells.components.ProjectileCastHelper;
import com.robertx22.mine_and_slash.database.data.spells.entities.AutoAimingProj;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashEntities;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class SummonEntity extends TamableAnimal implements RangedAttackMob {

    public SummonEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }


    protected AbstractArrow getArrow(ItemStack pArrowStack, float pVelocity) {
        return ProjectileUtil.getMobArrow(this, pArrowStack, pVelocity, null);


    }

    Goal aggroGoal = null;

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {

            if (this.tickCount == 20) {
                var owner = getOwner();
                if (owner instanceof Player p) {
                    if (Load.player(p).config.isConfigEnabled(PlayerConfigData.Config.AGGRESSIVE_SUMMONS)) {
                        if (aggroGoal == null) {
                            aggroGoal = new NearestAttackableTargetGoal<>(this, Monster.class, false);
                            this.targetSelector.addGoal(2, aggroGoal);
                        }
                    }
                }
            }
        }
    }

    public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor) {

        if (true) {
            autoAimingRangedAttack(pTarget);
        } else {

            ItemStack itemstack = new ItemStack(net.minecraft.world.item.Items.ARROW);
            AbstractArrow abstractarrow = this.getArrow(itemstack, pDistanceFactor);
            
                
            double d0 = pTarget.getX() - this.getX();
            double d1 = pTarget.getY(0.3333333333333333D) - abstractarrow.getY();
            double d2 = pTarget.getZ() - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            abstractarrow.shoot(d0, d1 + d3 * (double) 0.2F, d2, 5, 0);
            this.playSound(com.robertx22.forge_compat.common.CompatHelper.unwrapSound(SoundEvents.SKELETON_SHOOT), 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.level().addFreshEntity(abstractarrow);
        }
    }


    /**
     * Launches a Wither skull toward (par2, par4, par6)
     */
    private void autoAimingRangedAttack(LivingEntity target) {

        SoundUtils.playSound(this, com.robertx22.forge_compat.common.CompatHelper.unwrapSound(SoundEvents.ARROW_SHOOT), 1, 0.2F);


        AutoAimingProj en = SlashEntities.AUTO_AIMING_SKELETON_SKULL.get().create(level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);

        en.setOwner(this);

        en.setPosRaw(getX(), getEyeY(), getZ());

        en.setDeltaMovement(ProjectileCastHelper.positionToVelocity(new MyPosition(getEyePosition()), new MyPosition(target.getEyePosition())));

        en.target = target;
        en.speed = 2;

        this.level().addFreshEntity(en);
    }

    public boolean usesMelee() {
        return true;
    }

    public boolean usesRanged() {
        return false;
    }

    @Override
    protected void registerGoals() {


        if (usesMelee()) {
            this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        }
        if (usesRanged()) {
            
        }

        
        this.goalSelector.addGoal(7, new FollowOwnerGoal(this, 1.0D, 6.0F, 1.0F));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        //this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (x, sl) -> canAttack(x)));
    }

    public Entity focusEntity = null;

    @Override
    public boolean canAttack(LivingEntity pTarget) {
        LivingEntity owner = getOwner();

        if (owner == null) {
            return false;
        }
        if (!pTarget.isAlive()) {
            return false;
        }
        if (!AllyOrEnemy.summonShouldAttack.is(owner, pTarget)) {
            return false;
        }
        // aggro the focus target, otherwise find something else
        if (focusEntity != null && focusEntity.isAlive() && isInAggroRadius((LivingEntity) focusEntity)) {
            return focusEntity == pTarget;
        } else {
            return isInAggroRadius(pTarget);
        }

    }

    // todo test this
    private boolean isInAggroRadius(LivingEntity target) {
        var aggroRadius = Load.Unit(this).summonedPetData.aggro_radius;

        int distance = (int) target.distanceTo(this);

        return aggroRadius >= distance;

    }

    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    protected boolean canRide(Entity pVehicle) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }

    // MC 26.3: Animal.isFood(ItemStack) is abstract
    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        return false;
    }
}
