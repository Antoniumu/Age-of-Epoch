package com.robertx22.test;

import com.google.common.base.Supplier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.robertx22.library_of_exile.registry.IGUID;
import com.robertx22.test.test2.SchemaTestMain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// SELF = The final class
// RAW = the type that's saved ultimately to json
// OBJECT = optional transformed value, for easy access, so i don't have to call DB.registry.get(x) every time for example
public abstract class SchemaPart<SELF, RAW, OBJECT> {

    public Supplier<SchemaPart<SELF, RAW, OBJECT>> sup; // for creating another instance, used when
    public String id;
    public ModSchema<?> holder;
    protected RAW object;
    public List<SchemaReq<RAW>> requirements = new ArrayList<>(); // checks validity, for example, a number must be above 0, or a registry id must be valid
    public SchemaSuggestions<RAW> suggestions = null; // mostly to browse a list of possible registries inside a gui
    public RAW optionalValue = null; // if the json doesnt contain the field for this part, this value will be used instead


    // todo add comments, requirements etc

    static String EMPTY_ID = "empty_id";

    private static class EMPTY_HOLDER extends MainSchema<EMPTY_HOLDER> {

        public EMPTY_HOLDER() {
            super(() -> new SchemaTestMain());
        }
    }

    public abstract RAW toRaw(OBJECT t);


    public static ModSchema EMPTY_HOLDER = new EMPTY_HOLDER();

    public SchemaPart(ModSchema holder, String id, Supplier<SchemaPart<SELF, RAW, OBJECT>> sup) {
        this.id = id;
        this.holder = holder;
        this.sup = sup;

        if (holder instanceof EMPTY_HOLDER == false) {
            holder.parts.add(this);
        }
    }

    public SELF setDefault(RAW val) {
        this.optionalValue = val;
        return (SELF) this;
    }

    public SELF getSelf() {
        return (SELF) this;
    }

    public SELF addReq(SchemaReq<RAW> req) {
        this.requirements.add(req);
        return getSelf();
    }

    public void setRaw(RAW raw) {
        this.object = raw;
    }


    public void set(OBJECT t) {
        this.object = toRaw(t);
    }

    public abstract OBJECT get();

    public RAW getRaw() {
        return object;
    }

    public boolean isOptional() {
        return optionalValue != null;
    }

    public void load(MainSchema main, JsonElement element) {
        RAW obj = fromJsonElement(main, element);
        this.object = obj;
        verify(main, SchemaReq.VerifyTime.ON_LOAD);
    }

    public void verify(MainSchema main, SchemaReq.VerifyTime time) {

        if (object == null && !isOptional()) {
            main.errorOut("Field " + id + " is null after loading");
        }
        for (SchemaReq r : requirements) {
            if (r.verifyTime == time) {
                if (!r.pred.test(object)) {
                    main.errorOut((String) r.errorMsg.apply(object));
                }
            }
        }

    }

    public abstract RAW fromJsonElement(MainSchema main, JsonElement element);

    public abstract void save(JsonObject json);


    public static class INT extends SchemaPart<INT, Integer, Integer> {


        public INT(ModSchema holder, String id) {
            super(holder, id, () -> new INT(EMPTY_HOLDER, EMPTY_ID));
        }

        public static INT EMPTY = new INT(EMPTY_HOLDER, EMPTY_ID);

        @Override
        public Integer toRaw(Integer t) {
            return t;
        }


        @Override
        public Integer get() {
            return getRaw();
        }

        @Override
        public Integer fromJsonElement(MainSchema main, JsonElement element) {
            return element.getAsInt();
        }

        @Override
        public void save(JsonObject json) {
            json.addProperty(id, getRaw());
        }
    }

    public static class ID extends SchemaPart<ID, String, String> {
        public static ID EMPTY = new ID(EMPTY_HOLDER);

        public ID(ModSchema holder) {
            super(holder, "id", () -> new ID(EMPTY_HOLDER));
        }

        @Override
        public String toRaw(String t) {
            return t;
        }

        @Override
        public String get() {
            return getRaw();
        }

        @Override
        public String fromJsonElement(MainSchema main, JsonElement element) {
            return element.getAsString();
        }

        @Override
        public void save(JsonObject json) {
            json.addProperty(id, getRaw());
        }
    }

    public static class REGISTRY<T extends IGUID> extends SchemaPart<REGISTRY<T>, String, T> {
        RegType<T> type;

        public REGISTRY(ModSchema holder, RegType<T> type, String id) {
            super(holder, id, () -> new REGISTRY(EMPTY_HOLDER, type, EMPTY_ID));
            this.type = type;
            this.addReq(validRegistry(type));

            this.suggestions = new SchemaSuggestions<String>() {
                @Override
                public List<String> getAllPossible() {
                    return type.getAll().stream().map(x -> x.GUID()).collect(Collectors.toList());
                }
            };
        }

        @Override
        public T get() {
            return type.getObject(getRaw());
        }

        @Override
        public String toRaw(T t) {
            return t.GUID();
        }


        @Override
        public String fromJsonElement(MainSchema main, JsonElement element) {
            return element.getAsString();
        }

        @Override
        public void save(JsonObject json) {
            json.addProperty(id, getRaw());
        }


        public static SchemaReq<String> validRegistry(RegType<?> type) {
            return new SchemaReq<String>(x -> type.hasObject(x), SchemaReq.VerifyTime.ON_DATAPACKS_LOADED, e -> e + " is not a registered object in " + type.getRegistryName() + " registry");
        }
    }


}
