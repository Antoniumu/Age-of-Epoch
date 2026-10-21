package com.robertx22.world_of_exile.world_gen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Unbreakable cage left where a tower {@code [boss]} sign used to be.
 * A nearby player turns it into the tower boss.
 */
public class TowerBossMarkerBlock extends Block implements EntityBlock {

    public TowerBossMarkerBlock() {
        super(Properties.of()
            .strength(-1.0F, 3600000.0F)
            .sound(SoundType.METAL)
            .lightLevel(state -> 10)
            .noOcclusion());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TowerBossMarkerBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
            pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
            0.0D, 0.02D, 0.0D);
    }
}
