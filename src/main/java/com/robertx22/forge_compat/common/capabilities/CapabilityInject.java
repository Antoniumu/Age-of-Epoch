package com.robertx22.forge_compat.common.capabilities;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface CapabilityInject {
    Class<?> value();
}
