package com.robertx22.forge_compat.fml;

import com.robertx22.forge_compat.api.distmarker.Dist;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class DistExecutor {
    public interface SafeRunnable extends Runnable {
        void run();
    }

    private static boolean matches(Dist dist) {
        return (dist == Dist.CLIENT && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
            || (dist == Dist.DEDICATED_SERVER && FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER);
    }

    public static void runWhenOn(Dist dist, Supplier<Runnable> toRun) {
        if (matches(dist) && toRun != null) {
            Runnable r = toRun.get();
            if (r != null) r.run();
        }
    }

    public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> toRun) {
        runWhenOn(dist, toRun);
    }

    public static void safeRunWhenOn(Dist dist, Supplier<SafeRunnable> toRun) {
        if (matches(dist) && toRun != null) {
            SafeRunnable r = toRun.get();
            if (r != null) r.run();
        }
    }

    public static <T> T unsafeCallWhenOn(Dist dist, Supplier<Callable<T>> toCall) {
        try {
            if (matches(dist) && toCall != null) return toCall.get().call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
