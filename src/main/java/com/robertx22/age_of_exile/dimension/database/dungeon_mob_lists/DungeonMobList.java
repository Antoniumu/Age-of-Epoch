package com.robertx22.age_of_exile.dimension.database.dungeon_mob_lists;

import com.robertx22.age_of_exile.database.registry.ExileRegistryTypes;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

public class DungeonMobList implements JsonExileRegistry<DungeonMobList>, IAutoGson<DungeonMobList> {
    public static DungeonMobList SERIALIZER = new DungeonMobList();

    public String id = "";

    public List<WeightedMobEntry> mobs = new ArrayList<>();
    public List<WeightedMobEntry> bosses = new ArrayList<>();

    @Override
    public Class<DungeonMobList> getClassForSerialization() {
        return DungeonMobList.class;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.DUNGEON_MOB_LIST;
    }

    @Override
    public String GUID() {
        return id;
    }

    public LivingEntity spawnBoss(ServerLevel world, BlockPos pos) {
        WeightedMobEntry random = RandomUtils.weightedRandom(bosses);

        EntityType type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(random.id));

        LivingEntity en = (LivingEntity) type.create(world, EntitySpawnReason.MOB_SUMMONED);

        Load.Unit(en)
            .setRarity(IRarity.BOSS_ID);

        setup(en, pos, world);

        return en;
    }

    public EntityType getRandomMob() {
        EntityType type = null;
        try {
            WeightedMobEntry random = RandomUtils.weightedRandom(mobs);
            type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(random.id));
        } catch (Exception e) {
            e.printStackTrace();
            return net.minecraft.world.entity.EntityTypes.ZOMBIE;
        }

        return type;
    }

    public LivingEntity spawnMob(ServerLevel world, EntityType type, BlockPos pos) {

        try {

            LivingEntity en = (LivingEntity) type.create(world, EntitySpawnReason.MOB_SUMMONED);

            setup(en, pos, world);

            return en;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void setup(LivingEntity en, BlockPos pos, ServerLevel world) {

        en.setPos(pos.getX(), pos.getY(), pos.getZ());

        if (en instanceof Mob) {
            Mob mob = (Mob) en;

            mob.setPersistenceRequired();

            mob.finalizeSpawn(
                world,
                world.getCurrentDifficultyAt(pos),
                EntitySpawnReason.MOB_SUMMONED,
                null);

        }

        world.addFreshEntityWithPassengers(en);

    }

}
