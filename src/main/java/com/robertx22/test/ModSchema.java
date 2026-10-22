package com.robertx22.test;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public abstract class ModSchema<T> {

    public String id;

    public Supplier<ModSchema> sup;

    public List<SchemaPart> parts = new ArrayList<>();
    public List<ModSchema> schemas = new ArrayList<>();

    public ModSchema<T> createNewInstance() {
        return sup.get();
    }

    public ModSchema(ModSchema parent, String id, Supplier<ModSchema> sup) {
        this.id = id;
        this.sup = sup;
        if (parent == null && this instanceof MainSchema) {
            // main schemas dont have a parent
        } else {
            Preconditions.checkArgument(parent != null);
            parent.schemas.add(this);
        }
    }


    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (SchemaPart part : parts) {
            part.save(json);
        }
        for (ModSchema part : schemas) {
            JsonObject inner = part.toJson();
            json.add(part.id, inner);
        }

        return json;
    }

    public void load(MainSchema main, JsonObject json) {
        for (SchemaPart part : parts) {
            if (!json.has(part.id)) {
                if (part.isOptional()) {
                    part.object = part.optionalValue;
                } else {
                    throw new RuntimeException("Datapack error: " + part.id + " field inside " + main.id + " is missing and not optional!");
                }
            }
            var loaded = json.get(part.id);
            part.load(main, loaded);
        }
        for (ModSchema part : schemas) {
            JsonObject inner = json.getAsJsonObject(part.id);
            part.load(main, inner);
        }
    }

}
