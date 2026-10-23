package com.robertx22.infinite_dungeons.components.data;

import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.layout.ShopEntry;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

@Storable
public class BuyHistoryEntryData {

    @Store
    public String shop_list = "";

    @Store
    public int index = 0;

    @Store
    public int amount = 0;

    public ShopEntry getEntry() {
        try {
            return DungeonsDB.ShopLists()
                .get(shop_list).shop_entries.get(index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
