package com.robertx22.infinite_dungeons.components.data;

import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.core.BlockPos;

@Storable
public class PlayerData {

    @Store
    public String current_dun_uuid = "";
    @Store
    public String orig_gamemode = "";
    @Store
    public BlockPos control_block_pos = BlockPos.ZERO;

    @Store
    public BuyHistoryData buy_history = new BuyHistoryData();

}
