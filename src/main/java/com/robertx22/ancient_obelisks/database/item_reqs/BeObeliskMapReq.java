package com.robertx22.ancient_obelisks.database.item_reqs;

import com.robertx22.ancient_obelisks.item.ObeliskItemNbt;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import net.minecraft.world.entity.player.Player;

public class BeObeliskMapReq extends BeItemReq {
    public BeObeliskMapReq(String id) {
        super("be_obelisk_map", id, "Must be an Obelisk Map Item");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return BeObeliskMapReq.class;
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        return ObeliskItemNbt.OBELISK_MAP.has(obj.stack);
    }
}
