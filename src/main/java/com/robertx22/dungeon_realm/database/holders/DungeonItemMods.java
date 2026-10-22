package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.database.item_mods.UberUpgradeMod;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;

public class DungeonItemMods extends ExileKeyHolder<ItemModification> {

    public static DungeonItemMods INSTANCE = new DungeonItemMods(DungeonMain.REGISTER_INFO);

    private DungeonItemMods(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<ItemModification, KeyInfo> UBER_UPGRADE = ExileKey.ofId(this, "uber_upgrade", x -> new UberUpgradeMod(x.GUID()));


    @Override
    public void loadClass() {

    }
}
