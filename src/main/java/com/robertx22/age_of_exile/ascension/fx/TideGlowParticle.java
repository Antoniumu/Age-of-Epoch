package com.robertx22.age_of_exile.ascension.fx;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Частица «свечение прилива» — мягкий объёмный блик живой воды.
 *
 * <p>Это не ванильная частица: свой спрайт-набор (3 кадра: сфера света, брызга,
 * искра), полупрозрачный слой, отрицательная гравитация (вода «всплывает»),
 * бар вращения через смену кадров и затухание в конце жизни. Из таких частиц
 * {@link TideShapeAction} строит настоящие 3D-образы — спирали, торы, воронки.
 */
public class TideGlowParticle extends SingleQuadParticle {

    private final SpriteSet sprites;
    private final float baseAlpha;

    protected TideGlowParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(world, x, y, z, sprites.first());
        this.sprites = sprites;
        this.lifetime = 22 + world.getRandom()
            .nextInt(16);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        // живая вода легче воздуха — частица медленно всплывает
        this.gravity = -0.0035F;
        this.quadSize = 0.16F + world.getRandom()
            .nextFloat() * 0.16F;
        this.baseAlpha = 0.0F; // задаётся мягким «вплытием» в первых тиках
        this.alpha = 0.0F;
        // аква-бирюza с лёгким сдвигом к призматическому
        double hue = world.getRandom()
            .nextDouble();
        this.rCol = (float) (0.35F + hue * 0.25F);
        this.gCol = (float) (0.85F + hue * 0.15F);
        this.bCol = 1.0F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // мягкое появление и затухание — «голограмма воды», а не искра
        int fadeIn = 5;
        int fadeOut = 8;
        float a;
        if (this.age < fadeIn) {
            a = (this.age / (float) fadeIn);
        } else if (this.age > this.lifetime - fadeOut) {
            a = Math.max(0.0F, (this.lifetime - this.age) / (float) fadeOut);
        } else {
            a = 1.0F;
        }
        this.alpha = a;

        // гидродинамика: сопротивление среды + лёгкая болтанка, как у пузырьков
        this.yd -= this.gravity; // gravity отрицательная => всплытие
        this.xd *= 0.92D;
        this.yd *= 0.92D;
        this.zd *= 0.92D;
        double wob = Math.sin((this.age + this.x * 7.0D) * 0.35D) * 0.006D;
        this.move(this.xd + wob, this.yd, this.zd - wob);

        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    /**
     * Фабрика для Fabric-регистрации клиентской частицы.
     */
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
            return new TideGlowParticle(world, x, y, z, vx, vy, vz, this.sprites);
        }
    }

    /**
     * Регистрирует клиентскую фабрику частицы. Вызывается один раз из ClientInit.
     */
    public static void registerFactory() {
        try {
            // FAPI 26.x: реестр провайдеров; FabricSpriteSet наследует SpriteSet, поэтому Factory::new подхватит
            net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance()
                .register(SlashParticles.TIDE_GLOW.get(), TideGlowParticle.Factory::new);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
