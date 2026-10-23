package com.robertx22.library_of_exile.registry;

public class DataGenKey<T> implements IGUID {

    String id;

    public DataGenKey(String id) {
        this.id = id;
    }

    @Override
    public String GUID() {
        return id;
    }
}
