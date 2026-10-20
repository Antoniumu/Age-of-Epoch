package com.robertx22.infinite_dungeons.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import com.robertx22.forge_compat.client.IRendersAsItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import com.robertx22.forge_compat.fml.network.NetworkHooks;

@OnlyIn(value = Dist.CLIENT, _interface = IRendersAsItem.class)
public class TickingTNTEntity extends Entity implements IRendersAsItem {

    public TickingTNTEntity(EntityType type, Level world) {
        super(type, world);
    }

    @Override
    public void tick() {

        level.addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);

        if (!this.level.isClientSide) {

            if (this.tickCount > 20 * 5) {
                level.explode(this, getX(), getY(), getZ(), 3, Explosion.Mode.NONE);
                this.kill();
            } else {

            }

        }

        super.tick();
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {

    }

    @Override
    public Packet<?> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity) {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.TNT);
    }
}
