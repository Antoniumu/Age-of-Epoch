package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.style.IParticleRenderStrategy;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public abstract class ExileInteractionResultParticle {
    protected IParticleRenderStrategy strategy;
    protected final Vec3 originalPos;
    public double x, y, z, xo, yo, zo, xd, yd, zd;
    public int age = 0;
    public int lifetime = 25;
    public float gravity = 0.0f;

    protected ExileInteractionResultParticle(ClientLevel level, double x, double y, double z, IParticleRenderStrategy strategy) {
        this.x = x; this.y = y; this.z = z;
        this.xo = x; this.yo = y; this.zo = z;
        this.strategy = strategy;
        this.originalPos = new Vec3(x, y, z);
    }

    public IParticleRenderStrategy getStrategy() { return this.strategy; }
    public Vec3 getPos() { return new Vec3(this.x, this.y, this.z); }
    public Vec3 getOriginalPosition() { return this.originalPos; }
    public double getPrevX() { return this.xo; }
    public double getPrevY() { return this.yo; }
    public double getPrevZ() { return this.zo; }
    public int getAge() { return this.age; }
    public int getLiftTime() { return this.lifetime; }
    public void setLiftTime(int life) { this.lifetime = life; }
    public void setLifetime(int life) { this.lifetime = life; }
    public void setGravity(float gravity) { this.gravity = gravity; }
    public void setParticleSpeed(double dx, double dy, double dz) { this.xd = dx; this.yd = dy; this.zd = dz; }
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        this.x += this.xd; this.y += this.yd; this.z += this.zd;
        this.age++;
    }
    public void render(VertexConsumer vc, Camera cam, float partialTick) {}
}
