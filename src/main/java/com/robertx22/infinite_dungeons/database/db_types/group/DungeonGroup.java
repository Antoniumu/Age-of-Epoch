package com.robertx22.infinite_dungeons.database.db_types.group;
import net.minecraft.network.chat.MutableComponent;

import com.robertx22.infinite_dungeons.db_init.RegistryTypes;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import com.robertx22.forge_compat.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DungeonGroup implements JsonExileRegistry<DungeonGroup>, IAutoGson<DungeonGroup> {
    public static DungeonGroup SERIALIZER = new DungeonGroup();

    public int weight = 1000;
    public String id = "";

    public int modeldatanum = 0;

    public String key_item_id = "";
    public List<CoinDrop> coin_drops = new ArrayList<>();

    public Item getKeyItem() {
        return ForgeRegistries.ITEMS.getValue(Identifier.parse(key_item_id));
    }

    public MutableComponent getTranslatable() {
        return Component.translatable(MainID.MODID + ".dungeon_group." + id);
    }

    public DungeonGroup edit(Consumer<DungeonGroup> cons) {
        cons.accept(this);
        return this;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.DUNGEON_GROUP;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public Class<DungeonGroup> getClassForSerialization() {
        return DungeonGroup.class;
    }

}
