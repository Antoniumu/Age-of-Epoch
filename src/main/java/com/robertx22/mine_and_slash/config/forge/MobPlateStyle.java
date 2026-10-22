package com.robertx22.mine_and_slash.config.forge;

public enum MobPlateStyle {
    CLASSIC("Классический"),
    MODERN("Современный"),
    RUNE("Рунический");

    public final String displayName;

    MobPlateStyle(String displayName) {
        this.displayName = displayName;
    }

    public MobPlateStyle next() {
        MobPlateStyle[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }

    public static MobPlateStyle byName(String s) {
        if (s == null) return MODERN;
        for (MobPlateStyle st : values()) {
            if (st.name().equalsIgnoreCase(s.trim())) {
                return st;
            }
        }
        return MODERN;
    }
}
