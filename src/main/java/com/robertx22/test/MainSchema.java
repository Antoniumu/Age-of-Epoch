package com.robertx22.test;

import com.google.common.base.Supplier;

public abstract class MainSchema<T> extends ModSchema<T> {

    public MainSchema(Supplier<ModSchema> sup) {
        super(null, "main_schemas_have_no_id", sup);
    }

    public SchemaPart.ID id = new SchemaPart.ID(this);

    public void errorOut(String reason) {
        try {
            throw new RuntimeException("Datapack " + id.get() + " is broken!");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        throw new RuntimeException(reason);
    }

}
