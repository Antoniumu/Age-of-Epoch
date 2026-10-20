package com.robertx22.forge_compat.eventbus.api;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SubscribeEvent {
    EventPriority priority() default EventPriority.NORMAL;
    boolean receiveCanceled() default false;
}
