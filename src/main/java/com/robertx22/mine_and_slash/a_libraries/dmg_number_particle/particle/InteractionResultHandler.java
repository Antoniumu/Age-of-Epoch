package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.DamageParticleRenderer;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.NumberUtils;
import com.robertx22.mine_and_slash.vanilla_mc.packets.interaction.IParticleSpawnMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

public class InteractionResultHandler {

    public enum ExileParticleType {
        DAMAGE(new IParticleSpawnMaterial.DamageInformation(null, null, false)),
        NULLIFIED_DAMAGE(IParticleSpawnMaterial.Type.DODGE),
        HEAL(new IParticleSpawnMaterial.HealNumber(0.0f));
        public final IParticleSpawnMaterial target;

        ExileParticleType(IParticleSpawnMaterial target) {
            this.target = target;
        }
    }

    public enum ClientSpawnStrategy {
        DEFAULT((info, entity) -> {
            if (Minecraft.getInstance().level == null) return;
            var mat = (IParticleSpawnMaterial.DamageInformation) info;
            ImmutableMap<Elements, Float> dmgMap = mat.getDmgMap();
            boolean crit = mat.isCrit();
            for (Map.Entry<Elements, Float> entry : dmgMap.entrySet()) {
                Float damage = entry.getValue();
                if (damage.intValue() > 0) {
                    double x = entity.getRandomX(0.5D);
                    double y = entity.getEyeY();
                    double z = entity.getRandomZ(0.5D);
                    String txt = (entry.getKey() != null ? entry.getKey().format + entry.getKey().icon : "") + NumberUtils.format(damage) + (crit ? "!" : "");
                    DamageParticleRenderer.spawn(Component.literal(txt), x, y, z, 0.0D, 0.05D, 0.0D, crit ? 1.35F : 1.0F, crit);
                }
            }
        },
        (type, entity) -> {
            if (Minecraft.getInstance().level == null) return;
            var mat = (IParticleSpawnMaterial.Type) type;
            DamageParticleRenderer.spawn(mat.text.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD), entity.getRandomX(0.5D), entity.getEyeY(), entity.getRandomZ(0.5D), 0.0D, 0.045D, 0.0D, 1.0F, false);
            if (ClientOnly.getPlayer() != null) {
                ClientOnly.getPlayer().level().playLocalSound(entity.blockPosition(), mat.sound, SoundSource.PLAYERS, 1, 1.5F, true);
            }
        },
        (type, entity) -> {
            if (Minecraft.getInstance().level == null) return;
            var mat = (IParticleSpawnMaterial.HealNumber) type;
            DamageParticleRenderer.spawn(Component.literal("+" + NumberUtils.format(mat.number())).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), entity.getRandomX(0.5D), entity.getEyeY(), entity.getRandomZ(0.5D), 0.0D, 0.05D, 0.0D, 1.05F, false);
        }),

        IN_A_ROW((info, entity) -> {
            if (Minecraft.getInstance().level == null) return;
            var mat = (IParticleSpawnMaterial.DamageInformation) info;
            ImmutableMap<Elements, Float> dmgMap = mat.getDmgMap();
            boolean crit = mat.isCrit();
            StringBuilder sb = new StringBuilder();
            dmgMap.entrySet().stream().sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal())).forEachOrdered(x -> {
                if (sb.length() > 0) sb.append(" ");
                sb.append(x.getKey().format).append(x.getKey().icon).append(NumberUtils.format(x.getValue()));
            });
            if (crit) sb.append("!");
            Random random = new Random();
            DamageParticleRenderer.spawn(Component.literal(sb.toString()), entity.getX() + random.nextDouble(-0.5d, 0.5d), entity.getEyeY() + random.nextDouble(-0.2d, 0.5d), entity.getZ(), 0.0D, 0.035D, 0.0D, crit ? 1.35F : 1.0F, crit);
        },
        (type, entity) -> {
            if (Minecraft.getInstance().level == null) return;
            var mat = (IParticleSpawnMaterial.Type) type;
            DamageParticleRenderer.spawn(mat.text.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD), entity.getX(), entity.getEyeY(), entity.getZ(), 0.0D, 0.035D, 0.0D, 1.0F, false);
        },
        (type, entity) -> {
            if (Minecraft.getInstance().level == null) return;
            var mat = (IParticleSpawnMaterial.HealNumber) type;
            DamageParticleRenderer.spawn(Component.literal("+" + NumberUtils.format(mat.number())).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), entity.getX(), entity.getEyeY(), entity.getZ(), 0.0D, 0.035D, 0.0D, 1.05F, false);
        });

        public final BiConsumer<IParticleSpawnMaterial, Entity> damageStrategy;
        public final BiConsumer<IParticleSpawnMaterial, Entity> nullifiedDamageStrategy;
        public final BiConsumer<IParticleSpawnMaterial, Entity> healStrategy;

        ClientSpawnStrategy(BiConsumer<IParticleSpawnMaterial, Entity> damageStrategy, BiConsumer<IParticleSpawnMaterial, Entity> nullifiedDamageStrategy, BiConsumer<IParticleSpawnMaterial, Entity> healStrategy) {
            this.damageStrategy = damageStrategy;
            this.nullifiedDamageStrategy = nullifiedDamageStrategy;
            this.healStrategy = healStrategy;
        }
    }
}
