package com.robertx22.forge_compat.fml;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

public interface IExtensionPoint<T> {
    record DisplayTest(Supplier<String> suppliedVersion, BiPredicate<String, Boolean> remoteVersionTest) implements IExtensionPoint<DisplayTest> {
        public static final String IGNORESERVERONLY = "OHNOES";
    }
}
