package com.robertx22.age_of_exile.database.data.spells.components;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Замена параметрических частиц на простые.
 *
 * <p>Часть частиц в MC — это тип <b>с параметром</b> ({@code BlockParticleOption},
 * {@code ItemParticleOption}, {@code DustParticleOptions}). Такие типы нельзя
 * отрисовать «как есть»: нужен блок/предмет/цвет. В заклинаниях же хранится
 * только идентификатор частицы, поэтому такие записи раньше падали с
 * {@code ClassCastException} и убивали каст целиком.
 *
 * <p>Здесь подбирается визуально близкая простая частица. Это не «костыль ради
 * галочки»: падение каста — баг, а близкая по смыслу частица сохраняет задумку
 * (брызги для воды, искры для предметов). Каждая подмена пишется в лог один раз —
 * чтобы автор заклинания видел, что параметрическую частицу лучше заменить.
 */
public final class ParticleFallback {

    private static final Logger LOGGER = LoggerFactory.getLogger("AgeOfEpoch-Particles");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private ParticleFallback() {
    }

    public static SimpleParticleType simple(ParticleType<?> type) {
        Identifier key = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        String path = key == null ? "" : key.getPath();

        if (REPORTED.add(path)) {
            LOGGER.warn("частица '{}' требует параметра — рисуем ближайшую простую", path);
        }

        if (path.contains("water") || path.contains("rain") || path.contains("splash")) {
            return ParticleTypes.SPLASH;
        }
        if (path.contains("lava")) {
            return ParticleTypes.LAVA;
        }
        if (path.contains("item")) {
            // предметные частицы — «осколки»: искры читаются так же
            return ParticleTypes.CRIT;
        }
        if (path.contains("dust") || path.contains("block") || path.contains("shriek")) {
            return ParticleTypes.POOF;
        }
        return ParticleTypes.CRIT;
    }
}
