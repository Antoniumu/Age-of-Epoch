package com.robertx22.dungeon_realm.structure;

import com.google.gson.JsonSyntaxException;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.dimension.MapDataFinder;
import com.robertx22.library_of_exile.dimension.MapDimensionInfo;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.CapabilityManager;
import com.robertx22.forge_compat.common.capabilities.CapabilityToken;
import com.robertx22.forge_compat.common.capabilities.ICapabilityProvider;
import com.robertx22.forge_compat.common.util.INBTSerializable;
import com.robertx22.forge_compat.common.util.LazyOptional;
import com.robertx22.forge_compat.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DungeonMapCapability implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public Level world;

    public static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(DungeonMain.MODID, "world_data");
    public static Capability<DungeonMapCapability> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    transient final LazyOptional<DungeonMapCapability> supp = LazyOptional.of(() -> this);

    public DungeonMapCapability(Level world) {
        this.world = world;
    }

    public static DungeonMapCapability get(Level entity) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(entity.getServer().overworld(), INSTANCE).orElse(new DungeonMapCapability(entity));
    }

    public static DungeonMapCapability getFromServer() {
        return get(ServerLifecycleHooks.getCurrentServer().overworld());
    }


    public DungeonWorldData data = new DungeonWorldData();

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == INSTANCE) {
            return supp.cast();
        }
        return LazyOptional.empty();

    }

    @Override
    public CompoundTag serializeNBT() {
        var nbt = new CompoundTag();

        try {

            LoadSave.Save(data, nbt, "data");

        } catch (Exception e) {
            e.printStackTrace();
        }


        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        try {
            this.data = LoadSave.loadOrBlank(DungeonWorldData.class, new DungeonWorldData(), nbt, "data", new DungeonWorldData());


        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

    }

    public static MapDataFinder<DungeonMapData> DATA_GETTER = new MapDataFinder<>() {
        @Override
        public DungeonMapData getData(Pos pos) {
            return get(pos.level).data.data.getData(this.getInfo().structure, pos.pos);
        }

        @Override
        public MapDimensionInfo getInfo() {
            return DungeonMain.MAP;
        }

    };
}
