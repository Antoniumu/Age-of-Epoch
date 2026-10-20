package com.robertx22.age_of_exile.dimension.spawner;

import com.robertx22.age_of_exile.dimension.SpawnUtil;
import com.robertx22.age_of_exile.dimension.database.dungeon_mob_lists.DungeonMobList;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlockEntities;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.forge_compat.blocks.ITickableBlockEntity;
import com.robertx22.library_of_exile.packets.particles.ParticleEnum;
import com.robertx22.library_of_exile.packets.particles.ParticlePacketData;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ModSpawnerBlockEntity extends BlockEntity implements ITickableBlockEntity {

    public static int DEFAULT_SPAWNS = 10;

    private static int MAX_NEARBY_ENTITIES = 5;
    private static int spawnRange = 16;

    public ModSpawnerBlockEntity() {
        this(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
    }

    public ModSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(SlashBlockEntities.SPAWNER.get(), pos, state);
    }

    int ticks = 0;
    public int spawnsLeft = DEFAULT_SPAWNS;
    public boolean spawnAllAtOnce = false;
    public int requiredPlayerRange = 16;

    @Override
    public void tick() {

        if (spawnsLeft < 1) {
            if (!level.isClientSide()) {
                this.level().removeBlock(worldPosition, false);
                return;
            }
        }

        ticks++;

        if (ticks % 60 == 0) {
            if (!level.isClientSide()) {
                if (isPlayerInRange()) {
                    if (WorldUtils.isMapWorldClass(level)) {
                        int entities = level.getEntitiesOfClass(LivingEntity.class, (new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), (worldPosition.getX() + 1), (worldPosition.getY() + 1), (worldPosition.getZ() + 1))).inflate(this.spawnRange))
                            .size();
                        if (!spawnAllAtOnce && entities > MAX_NEARBY_ENTITIES) {
                            return;
                        }

                        DungeonMobList list = Load.dungeonData(level).data.get(worldPosition).data.getMobList();

                        if (list != null) {
                            int spawns = RandomUtils.RandomRange(1, 4);

                            if (spawns > spawnsLeft || spawnAllAtOnce) {
                                spawns = spawnsLeft;
                            }

                            this.spawnsLeft -= spawns;

                            List<BlockPos> positions = new ArrayList<>();
                            for (int x = -4; x < 4; x++) {
                                for (int z = -4; z < 4; z++) {
                                    positions.add(worldPosition.offset(x, 0, z));
                                    positions.add(worldPosition.offset(x, 1, z));
                                }
                            }
                            ParticleEnum.sendToClients(worldPosition, level,
                                new ParticlePacketData(worldPosition, ParticleEnum.AOE).radius(0.5F)
                                    .type(ParticleTypes.FLAME)
                                    .motion(new Vec3(0, 0, 0))
                                    .amount(10));

                            ParticleEnum.sendToClients(worldPosition, level,
                                new ParticlePacketData(worldPosition, ParticleEnum.AOE).radius(0.5F)
                                    .type(ParticleTypes.SMOKE)
                                    .motion(new Vec3(0, 0, 0))
                                    .amount(10));

                            for (int i = 0; i < spawns; i++) {

                                while (spawns > 0 && !positions.isEmpty()) {

                                    BlockPos blockPos = RandomUtils.randomFromList(positions);

                                    EntityType type = list.getRandomMob();

                                    if (SpawnUtil.canPlaceMob(level, type, blockPos)) {
                                        list.spawnMob((ServerLevel) level, type, blockPos);
                                        spawns--;
                                    }

                                    positions.remove(blockPos);
                                }

                            }
                        }

                    }
                }
            }
        }

    }

    private boolean isPlayerInRange() {
        BlockPos blockPos = this.getBlockPos();

        return this.getLevel()
            .hasNearbyAlivePlayer((double) blockPos.getX() + 0.5D, (double) blockPos.getY() + 0.5D, (double) blockPos.getZ() + 0.5D, (double) this.requiredPlayerRange);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag nbt = new CompoundTag();
        this.save(nbt);
        output.store("AgeOfEpochData", CompoundTag.CODEC, nbt);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("AgeOfEpochData", CompoundTag.CODEC).ifPresent(nbt -> this.load(this.getBlockState(), nbt));
    }

    public CompoundTag save(CompoundTag nbt) {
        nbt.putInt("ticks", ticks);
        nbt.putInt("spawnsLeft", spawnsLeft);
        nbt.putInt("range", requiredPlayerRange);
        nbt.putBoolean("at_once", spawnAllAtOnce);
        return nbt;
    }

    public void load(BlockState state, CompoundTag nbt) {
        try {
            ticks = nbt.getIntOr("ticks", 0);
            spawnsLeft = nbt.getIntOr("spawnsLeft", DEFAULT_SPAWNS);
            requiredPlayerRange = nbt.getIntOr("range", 16);
            spawnAllAtOnce = nbt.getBooleanOr("at_once", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
