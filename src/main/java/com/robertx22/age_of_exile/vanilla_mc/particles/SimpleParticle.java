package com.robertx22.age_of_exile.vanilla_mc.particles;

import net.minecraft.client.particle.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

// copied from BubblePopParticle
public class SimpleParticle extends SpriteTexturedParticle {
    private final IAnimatedSprite spriteProvider;

    private SimpleParticle(int maxage, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, IAnimatedSprite spriteProvider) {
        super(world, x, y, z);
        this.spriteProvider = spriteProvider;
        this.lifetime = maxage;
        this.gravity = 0.008F;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.setSpriteFromAge(spriteProvider);
    }

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

    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<SimpleParticleType> {
        private final IAnimatedSprite spriteProvider;

        int maxAge;

        public Factory(int age, IAnimatedSprite spriteProvider) {
            this.spriteProvider = spriteProvider;
            this.maxAge = age;
        }

        public Particle createParticle(SimpleParticleType defaultParticleType, ClientLevel clientWorld, double d, double e, double f, double g, double h, double i) {
            return new SimpleParticle(maxAge, clientWorld, d, e, f, g, h, i, this.spriteProvider);
        }
    }
}
