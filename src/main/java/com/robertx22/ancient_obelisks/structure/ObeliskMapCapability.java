package com.robertx22.ancient_obelisks.structure;

import com.robertx22.ancient_obelisks.main.ObelisksMain;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ObeliskMapCapability implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public Level world;

    public static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(ObelisksMain.MODID, "world_data");
    public static Capability<ObeliskMapCapability> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    transient final LazyOptional<ObeliskMapCapability> supp = LazyOptional.of(() -> this);

    public ObeliskMapCapability(Level world) {
        this.world = world;
    }

    public static ObeliskMapCapability get(Level entity) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(entity.getServer().overworld(), INSTANCE).orElse(new ObeliskMapCapability(entity));
    }


    public ObeliskWorldData data = new ObeliskWorldData();

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

            this.data = LoadSave.loadOrBlank(ObeliskWorldData.class, new ObeliskWorldData(), nbt, "data", new ObeliskWorldData());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
