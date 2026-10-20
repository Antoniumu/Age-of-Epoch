package com.robertx22.age_of_exile.dimension.dungeon_data;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.components.forge.BaseProvider;
import com.robertx22.library_of_exile.components.forge.BaseStorage;
import com.robertx22.library_of_exile.components.forge.ICommonCap;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.CapabilityInject;
import com.robertx22.forge_compat.event.AttachCapabilitiesEvent;
import com.robertx22.forge_compat.eventbus.api.SubscribeEvent;
import com.robertx22.forge_compat.fml.common.Mod;

@Mod.EventBusSubscriber
public class WorldDungeonCap implements ICommonCap {

    public static final ResourceLocation RESOURCE = ResourceLocation.fromNamespaceAndPath(SlashRef.MODID, "dungeon");

    @Mod.EventBusSubscriber
    public static class EventHandler {
        @SubscribeEvent
        public static void onEntityConstruct(AttachCapabilitiesEvent<Level> event) {
            event.addCapability(RESOURCE, new Provider(event.getObject()));
        }
    }

    public static class Storage implements BaseStorage<WorldDungeonCap> {

    }

    public static class Provider extends BaseProvider<WorldDungeonCap, Level> {
        public Provider(Level owner) {
            super(owner);
        }

        @Override
        public WorldDungeonCap newDefaultImpl(Level owner) {
            return new WorldDungeonCap(owner);
        }

        @Override
        public Capability<WorldDungeonCap> dataInstance() {
            return Data;
        }
    }

    @CapabilityInject(WorldDungeonCap.class)
    public static Capability<WorldDungeonCap> Data = null;

    private static final String LOC = "dungeon_data";

    Level world;
    public WorldDungeonsData data = new WorldDungeonsData();

    public WorldDungeonCap(Level world) {
        this.world = world;
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        this.data = LoadSave.Load(WorldDungeonsData.class, new WorldDungeonsData(), nbt, LOC);
        if (data == null) {
            data = new WorldDungeonsData();
        }
    }

    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = new CompoundTag();
        LoadSave.Save(data, nbt, LOC);
        return nbt;
    }

}