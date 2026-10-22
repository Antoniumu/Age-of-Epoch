package com.robertx22.mine_and_slash.capability.chunk;

import com.robertx22.library_of_exile.components.ICap;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import com.robertx22.forge_compat.common.capabilities.Capability;
import com.robertx22.forge_compat.common.capabilities.CapabilityManager;
import com.robertx22.forge_compat.common.capabilities.CapabilityToken;
import com.robertx22.forge_compat.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChunkCap implements ICap {


    public static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(SlashRef.MODID, "chunk");
    public static Capability<ChunkCap> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    transient final LazyOptional<ChunkCap> supp = LazyOptional.of(() -> this);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == INSTANCE) {
            return supp.cast();
        }
        return LazyOptional.empty();

    }


    transient LevelChunk chunk;

    public boolean generatedMobs = false;
    public boolean generatedTerrain = false;

    public ChunkCap(LevelChunk chunk) {
        this.chunk = chunk;
    }


    List<CompoundTag> savedMobs = new ArrayList<>();

    public void tryLoadMobs(Level world) {
        if (!savedMobs.isEmpty()) {
            try {

                var saved = savedMobs.stream().toList();
                savedMobs.clear();
                mobIds.clear();

                for (CompoundTag nbt : saved) {
                    
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public List<UUID> mobIds = new ArrayList<>();

    public void trySaveMob(LivingEntity en) {

        if (en instanceof Player) {
            return;
        }

        if (savedMobs.size() > 30) {
            if (MMORPG.RUN_DEV_TOOLS) {
                ExileLog.get().warn("Saved too many mobs in 1 chunk, stopping just in case");
            }
            return;
        }
        if (mobIds.contains(en.getUUID())) {
            return;
        }
        mobIds.add(en.getUUID());

        CompoundTag nbt = new CompoundTag();

        savedMobs.add(nbt);
    }

    @Override
    public CompoundTag serializeNBT() {

        CompoundTag nbt = new CompoundTag();

        try {
            nbt.putBoolean("gen", generatedTerrain);
            nbt.putBoolean("genmobs", generatedMobs);

            nbt.putInt("mobs", savedMobs.size());

            for (int i = 0; i < savedMobs.size(); i++) {
                nbt.put(i + "", savedMobs.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ExileLog.get().warn("Mob Unloading/Loading Error");
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        try {
            this.generatedTerrain = nbt.getBooleanOr("gen", false);
            this.generatedMobs = nbt.getBooleanOr("genmobs", false);

            int mobs = nbt.getIntOr("mobs", 0);

            this.savedMobs = new ArrayList<>();
            this.mobIds = new ArrayList<>();

            for (int i = 0; i < mobs; i++) {
                var mobnbt = nbt.getCompoundOrEmpty(i + "");
                java.util.UUID id = java.util.UUID.randomUUID();
                if (id != null) {
                    savedMobs.add(mobnbt);
                    mobIds.add(id);
                }
                // todo test if game still freezes
            }
        } catch (Exception e) {
            e.printStackTrace();
            ExileLog.get().warn("Mob Unloading/Loading Error");
        }

    }

    @Override
    public String getCapIdForSyncing() {
        return "chunk_data";
    }

}
