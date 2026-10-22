package com.robertx22.mine_and_slash.saveclasses.item_classes.rework;

import net.minecraft.nbt.CompoundTag;

public abstract class NbtKey<T> {

    String id;
    CompoundTag nbt;

    public NbtKey(String id, CompoundTag nbt) {
        this.id = id;
        this.nbt = nbt;
    }

    public final T get() {
        if (!nbt.contains(id)) {
            return getDefault();
        }
        return getInternal();
    }

    protected abstract T getInternal();

    protected abstract T getDefault();


    public abstract void set(T obj);

    public class BooleanZ extends NbtKey<Boolean> {


        public BooleanZ(String id, CompoundTag nbt) {
            super(id, nbt);
        }

        protected Boolean getInternal() {
            return nbt.getBooleanOr(id, false);
        }

        @Override
        protected Boolean getDefault() {
            return false;
        }

        @Override
        public void set(Boolean obj) {
            nbt.putBoolean(id, obj);
        }
    }

    public class StringZ extends NbtKey<String> {


        public StringZ(String id, CompoundTag nbt) {
            super(id, nbt);
        }

        protected String getInternal() {
            return nbt.getStringOr(id, "");
        }

        @Override
        protected String getDefault() {
            return "";
        }

        @Override
        public void set(String obj) {
            nbt.putString(id, obj);
        }
    }

    public class IntZ extends NbtKey<Integer> {

        public IntZ(String id, CompoundTag nbt) {
            super(id, nbt);
        }

        @Override
        protected Integer getInternal() {
            return nbt.getIntOr(id, 0);
        }

        @Override
        protected Integer getDefault() {
            return 0;
        }

        @Override
        public void set(Integer obj) {
            nbt.putInt(id, obj);
        }
    }


    public class FloatZ extends NbtKey<Float> {

        public FloatZ(String id, CompoundTag nbt) {
            super(id, nbt);
        }

        @Override
        protected Float getInternal() {
            return nbt.getFloatOr(id, 0F);
        }

        @Override
        protected Float getDefault() {
            return 0F;
        }

        @Override
        public void set(Float obj) {
            nbt.putFloat(id, obj);
        }
    }
}





