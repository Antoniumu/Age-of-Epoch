package com.robertx22.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.robertx22.library_of_exile.registry.serialization.MyGSON;

import java.util.ArrayList;
import java.util.List;

// used for lists, maps etc
public class PrimitiveList<T> extends SchemaPart<PrimitiveList<T>, List<T>, List<T>> {

    public PrimitiveList(ModSchema holder, String id) {
        super(holder, id, null);
        this.object = new ArrayList<>();
    }

    @Override
    public List<T> toRaw(List<T> t) {
        return t;
    }

    @Override
    public List<T> get() {
        return object;
    }

    @Override
    public List<T> fromJsonElement(MainSchema main, JsonElement element) {
        return (List<T>) MyGSON.GSON.fromJson(element, object.getClass());
    }

    @Override
    public void save(JsonObject json) {
        json.add(id, MyGSON.GSON.toJsonTree(object));
    }
}
