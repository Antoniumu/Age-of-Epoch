package com.robertx22.infinite_dungeons.database.db_types;

import com.robertx22.infinite_dungeons.database.db_types.layout.ShopEntry;
import com.robertx22.infinite_dungeons.db_init.RegistryTypes;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ShopList implements JsonExileRegistry<ShopList>, IAutoGson<ShopList> {
    public static ShopList SERIALIZER = new ShopList();

    public int weight = 1000;
    private String id = "";
    public String for_layout_id = "";
    public Integer priority = 0; // higher priority means that one gets used

    public List<ShopEntry> shop_entries = new ArrayList<>();

    public void setIdForBaseModPerLayoutId(String layoutId) {
        this.id = MainID.MODID + "_" + layoutId;
        this.for_layout_id = layoutId;
        this.priority = 0;
    }

    public void setIdForMineAndSlashModPerLayoutId(String layoutId) {
        this.id = "mmorpg" + "_" + layoutId;
        this.for_layout_id = layoutId;
        this.priority = 1;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.SHOP_LIST;
    }

    @Override
    public String GUID() {
        return id;
    }

    public ShopList edit(Consumer<ShopList> cons) {
        cons.accept(this);
        return this;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public Class<ShopList> getClassForSerialization() {
        return ShopList.class;
    }

}
