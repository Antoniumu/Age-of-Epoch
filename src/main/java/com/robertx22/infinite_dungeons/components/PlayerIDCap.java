package com.robertx22.infinite_dungeons.components;

import com.robertx22.forge_compat.common.capabilities.CapabilityProvider;

import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.infinite_dungeons.components.data.PlayerData;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.library_of_exile.components.forge.BaseProvider;
import com.robertx22.library_of_exile.components.forge.BaseStorage;
import com.robertx22.library_of_exile.components.forge.IPlayerCap;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.CapabilityInject;
import com.robertx22.forge_compat.event.AttachCapabilitiesEvent;
import com.robertx22.forge_compat.eventbus.api.SubscribeEvent;
import com.robertx22.forge_compat.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber
public class PlayerIDCap implements IPlayerCap {

    public static final ResourceLocation RESOURCE = ResourceLocation.fromNamespaceAndPath(MainID.MODID, "data");

    @CapabilityInject(PlayerIDCap.class)
    public static Capability<PlayerIDCap> Data = null;

    public static PlayerIDCap get(Player entity) {
        return CapabilityProvider.get(entity, Data)
            .orElse(null);
    }

    @Override
    public String getCapIdForSyncing() {
        return "infinite_dungeons_player_data";
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

    public static class Provider extends BaseProvider<PlayerIDCap, Player> {
        public Provider(Player owner) {
            super(owner);
        }

        @Override
        public PlayerIDCap newDefaultImpl(Player owner) {
            return new PlayerIDCap(owner);
        }

        @Override
        public Capability<PlayerIDCap> dataInstance() {
            return Data;
        }
    }

    public static class Storage implements BaseStorage<PlayerIDCap> {

    }

    public PlayerData data = new PlayerData();

    public Player owner;

    public PlayerIDCap(Player en) {
        this.owner = en;
    }

    public Optional<ControlBlockEntity> getControlBlock() {
        try {
            return Optional.of((ControlBlockEntity) this.owner.level.getBlockEntity(data.control_block_pos));
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean isInDungeon() {

        if (getControlBlock().isPresent()) {
            return getControlBlock().get()
                .isInsideDungeon(owner.blockPosition());
        }

        return false;

    }

    static String LOC = "rep";

    @Override
    public CompoundTag saveToNBT() {

        CompoundTag nbt = new CompoundTag();
        try {

            if (data != null) {
                LoadSave.Save(data, nbt, LOC);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nbt;

    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        try {
            this.data = LoadSave.Load(PlayerData.class, new PlayerData(), nbt, LOC);
            if (data == null) {
                data = new PlayerData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
