package com.robertx22.ancient_obelisks.block_entity;

import com.robertx22.ancient_obelisks.main.ObeliskEntries;
import com.robertx22.ancient_obelisks.structure.ObeliskMapCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.robertx22.forge_compat.fml.server.ServerLifecycleHooks;

public class ObeliskBE extends BlockEntity {


    public boolean gaveMap = false;
    public int x = -1;
    public int z = -1;

    public String currentWorldUUID = "";

    public boolean isActivated() {
        if (currentWorldUUID.isEmpty() || !currentWorldUUID.equals(ObeliskMapCapability.get(ServerLifecycleHooks.getCurrentServer().overworld()).data.data.uuid)) {
            return false;
        }
        return x != -1 || z != -1;

    }

    public void setGaveMap() {
        this.gaveMap = true;
        this.setChanged();
    }

    public ObeliskBE(BlockPos pPos, BlockState pBlockState) {
        super(ObeliskEntries.OBELISK_BE.get(), pPos, pBlockState);

    }

    protected void saveAdditionalLegacyTag(CompoundTag nbt) {
        // super.saveAdditional(nbt);
        nbt.putBoolean("gave", gaveMap);
        nbt.putInt("xp", x);
        nbt.putInt("zp", z);
        nbt.putString("uid", currentWorldUUID);

    }

    public void loadLegacyTag(CompoundTag pTag) {
        // MC 26.3 ValueInput bridge delegates here
        // super.load(pTag);
        this.gaveMap = pTag.getBooleanOr("gave", false);
        this.x = pTag.getIntOr("xp", 0);
        this.z = pTag.getIntOr("zp", 0);
        this.currentWorldUUID = pTag.getStringOr("uid", "");
    }


    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        input.read("aoe_be_nbt", CompoundTag.CODEC).ifPresent(__t -> this.loadLegacyTag(__t));
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag __nbt = new CompoundTag();
        this.saveAdditionalLegacyTag(__nbt);
        output.store("aoe_be_nbt", CompoundTag.CODEC, __nbt);
    }

}
