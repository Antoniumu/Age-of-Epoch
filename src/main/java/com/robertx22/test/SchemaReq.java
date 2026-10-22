package com.robertx22.test;

import java.util.function.Function;
import java.util.function.Predicate;

public class SchemaReq<T> {

    public Predicate<T> pred;
    public VerifyTime verifyTime;
    public Function<T, String> errorMsg;

    public SchemaReq(Predicate<T> pred, VerifyTime verifyTime, Function<T, String> errorMsg) {
        this.pred = pred;
        this.verifyTime = verifyTime;
        this.errorMsg = errorMsg;
    }

    public enum VerifyTime {
        ON_LOAD,
        ON_DATAPACKS_LOADED
    }

    public static class INT {
        public static SchemaReq<Integer> POSITIVE = new SchemaReq<>(x -> x > -1, VerifyTime.ON_LOAD, e -> "Number " + e + " must be positive");
    }

}
