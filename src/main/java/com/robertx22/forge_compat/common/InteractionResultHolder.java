package com.robertx22.forge_compat.common;

import net.minecraft.world.InteractionResult;

public record InteractionResultHolder<T>(InteractionResult getResult, T getObject) {
    public static <T> InteractionResultHolder<T> success(T obj) { return new InteractionResultHolder<>(InteractionResult.SUCCESS, obj); }
    public static <T> InteractionResultHolder<T> consume(T obj) { return new InteractionResultHolder<>(InteractionResult.CONSUME, obj); }
    public static <T> InteractionResultHolder<T> pass(T obj) { return new InteractionResultHolder<>(InteractionResult.PASS, obj); }
    public static <T> InteractionResultHolder<T> fail(T obj) { return new InteractionResultHolder<>(InteractionResult.FAIL, obj); }
    public static <T> InteractionResultHolder<T> sidedSuccess(T obj, boolean clientSide) { return new InteractionResultHolder<>(InteractionResult.SUCCESS, obj); }
}
