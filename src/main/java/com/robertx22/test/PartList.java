package com.robertx22.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.robertx22.library_of_exile.registry.serialization.MyGSON;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PartList<RAW, OBJECT> extends SchemaPart<PartList<RAW, OBJECT>, List<RAW>, List<OBJECT>> {

    public SchemaPart<?, RAW, OBJECT> verifierSchema;

    public void add(RAW obj) {
        object.add(obj);
    }

    @Override
    public List<RAW> toRaw(List<OBJECT> t) {
        return object;
    }

    @Override
    public List<OBJECT> get() {
        return object.stream().map(x -> {
            var obj = verifierSchema.sup.get();
            obj.setRaw(x);
            return obj.get();
        }).collect(Collectors.toList());
    }

    public PartList(ModSchema holder, String id, SchemaPart verifierSchema) {
        super(holder, id, null);
        this.verifierSchema = verifierSchema;
    }

    @Override
    public List<RAW> fromJsonElement(MainSchema main, JsonElement element) {
        List<RAW> list = new ArrayList<>();
        for (JsonElement ele : element.getAsJsonArray()) {
            var obj = verifierSchema.sup.get();
            obj.load(main, ele);
            list.add((obj.getRaw()));
        }
        return list;
    }

    @Override
    public void save(JsonObject json) {
        json.add(id, MyGSON.GSON.toJsonTree(object));
    }
}
