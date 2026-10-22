package com.robertx22.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.robertx22.library_of_exile.registry.serialization.MyGSON;

import java.util.Map;

// if they're all primitives and i anyway just serialize them all with gson, then i need to just do that and bundle some verifier?

public class PartMap<Key, T> extends SchemaPart<PartMap<Key, T>, Map<Key, T>, Map<Key, T>> {

    public SchemaPart<?, ?, T> verifierSchema;


    public void put(Key key, T obj) {
        object.put(key, obj);
    }

    @Override
    public Map<Key, T> toRaw(Map<Key, T> t) {
        return object;
    }

    @Override
    public Map<Key, T> get() {
        return object;
    }

    public PartMap(ModSchema holder, String id, SchemaPart verifierSchema) {
        super(holder, id, null);
        this.verifierSchema = verifierSchema;
    }

    // todo if i save like this i cant verify stuff
    @Override
    public Map<Key, T> fromJsonElement(MainSchema main, JsonElement element) {
        return MyGSON.GSON.fromJson(element, object.getClass());
    }

    @Override
    public void save(JsonObject json) {
        json.add(id, MyGSON.GSON.toJsonTree(object));
    }
}
