package com.robertx22.test.test2;

import com.robertx22.test.*;

public class SchemaTestMain extends MainSchema<SchemaTestMain> {

    public SchemaTestMain() {
        super(() -> new SchemaTestMain());
    }

    public SchemaPart.INT number = new SchemaPart.INT(this, "number")
            .addReq(SchemaReq.INT.POSITIVE);

    public SchemaPart.INT optionalNUmber = new SchemaPart.INT(this, "optnum")
            .setDefault(2)
            .addReq(SchemaReq.INT.POSITIVE);

    public SchemaTestInner inner = new SchemaTestInner(this);

    public PartList<Integer, Integer> partlist = new PartList(this, "partlist", SchemaPart.INT.EMPTY);

    public SchemaList<SchemaTestInner> schemalist = new SchemaList<>(this, "list", new SchemaTestInner(SchemaPart.EMPTY_HOLDER));

    public PrimitiveList<String> parts = new PrimitiveList<>(this, "parts");

    public static class SchemaTestInner extends ModSchema<SchemaTestInner> {

        public SchemaTestInner(ModSchema parent) {
            super(parent, "inner", () -> new SchemaTestInner(parent));
        }

        public SchemaPart.INT number2 = new SchemaPart.INT(this, "number2");

    }

}
