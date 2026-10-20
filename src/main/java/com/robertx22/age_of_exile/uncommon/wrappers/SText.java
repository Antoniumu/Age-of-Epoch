package com.robertx22.age_of_exile.uncommon.wrappers;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class SText {
    public final String msg;

    public SText(String msg) {
        this.msg = msg;
    }

    public static MutableComponent of(String msg) {
        return Component.literal(msg);
    }
}
