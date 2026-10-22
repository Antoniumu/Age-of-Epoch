package com.robertx22.forge_compat.fml;

import com.robertx22.forge_compat.api.distmarker.Dist;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface EventBusSubscriber {
        Dist[] value() default { Dist.CLIENT, Dist.DEDICATED_SERVER };
        String modid() default "";
        Bus bus() default Bus.FORGE;

        enum Bus {
            FORGE, MOD
        }
    }
}
