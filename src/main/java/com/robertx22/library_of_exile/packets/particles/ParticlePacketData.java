package com.robertx22.library_of_exile.packets.particles;

import com.robertx22.library_of_exile.utils.RGB;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
@Storable
public class ParticlePacketData {

    private ParticlePacketData() {

    }

    public static ParticlePacketData empty() {
        return new ParticlePacketData();
    }

    @Store
    public double x = 1;
    @Store
    public double y = 1;
    @Store
    public double z = 1;

    @Store
    public boolean isVecPos = false;

    @Store
    public double mx = 1;
    @Store
    public double my = 1;
    @Store
    public double mz = 1;

    @Store
    public ParticleEnum type;

    @Store
    public float radius = 1;

    @Store
    public int amount = 1;

    @Store
    public RGB color;

    @Store
    public String particleID;

    public <T extends ParticleOptions> ParticleOptions getParticleType() {
        ParticleType<T> particleType = (ParticleType<T>) BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(particleID));

        if (particleType instanceof ParticleOptions) {
            return (ParticleOptions) particleType;
        } else return ParticleTypes.CRIT;
    }

    public ParticlePacketData motion(Vec3 v) {
        mx = v.x;
        my = v.y;
        mz = v.z;
        return this;
    }

    public ParticlePacketData type(ParticleType type) {
        this.particleID = BuiltInRegistries.PARTICLE_TYPE.getKey(type)
            .toString();
        return this;
    }

    public ParticlePacketData amount(int num) {
        this.amount = num;
        return this;
    }

    public ParticlePacketData radius(double rad) {
        this.radius = (float) rad;
        return this;
    }

    public ParticlePacketData radius(float rad) {
        this.radius = rad;
        return this;
    }

    public ParticlePacketData color(RGB color) {
        this.color = color;
        return this;
    }

    public Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    public BlockPos getBlockPos() {
        return new BlockPos((int) x, (int) y, (int) z);
    }

    public ParticlePacketData(Vec3 pos, ParticleEnum type) {
        x = pos.x();
        y = pos.y();
        z = pos.z();
        this.isVecPos = true;
        this.type = type;
    }

    public ParticlePacketData(BlockPos pos, ParticleEnum type) {
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
        this.type = type;
    }
}
