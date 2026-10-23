package com.robertx22.forge_compat.fml.common;

import com.robertx22.forge_compat.api.distmarker.Dist;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String value();

    enum Bus { FORGE, MOD }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface EventBusSubscriber {
        Dist[] value() default {Dist.CLIENT, Dist.DEDICATED_SERVER};

        String modid() default "";

        Bus bus() default Bus.FORGE;
    }
}
