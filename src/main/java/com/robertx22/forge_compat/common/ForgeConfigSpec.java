package com.robertx22.forge_compat.common;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Simplified ForgeConfigSpec backed by in-memory values.
 */
public class ForgeConfigSpec {

    private final Map<String, Object> values = new HashMap<>();

    public static class ConfigValue<T> implements Supplier<T> {
        private final String path;
        private final T defaultValue;
        private final Map<String, Object> store;

        ConfigValue(String path, T defaultValue, Map<String, Object> store) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.store = store;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            Object v = store.get(path);
            return v == null ? defaultValue : (T) v;
        }

        public void set(T value) { store.put(path, value); }

        public String getPath() { return path; }
    }

    public static class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(String path, Boolean def, Map<String, Object> store) { super(path, def, store); }
    }

    public static class IntValue extends ConfigValue<Integer> {
        IntValue(String path, Integer def, Map<String, Object> store) { super(path, def, store); }
    }

    public static class DoubleValue extends ConfigValue<Double> {
        DoubleValue(String path, Double def, Map<String, Object> store) { super(path, def, store); }
    }

    public static class LongValue extends ConfigValue<Long> {
        LongValue(String path, Long def, Map<String, Object> store) { super(path, def, store); }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        EnumValue(String path, T def, Map<String, Object> store) { super(path, def, store); }
    }

    public static class Builder {
        private final List<String> stack = new ArrayList<>();
        private final Map<String, Object> values = new HashMap<>();

        public Builder comment(String... comments) { return this; }

        public Builder comment(String comment) { return this; }

        public Builder push(String name) { stack.add(name); return this; }

        public Builder pop() { if (!stack.isEmpty()) stack.remove(stack.size() - 1); return this; }

        public Builder worldRestart() { return this; }

        private String path(String name) {
            StringBuilder sb = new StringBuilder();
            for (String s : stack) sb.append(s).append('.');
            sb.append(name);
            return sb.toString();
        }

        public <T> ConfigValue<T> define(String name, T defaultValue) {
            return define(name, defaultValue, x -> true);
        }

        public <T> ConfigValue<T> define(String name, T defaultValue, Predicate<Object> validator) {
            return new ConfigValue<>(path(name), defaultValue, values);
        }

        public <T> ConfigValue<List<? extends T>> defineList(String name, List<? extends T> defaultValue, Predicate<Object> elementValidator) {
            return new ConfigValue<>(path(name), defaultValue, values);
        }

        public BooleanValue define(String name, boolean defaultValue) {
            return new BooleanValue(path(name), defaultValue, values);
        }

        public IntValue defineInRange(String name, int defaultValue, int min, int max) {
            return new IntValue(path(name), defaultValue, values);
        }

        public DoubleValue defineInRange(String name, double defaultValue, double min, double max) {
            return new DoubleValue(path(name), defaultValue, values);
        }

        public LongValue defineInRange(String name, long defaultValue, long min, long max) {
            return new LongValue(path(name), defaultValue, values);
        }

        public <T extends Enum<T>> EnumValue<T> defineEnum(String name, T defaultValue) {
            return new EnumValue<>(path(name), defaultValue, values);
        }

        public <T extends Enum<T>> EnumValue<T> defineEnum(String name, T defaultValue, java.util.function.Function<T, String> namer) {
            return new EnumValue<>(path(name), defaultValue, values);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T> Pair<T, ForgeConfigSpec> configure(java.util.function.Function<Builder, T> consumer) {
            T instance = consumer.apply(this);
            ForgeConfigSpec spec = new ForgeConfigSpec();
            spec.values.putAll(values);
            return (Pair) new ImmutablePair<>(instance, spec);
        }
    }
}
