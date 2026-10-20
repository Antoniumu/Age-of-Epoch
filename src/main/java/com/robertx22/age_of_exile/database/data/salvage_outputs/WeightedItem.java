package com.robertx22.age_of_exile.database.data.salvage_outputs;

import com.robertx22.library_of_exile.registry.IWeighted;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class WeightedItem implements IWeighted {

    String item;
    int weight;

    public WeightedItem(Item item, int weight) {
        this.item = BuiltInRegistries.ITEM.getKey(item)
            .toString();
        this.weight = weight;
    }

    public Item getItem() {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(item));
    }

    @Override
    public int Weight() {
        return weight;
    }
}
