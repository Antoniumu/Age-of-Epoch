package com.robertx22.ancient_obelisks.database.item_reqs;

import com.robertx22.ancient_obelisks.item.ObeliskItemMapData;
import com.robertx22.ancient_obelisks.item.ObeliskItemNbt;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class ObeliskReq extends ItemRequirement {
    public ObeliskReq(String serializer, String id) {
        super(serializer, id);
    }

    public abstract boolean isValid(ItemStack gear, ObeliskItemMapData data);

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        if (!ObeliskItemNbt.OBELISK_MAP.has(obj.stack)) {
            return false;
        }

        return isValid(obj.stack, ObeliskItemNbt.OBELISK_MAP.loadFrom(obj.stack));
    }
}
