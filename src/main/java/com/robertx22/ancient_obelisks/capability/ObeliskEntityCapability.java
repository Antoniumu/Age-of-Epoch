package com.robertx22.ancient_obelisks.capability;

import com.google.gson.JsonSyntaxException;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.CapabilityManager;
import com.robertx22.forge_compat.common.capabilities.CapabilityToken;
import com.robertx22.forge_compat.common.capabilities.ICapabilityProvider;
import com.robertx22.forge_compat.common.util.INBTSerializable;
import com.robertx22.forge_compat.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ObeliskEntityCapability implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public LivingEntity en;

    public static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(ObelisksMain.MODID, "entity");
    public static Capability<ObeliskEntityCapability> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    transient final LazyOptional<ObeliskEntityCapability> supp = LazyOptional.of(() -> this);

    public ObeliskEntityCapability(LivingEntity en) {
        this.en = en;
    }

    public static ObeliskEntityCapability get(LivingEntity entity) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(entity, INSTANCE).orElse(new ObeliskEntityCapability(entity));
    }

    public ObeliskEntityData data = new ObeliskEntityData();

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
            this.data = LoadSave.loadOrBlank(ObeliskEntityData.class, new ObeliskEntityData(), nbt, "data", new ObeliskEntityData());

        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

    }
}
