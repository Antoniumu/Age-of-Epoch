package com.robertx22.infinite_dungeons.components.data;

import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.layout.ShopEntry;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Storable
public class BuyHistoryData {

    @Store
    public String shop_list = "";

    @Store
    public List<BuyHistoryEntryData> all = new ArrayList<>();

    // this could be made better...
    public BuyHistoryEntryData getOrCreateHistoryOf(ShopEntry entry) {

        Optional<BuyHistoryEntryData> opt = all.stream()
            .filter(x -> x.getEntry()
                .equals(entry))
            .findFirst();

        if (opt.isPresent()) {
            return opt.get();
        }
        BuyHistoryEntryData data = new BuyHistoryEntryData();
        data.shop_list = shop_list;

        int index = 0;
        for (ShopEntry e : DungeonsDB.ShopLists()
            .get(shop_list).shop_entries) {
            if (e.equals(entry)) {
                data.index = index;
                break;
            }
            index++;
        }
        all.add(data);
        return data;

    }
}
