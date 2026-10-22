package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.style.IParticleRenderStrategy;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

public abstract class ExileInteractionResultParticle extends SingleQuadParticle {
    protected IParticleRenderStrategy strategy;
    protected final Vec3 originalPos;

    protected ExileInteractionResultParticle(ClientLevel level, double x, double y, double z, IParticleRenderStrategy strategy) {
        super(level, x, y, z, (TextureAtlasSprite) null);
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
    public void render(VertexConsumer vc, Camera cam, float partialTick) {}

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }
}
