package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestItemMods extends ExileKeyHolder<ItemModification> {
    public static HarvestItemMods INSTANCE = new HarvestItemMods(HarvestMain.REGISTER_INFO);

    public HarvestItemMods(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    // public ExileKey<ItemModification, KeyInfo> ADD_WAVE = ExileKey.ofId(this, "add_wave", x -> new AddObeliskWaveMod(x.GUID(), new AddObeliskWaveMod.Data(1)));


    @Override
    public void loadClass() {

    }
}
