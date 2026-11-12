package com.robertx22.age_of_exile.mmorpg.registers.common;

import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import net.minecraft.core.particles.SimpleParticleType;

public class SlashParticles {

    /**
     * «Свечение прилива» — частица Вознесённых классов (3D-образы воды).
     * Спрайты: assets/mmorpg/textures/particle/tide_glow, декларация: particles/tide_glow.json,
     * клиентская фабрика: TideGlowParticle.registerFactory().
     */
    public static RegObj<SimpleParticleType> TIDE_GLOW;

    public static void init() {
        TIDE_GLOW = Def.particle("tide_glow", () -> new SimpleParticleType(false));
    }

}
