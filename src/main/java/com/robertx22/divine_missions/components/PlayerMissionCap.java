package com.robertx22.divine_missions.components;

import com.robertx22.forge_compat.common.capabilities.CapabilityProvider;

import com.robertx22.divine_missions.components.data.PlayerMissionData;
import com.robertx22.divine_missions.components.data.PlayerRepudationData;
import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.database.db_types.God;
import com.robertx22.divine_missions.database.db_types.ReputationLevel;
import com.robertx22.library_of_exile.components.forge.BaseProvider;
import com.robertx22.library_of_exile.components.forge.BaseStorage;
import com.robertx22.library_of_exile.components.forge.IPlayerCap;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.CapabilityInject;
import com.robertx22.forge_compat.event.AttachCapabilitiesEvent;
import com.robertx22.forge_compat.eventbus.api.SubscribeEvent;
import com.robertx22.forge_compat.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber
public class PlayerMissionCap implements IPlayerCap {

    public static final ResourceLocation RESOURCE = ResourceLocation.fromNamespaceAndPath(Ref.MODID, "player_missions");

    @CapabilityInject(PlayerMissionCap.class)
    public static Capability<PlayerMissionCap> Data = null;

    public static PlayerMissionCap get(Player entity) {
        return CapabilityProvider.get(entity, Data)
            .orElse(null);
    }

    @Override
    public String getCapIdForSyncing() {
        return "player_missions";
    }

    @Mod.EventBusSubscriber
    public static class EventHandler {
        @SubscribeEvent
        public static void onEntityConstruct(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(RESOURCE, new Provider((Player) event.getObject()));
            }
        }
    }

    public static class Provider extends BaseProvider<PlayerMissionCap, Player> {
        public Provider(Player owner) {
            super(owner);
        }

        @Override
        public PlayerMissionCap newDefaultImpl(Player owner) {
            return new PlayerMissionCap(owner);
        }

        @Override
        public Capability<PlayerMissionCap> dataInstance() {
            return Data;
        }
    }

    public static class Storage implements BaseStorage<PlayerMissionCap> {

    }

    public PlayerMissionData missionData = new PlayerMissionData();
    public PlayerRepudationData repData = new PlayerRepudationData();

    public Player owner;

    public PlayerMissionCap(Player en) {
        this.owner = en;
    }

    static String MISSIN_DATA_LOC = "mission_data";
    static String REP_LOC = "rep";

    public ReputationLevel getReputationLevel(God god) {

        List<ReputationLevel> can = new ArrayList<>();

        int rep = repData.getReputation(god);

        MissionsDB.ReputationLevels()
            .getList()
            .forEach(x -> {
                if (x.rep_needed >= 0) {
                    if (rep >= x.rep_needed) {
                        can.add(x);
                    }
                } else {
                    if (rep <= x.rep_needed) {
                        can.add(x);
                    }
                }
            });

        if (can.isEmpty()) {
            can.add(MissionsDB.ReputationLevels()
                .get("neutral"));
        }

        ReputationLevel repLevel = can.stream()
            .max(Comparator.comparingInt(x -> x.rank))
            .get();

        return repLevel;
    }

    public void addReputation(int amount, God god) {

        ReputationLevel level = getReputationLevel(god);

        repData.addReputation(amount, god);
        ReputationLevel after = getReputationLevel(god);

        if (level.GUID()
            .equals(after.GUID()) == false) {

            owner.displayClientMessage(Component.translatable("divine_missions.new_reputation_message",
                after.getTranslatable()
                    .withStyle(after.getFormat()),
                god.getTranslatable()
                    .withStyle(god.getFormat())), false);
        }
    }

    @Override
    public CompoundTag saveToNBT() {

        CompoundTag nbt = new CompoundTag();
        try {
            if (missionData != null) {
                LoadSave.Save(missionData, nbt, MISSIN_DATA_LOC);
            }
            if (repData != null) {
                LoadSave.Save(repData, nbt, REP_LOC);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nbt;

    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        try {
            this.missionData = LoadSave.Load(PlayerMissionData.class, new PlayerMissionData(), nbt, MISSIN_DATA_LOC);
            if (missionData == null) {
                missionData = new PlayerMissionData();
            }
            this.repData = LoadSave.Load(PlayerRepudationData.class, new PlayerRepudationData(), nbt, REP_LOC);
            if (repData == null) {
                repData = new PlayerRepudationData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
