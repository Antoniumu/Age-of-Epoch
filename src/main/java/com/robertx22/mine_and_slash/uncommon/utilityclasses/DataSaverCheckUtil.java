package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import net.minecraft.world.item.ItemStack;
import com.robertx22.forge_compat.fml.ModList;

import java.util.function.Supplier;

public class DataSaverCheckUtil {
    public static boolean checkForDataSaver(String saverGUID, ItemStack itemStack){
        if (com.robertx22.forge_compat.common.NbtStackCompat.hasTag(itemStack)) {
            return !com.robertx22.forge_compat.common.NbtStackCompat.getTag(itemStack).getString(saverGUID).isEmpty();
        }
        return false;

    }
}
