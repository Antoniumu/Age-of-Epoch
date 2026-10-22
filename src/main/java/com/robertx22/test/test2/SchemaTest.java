package com.robertx22.test.test2;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.robertx22.test.SchemaPart;

public class SchemaTest {

    public static void run() {

        if (true) {
            return;
        }


        Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();


        SchemaTestMain schema = new SchemaTestMain();
        schema.id.setRaw("mytest");
        schema.number.setRaw(-50);
        schema.inner.number2.setRaw(100);

        var add1 = new SchemaTestMain.SchemaTestInner(SchemaPart.EMPTY_HOLDER);
        add1.number2.setRaw(5);
        var add2 = new SchemaTestMain.SchemaTestInner(SchemaPart.EMPTY_HOLDER);
        add2.number2.setRaw(10);
        schema.schemalist.get().add(add1);
        schema.schemalist.get().add(add2);

        schema.parts.get().add("testpart");
        schema.parts.get().add("testpart2");

        JsonObject json = schema.toJson();

        System.out.println(json.toString());
        //System.out.println(GSON.toJsonTree(json.toString()));

        System.out.println("-----------\n");


        SchemaTestMain test2 = new SchemaTestMain();
        test2.load(test2, json);

        Preconditions.checkArgument(test2.number.get() == -50);
        Preconditions.checkArgument(test2.inner.number2.get() == 100);

        System.out.println(test2.toJson());


        System.out.println("-----------\n");

    }
}
