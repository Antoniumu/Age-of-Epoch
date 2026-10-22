package com.robertx22.mine_and_slash.capability.world;

import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.components.ICap;
import com.robertx22.library_of_exile.dimension.MapDataFinder;
import com.robertx22.library_of_exile.dimension.MapDimensionInfo;
import com.robertx22.library_of_exile.utils.LoadSave;
import com.robertx22.mine_and_slash.maps.MapData;
import com.robertx22.mine_and_slash.maps.MnsMapDataHolder;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.CapabilityManager;
import com.robertx22.forge_compat.common.capabilities.CapabilityToken;
import com.robertx22.forge_compat.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldData implements ICap {


    public static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(SlashRef.MODID, "world");
    public static Capability<WorldData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    transient final LazyOptional<WorldData> supp = LazyOptional.of(() -> this);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == INSTANCE) {
            return supp.cast();
        }
        return LazyOptional.empty();

    }

    public static WorldData get(Level level) {
        if (level.isClientSide()) {
            return new WorldData(level);
        }

        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(level.getServer().overworld(), INSTANCE).orElse(new WorldData(level));
    }


    private static final String MAP = "mapdata";

    transient Level level;

    public MnsMapDataHolder map = new MnsMapDataHolder();


    public WorldData(Level level) {
        this.level = level;
    }


    @Override
    public CompoundTag serializeNBT() {

        CompoundTag nbt = new CompoundTag();

        LoadSave.Save(map, nbt, MAP);

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        this.map = loadOrBlank(MnsMapDataHolder.class, new MnsMapDataHolder(), nbt, MAP, new MnsMapDataHolder());

    }

    public static <OBJ> OBJ loadOrBlank(Class theclass, OBJ newobj, CompoundTag nbt, String loc, OBJ blank) {
        try {
            OBJ data = LoadSave.Load(theclass, newobj, nbt, loc);
            if (data == null) {
                return blank;
            } else {
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blank;
    }

    @Override
    public String getCapIdForSyncing() {
        return "world_data";
    }

    public static MapDataFinder<MapData> DATA_GETTER = new MapDataFinder<>() {
        @Override
        public MapData getData(Pos pos) {
            return get(pos.level).map.getData(this.getInfo().structure, pos.pos);
        }

        @Override
        public MapDimensionInfo getInfo() {
            return DungeonMain.MAP;
        }

    };
}
