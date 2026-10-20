package com.robertx22.forge_compat.fml;

import com.robertx22.forge_compat.api.distmarker.Dist;

import java.util.function.Supplier;

public class DistExecutor {

    public static void runWhenOn(Dist dist, Supplier<Runnable> toRun) {
        boolean isClient;
        try {
            Class.forName("net.minecraft.client.Minecraft");
            isClient = true;
        } catch (Throwable t) {
            isClient = false;
        }
        if ((dist == Dist.CLIENT) == isClient) {
            toRun.get().run();
        }
    }

    public static void runWhenOn(Dist dist, Supplier<Runnable> toRun, Supplier<Runnable> orElse) {
        boolean isClient;
        try {
            Class.forName("net.minecraft.client.Minecraft");
            isClient = true;
        } catch (Throwable t) {
            isClient = false;
        }
        if ((dist == Dist.CLIENT) == isClient) {
            toRun.get().run();
        } else if (orElse != null) {
            orElse.get().run();
        }
    }

    public static void runWhenOn(Dist dist, Supplier<Runnable> toRun, Runnable orElse) {
        runWhenOn(dist, toRun, () -> orElse);
    }
}
