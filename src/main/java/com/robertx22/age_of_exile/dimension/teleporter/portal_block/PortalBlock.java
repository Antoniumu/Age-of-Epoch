package com.robertx22.age_of_exile.dimension.teleporter.portal_block;

import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.OpaqueBlock;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import com.robertx22.library_of_exile.utils.VanillaReg;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import com.robertx22.forge_compat.material.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.Random;

public class PortalBlock extends OpaqueBlock implements EntityBlock {

    public PortalBlock() {
        super(Properties.of()
            .noCollision()
            .strength(5F, 2));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalBlockEntity(pos, state);
    }

    public BlockEntity newBlockEntity(BlockGetter world) {
        return new PortalBlockEntity();
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextInt(100) == 0) {
            world.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);
        }

        for (int i = 0; i < 4; ++i) {
            double d = (double) pos.getX() + random.nextDouble();
            double e = (double) pos.getY() + random.nextDouble();
            double f = (double) pos.getZ() + random.nextDouble();
            double g = ((double) random.nextFloat() - 0.5D) * 0.5D;
            double h = ((double) random.nextFloat() - 0.5D) * 0.5D;
            double j = ((double) random.nextFloat() - 0.5D) * 0.5D;
            int k = random.nextInt(2) * 2 - 1;
            if (!world.getBlockState(pos.west())
                .is(this) && !world.getBlockState(pos.east())
                .is(this)) {
                d = (double) pos.getX() + 0.5D + 0.25D * (double) k;
                g = (double) (random.nextFloat() * 2.0F * (float) k);
            } else {
                f = (double) pos.getZ() + 0.5D + 0.25D * (double) k;
                j = (double) (random.nextFloat() * 2.0F * (float) k);
            }

            world.addParticle(ParticleTypes.PORTAL, d, e, f, g, h, j);
        }

    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity,
                             net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean flag) {
        try {

            if (world.isClientSide()) {
                return;
            }

            if (entity instanceof Player) {

                RPGPlayerData maps = Load.playerRPGData((Player) entity);

                if (entity.getDeltaMovement().y > 0) {
                    maps.maps.ticksinPortal = 0; // jumping bugs it somehow
                    return;
                }

                if (WorldUtils.isMapWorldClass(world)) {
                    if (maps.maps.ticksinPortal < 40) {
                        maps.maps.ticksinPortal++;
                    } else {
                        maps.maps.ticksinPortal = 0;
                        BlockPos p = Load.playerRPGData((Player) entity).maps.tel_pos.above();

                        TeleportUtils.teleport((ServerPlayer) entity, p, Identifier.parse(maps.maps.tp_b_dim));

                        SoundUtils.playSound(entity, SoundEvents.PORTAL_TRAVEL, world.getRandom().nextFloat() * 0.4F + 0.8F, 0.25F);
                        return;
                    }
                }

                PortalBlockEntity be = (PortalBlockEntity) world.getBlockEntity(pos);

                if (entity instanceof ServerPlayer) {
                    if (!entity.isPassenger() && !entity.isVehicle() && !((ServerPlayer) entity).isChangingDimension()) {

                        if (maps.maps.ticksinPortal < 40) {
                            maps.maps.ticksinPortal++;
                        } else {

                            if (be.data.dungeonPos == BlockPos.ZERO) {
                                return;
                            } else {
                                maps.maps.ticksinPortal = 0;
                                maps.maps.tel_pos = be.data.tpbackpos;
                                maps.maps.tp_b_dim = VanillaReg.getId(entity.level())
                                    .toString();

                                TeleportUtils.teleport((ServerPlayer) entity, be.data.dungeonPos, be.data.dungeonType.DIMENSION_ID);
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
