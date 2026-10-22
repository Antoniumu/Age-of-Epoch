package com.robertx22.addons.orbs_of_crafting.currency.reworked.keys;

import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;

public class LootTypeKey extends KeyInfo {

    public LootType type;

    public LootTypeKey(LootType type) {
        this.type = type;
    }

    @Override
    public String GUID() {
        return type.GUID();
    }
}
