package com.robertx22.library_of_exile.components;

import com.robertx22.forge_compat.common.capabilities.CapabilityProvider;

import com.robertx22.library_of_exile.components.forge.BaseProvider;
import com.robertx22.library_of_exile.components.forge.BaseStorage;
import com.robertx22.library_of_exile.components.forge.ICommonCap;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.CapabilityInject;
import com.robertx22.forge_compat.event.AttachCapabilitiesEvent;
import com.robertx22.forge_compat.eventbus.api.SubscribeEvent;
import com.robertx22.forge_compat.fml.common.Mod;

@Mod.EventBusSubscriber
public class EntityInfoComponent {
    public static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(Ref.MODID, "entity_info");

    @CapabilityInject(IEntityInfo.class)
    public static Capability<IEntityInfo> Data = null;

    public static IEntityInfo get(LivingEntity entity) {
        return CapabilityProvider.get(entity, Data)
            .orElse(null);
    }

    private static final String DMG_STATS = "dmg_stats";
    private static final String SPAWN_POS = "spawn_pos";
    private static final String SPAWN_REASON = "spawn";

    @Mod.EventBusSubscriber
    public static class EventHandler {
        @SubscribeEvent
        public static void onEntityConstruct(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof LivingEntity) {
                event.addCapability(RESOURCE, new Provider((LivingEntity) event.getObject()));
            }
        }
    }

    public interface IEntityInfo extends ICommonCap, BaseStorage<IEntityInfo> {

        EntityDmgStatsData getDamageStats();

        BlockPos getSpawnPos();

        void spawnInit(Entity en);

        MySpawnReason getSpawnReason();

        void setSpawnReasonOnCreate(EntitySpawnReason reason);
    }

    public static class Provider extends BaseProvider<IEntityInfo, LivingEntity> {
        public Provider(LivingEntity owner) {
            super(owner);
        }

        @Override
        public IEntityInfo newDefaultImpl(LivingEntity owner) {
            return new DefaultImpl(owner);
        }

        @Override
        public Capability<IEntityInfo> dataInstance() {
            return Data;
        }
    }

    public static class Storage implements BaseStorage<IEntityInfo> {

    }

    public static class DefaultImpl implements IEntityInfo {

        EntityDmgStatsData dmgStats = new EntityDmgStatsData();

        private BlockPos spawnPos;
        public MySpawnReason spawnReason = null;

        public LivingEntity owner;

        public DefaultImpl(LivingEntity en) {
            this.owner = en;
        }

        @Override
        public BlockPos getSpawnPos() {
            if (spawnPos != null) {
                return spawnPos;
            }
            return BlockPos.ZERO;
        }

        @Override
        public void spawnInit(Entity entity) {
            if (spawnPos == null || (spawnPos.getX() == 0 && spawnPos.getY() == 0 && spawnPos.getZ() == 0)) {
                this.spawnPos = entity.blockPosition();
            }
        }

        @Override
        public MySpawnReason getSpawnReason() {
            return spawnReason == null ? MySpawnReason.NATURAL : spawnReason;
        }

        @Override
        public void setSpawnReasonOnCreate(EntitySpawnReason reason) {
            if (spawnReason == null) {
                spawnReason = MySpawnReason.get(reason);
            }
        }

        @Override
        public EntityDmgStatsData getDamageStats() {
            return dmgStats;
        }

        @Override
        public CompoundTag saveToNBT() {
            CompoundTag nbt = new CompoundTag();
            try {
                if (dmgStats != null) {
                    LoadSave.Save(dmgStats, nbt, DMG_STATS);
                }
                if (spawnPos != null) {
                    nbt.putLong(SPAWN_POS, spawnPos.asLong());
                }

                nbt.putString(SPAWN_REASON, getSpawnReason().name());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return nbt;
        }

        @Override
        public void loadFromNBT(CompoundTag nbt) {

            try {
                this.dmgStats = LoadSave.Load(EntityDmgStatsData.class, new EntityDmgStatsData(), nbt, DMG_STATS);
                if (dmgStats == null) {
                    dmgStats = new EntityDmgStatsData();
                }
                this.spawnPos = BlockPos.of(nbt.getLongOr(SPAWN_POS, 0L));

                String res = nbt.getStringOr(SPAWN_REASON, "");
                if (res != null && !res.isEmpty()) {
                    this.spawnReason = MySpawnReason.valueOf(res);
                } else {
                    this.spawnReason = MySpawnReason.NATURAL;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
