package com.robertx22.age_of_exile.dimension.teleporter.portal_block;

import com.robertx22.age_of_exile.dimension.teleporter.PortalBlocKData;
import com.robertx22.age_of_exile.mmorpg.registers.common.SlashBlockEntities;
import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.BaseModificationStation;
import com.robertx22.library_of_exile.utils.LoadSave;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;

public class PortalBlockEntity extends BaseModificationStation {

    public PortalBlocKData data = new PortalBlocKData();

    public PortalBlockEntity() {
        super(SlashBlockEntities.PORTAL.get(), 0);
    }

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
        if (data != null) {
            LoadSave.Save(data, nbt, "teldata");
        }
        return nbt;
    }

    @Override
    public void load(BlockState state, CompoundTag nbt) {
        try {
            super.load(state, nbt);
            this.data = LoadSave.Load(PortalBlocKData.class, new PortalBlocKData(), nbt, "teldata");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

