package com.robertx22.forge_compat.common.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LazyOptional<T> {

    private final Supplier<T> supplier;
    private final boolean empty;

    private LazyOptional(Supplier<T> supplier, boolean empty) {
        this.supplier = supplier;
        this.empty = empty;
    }

    public static <T> LazyOptional<T> of(Supplier<T> supplier) {
        return new LazyOptional<>(supplier, false);
    }

    public static <T> LazyOptional<T> empty() {
        return new LazyOptional<>(() -> null, true);
    }

    public boolean isPresent() {
        return !empty && supplier.get() != null;
    }

    public T orElse(T other) {
        if (empty) return other;
        T v = supplier.get();
        return v == null ? other : v;
    }

    public T orElseThrow(java.util.function.Supplier<? extends RuntimeException> ex) {
        T v = empty ? null : supplier.get();
        if (v == null) throw ex.get();
        return v;
    }

    public Optional<T> resolve() {
        return Optional.ofNullable(empty ? null : supplier.get());
    }

    public void ifPresent(Consumer<T> consumer) {
        if (!empty) {
            T v = supplier.get();
            if (v != null) consumer.accept(v);
        }
    }

    public <U> LazyOptional<U> cast() {
        return (LazyOptional<U>) this;
    }

    public void invalidate() {
        // no-op: cached capabilities live for the owner's lifetime
    }
}
