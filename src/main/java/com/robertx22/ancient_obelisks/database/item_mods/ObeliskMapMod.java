package com.robertx22.ancient_obelisks.database.item_mods;

import com.robertx22.ancient_obelisks.item.ObeliskItemMapData;
import com.robertx22.ancient_obelisks.item.ObeliskItemNbt;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import net.minecraft.world.item.ItemStack;

public abstract class ObeliskMapMod extends ItemModification {

    public ObeliskMapMod(String serializer, String id) {
        super(serializer, id);
    }

    public abstract void modifyGear(ItemStack stack, ObeliskItemMapData data, ItemModificationResult r);

    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {

        var data = ObeliskItemNbt.OBELISK_MAP.loadFrom(stack.stack);

        if (data != null) {
            modifyGear(stack.stack, data, r);
        }
        ObeliskItemNbt.OBELISK_MAP.saveTo(stack.stack, data);

    }

}
