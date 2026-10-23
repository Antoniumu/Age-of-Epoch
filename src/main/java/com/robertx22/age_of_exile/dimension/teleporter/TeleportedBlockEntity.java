package com.robertx22.age_of_exile.dimension.teleporter;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlockEntities;
import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.BaseModificationStation;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class TeleportedBlockEntity extends BaseModificationStation {

    public TeleportedBlockEntity() {
        super(SlashBlockEntities.TELEPORTER.get(), 0);
    }

    public TeleportedBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        super(SlashBlockEntities.TELEPORTER.get(), 0, pos, state);
    }

    public TeleporterData data = new TeleporterData();

    @Override
    public boolean modifyItem(int number, Player player) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        super.save(nbt);
        LoadSave.Save(data, nbt, "td");
        return nbt;
    }

    @Override
    public void load(BlockState state, CompoundTag nbt) {
        try {
            super.load(state, nbt);
            this.data = LoadSave.Load(TeleporterData.class, new TeleporterData(), nbt, "td");

            if (data == null) {
                data = new TeleporterData();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tick() {
        super.tick();

    }

}
