package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.library_of_exile.packets.particles.ParticleEnum;
import com.robertx22.library_of_exile.packets.particles.ParticlePacketData;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class ParticleUtils {
    public static void spawnDefaultSlashingWeaponParticles(Entity en) {
        SoundUtils.playSound(en, com.robertx22.forge_compat.common.CompatHelper.unwrapSound(SoundEvents.PLAYER_ATTACK_SWEEP), 1, 1);

        if (en instanceof Player) {
            Player p = (Player) en;
            // MC 26.3: Player.sweepAttack() was removed, spawn the sweep particles directly
            p.level().addParticle(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                p.getX(), p.getBoundingBox().maxY, p.getZ(), 0, 0, 0);
        }
    }

    public static void spawn(ParticleOptions type, Level world, Vec3 vec, Vec3 mot) {
        world.addParticle(type, vec.x, vec.y, vec.z, mot.x, mot.y, mot.z);
    }

    public static void spawn(ParticleOptions type, Level world, Vec3 vec) {
        world.addParticle(type, vec.x, vec.y, vec.z, 0, 0, 0);
    }

    public static void spawn(ParticleOptions particleData, Level world, double x, double y, double z, double xSpeed,
                             double ySpeed, double zSpeed) {
        world.addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);

    }

    public static void spawnParticles(ParticleType particle, Level world, BlockPos pos, int amount) {

        ParticleEnum.sendToClients(pos, world, new ParticlePacketData(pos, ParticleEnum.AOE).radius(1)
            .type(particle)
            .amount(amount));

    }

}
