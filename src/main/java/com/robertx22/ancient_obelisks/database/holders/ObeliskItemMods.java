package com.robertx22.ancient_obelisks.database.holders;

import com.robertx22.ancient_obelisks.database.item_mods.AddObeliskSpawnRateMod;
import com.robertx22.ancient_obelisks.database.item_mods.AddObeliskTierMod;
import com.robertx22.ancient_obelisks.database.item_mods.AddObeliskWaveMod;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;

public class ObeliskItemMods extends ExileKeyHolder<ItemModification> {
    public static ObeliskItemMods INSTANCE = new ObeliskItemMods(ObelisksMain.REGISTER_INFO);

    public ObeliskItemMods(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<ItemModification, KeyInfo> ADD_WAVE = ExileKey.ofId(this, "add_wave", x -> new AddObeliskWaveMod(x.GUID(), new AddObeliskWaveMod.Data(1)));
    public ExileKey<ItemModification, KeyInfo> ADD_TIER = ExileKey.ofId(this, "add_tier", x -> new AddObeliskTierMod(x.GUID(), new AddObeliskTierMod.Data(1)));
    public ExileKey<ItemModification, KeyInfo> ADD_SPAWN_RATE = ExileKey.ofId(this, "add_spawn_rate", x -> new AddObeliskSpawnRateMod(x.GUID(), new AddObeliskSpawnRateMod.Data(10)));


    @Override
    public void loadClass() {

    }
}
