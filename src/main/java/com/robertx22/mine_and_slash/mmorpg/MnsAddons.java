package com.robertx22.mine_and_slash.mmorpg;

import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.forge_compat.fml.ModList;


public class MnsAddons {
    public static boolean isObeliskLoaded() {
        return ModList.get().isLoaded("ancient_obelisks");
    }

    public static boolean isHarvestLoaded() {
        return ModList.get().isLoaded(Ref.Harvest.MODID);
    }
}
