package com.robertx22.infinite_dungeons.entity;

import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import com.robertx22.forge_compat.client.IRendersAsItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@OnlyIn(value = Dist.CLIENT, _interface = IRendersAsItem.class)
public class TickingTNTEntity extends Entity implements IRendersAsItem {

    public TickingTNTEntity(EntityType<? extends TickingTNTEntity> type, Level world) {
        super(type, world);
    }

    @Override
    public void tick() {
        this.level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);

        if (this.level() instanceof ServerLevel sl) {
            if (this.tickCount > 20 * 5) {
                sl.explode(this, getX(), getY(), getZ(), 3, Level.ExplosionInteraction.NONE);
                this.kill(sl);
            }
        }

        super.tick();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.TNT);
    }
}
