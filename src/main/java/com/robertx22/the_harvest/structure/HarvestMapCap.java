package com.robertx22.the_harvest.structure;

import com.robertx22.library_of_exile.utils.LoadSave;
import com.robertx22.the_harvest.main.HarvestMain;
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

public class HarvestMapCap implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public Level world;

    public static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(HarvestMain.MODID, "world_data");
    public static Capability<HarvestMapCap> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    transient final LazyOptional<HarvestMapCap> supp = LazyOptional.of(() -> this);

    public HarvestMapCap(Level world) {
        this.world = world;
    }

    public static HarvestMapCap get(Level entity) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(entity.getServer().overworld(), INSTANCE).orElse(new HarvestMapCap(entity));
    }

    public HarvestWorldData data = new HarvestWorldData();

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
            this.data = LoadSave.loadOrBlank(HarvestWorldData.class, new HarvestWorldData(), nbt, "data", new HarvestWorldData());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
