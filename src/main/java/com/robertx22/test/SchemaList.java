package com.robertx22.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SchemaList<T extends ModSchema> extends SchemaPart<SchemaList<T>, List<T>, List<T>> {

    public T emptySchema;


    public void add(T obj) {
        object.add(obj);
    }

    @Override
    public List<T> toRaw(List<T> t) {
        return object;
    }

    @Override
    public List<T> get() {
        return object;
    }

    public SchemaList(ModSchema holder, String id, T emptySchema) {
        super(holder, id, null); // todo
        this.emptySchema = emptySchema;
        this.object = new ArrayList<>();
    }


    @Override
    public List<T> fromJsonElement(MainSchema main, JsonElement element) {
        List<T> list = new ArrayList<>();
        for (JsonElement ele : element.getAsJsonArray()) {
            var s = emptySchema.createNewInstance();
            s.load(main, ele.getAsJsonObject());
            list.add((T) s);
        }
        return list;
    }

    @Override
    public void save(JsonObject json) {
        JsonArray array = new JsonArray();
        for (T t : object) {
            array.add(t.toJson());
        }
        json.add(id, array);
    }
}
