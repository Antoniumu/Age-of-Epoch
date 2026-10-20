package com.robertx22.age_of_exile.database.registrators;

import com.robertx22.age_of_exile.database.data.currency.base.CurrencyItem;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.forge_compat.registries.ForgeRegistries;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;

public class CurrencyItems implements ExileRegistryInit {

    @Override
    public void registerAll() {
        ExileDB.CurrencyItems().setDefault(com.robertx22.age_of_exile.mmorpg.registers.common.items.CurrencyItems.ORB_OF_TRANSMUTATION.get());
        ForgeRegistries.ITEMS.forEach(x -> {
            if (x instanceof CurrencyItem) {
                CurrencyItem cur = (CurrencyItem) x;
                cur.unregisterFromExileRegistry();
                cur.registerToExileRegistry();
            }
        });
    }

}
