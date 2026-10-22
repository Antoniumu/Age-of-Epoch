package com.robertx22.ancient_obelisks.database;

import com.robertx22.library_of_exile.database.mob_list.MobList;
import com.robertx22.library_of_exile.database.mob_list.MobListTags;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.tags.ExileTagRequirement;

public class Obelisk implements JsonExileRegistry<Obelisk>, IAutoGson<Obelisk> {
    public static Obelisk SERIALIZER = new Obelisk(new SimplePrebuiltMapData(1, ""), 0, "empty");

    public SimplePrebuiltMapData structure_data = null;

    public int weight = 1000;
    public String id = "";
    public ExileTagRequirement<MobList> mob_list_tag_check = new ExileTagRequirement().createBuilder().includes(MobListTags.MAP).build();

    public Obelisk(SimplePrebuiltMapData simple_prebuilt_map, int weight, String id) {
        this.structure_data = simple_prebuilt_map;
        this.weight = weight;
        this.id = id;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ObeliskDatabase.OBELISK_TYPE;
    }

    @Override
    public Class<Obelisk> getClassForSerialization() {
        return Obelisk.class;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }
}
