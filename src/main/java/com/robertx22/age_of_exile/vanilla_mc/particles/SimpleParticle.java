package com.robertx22.age_of_exile.vanilla_mc.particles;

import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class SimpleParticle extends SingleQuadParticle {
    private final SpriteSet spriteProvider;

    private SimpleParticle(int maxage, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet spriteProvider) {
        super(world, x, y, z, spriteProvider.first());
        this.spriteProvider = spriteProvider;
        this.lifetime = maxage;
        this.gravity = 0.008F;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.setSpriteFromAge(spriteProvider);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.yd -= (double) this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.setSpriteFromAge(this.spriteProvider);
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;

        int maxAge;

        public Factory(int age, SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
            this.maxAge = age;
        }

        @Override
        public Particle createParticle(SimpleParticleType defaultParticleType, ClientLevel clientWorld, double d, double e, double f, double g, double h, double i, RandomSource random) {
            return new SimpleParticle(maxAge, clientWorld, d, e, f, g, h, i, this.spriteProvider);
        }
    }
}
