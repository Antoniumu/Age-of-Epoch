package com.robertx22.forge_compat.common.capabilities;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class CapabilityToken<T> {
    @SuppressWarnings("unchecked")
    public Class<T> getRawType() {
        Type sup = getClass().getGenericSuperclass();
        if (sup instanceof ParameterizedType pt && pt.getActualTypeArguments().length > 0) {
            Type arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class<?> c) return (Class<T>) c;
            if (arg instanceof ParameterizedType p2 && p2.getRawType() instanceof Class<?> c2) return (Class<T>) c2;
        }
        return (Class<T>) Object.class;
    }
}
