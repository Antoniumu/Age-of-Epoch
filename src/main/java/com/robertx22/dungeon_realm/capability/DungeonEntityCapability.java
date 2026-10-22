package com.robertx22.dungeon_realm.capability;

import com.robertx22.dungeon_realm.main.DungeonMain;
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

public class DungeonEntityCapability implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public LivingEntity en;

    public static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(DungeonMain.MODID, "entity");
    public static Capability<DungeonEntityCapability> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    transient final LazyOptional<DungeonEntityCapability> supp = LazyOptional.of(() -> this);

    public DungeonEntityCapability(LivingEntity en) {
        this.en = en;
    }

    public static DungeonEntityCapability get(LivingEntity entity) {
        return com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get(entity, INSTANCE).orElse(new DungeonEntityCapability(entity));
    }

    public DungeonEntityData data = new DungeonEntityData();

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
            this.data = LoadSave.loadOrBlank(DungeonEntityData.class, new DungeonEntityData(), nbt, "data", new DungeonEntityData());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
