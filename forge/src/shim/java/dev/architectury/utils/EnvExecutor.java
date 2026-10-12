package dev.architectury.utils;

import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): runs code only on the given physical side. */
public final class EnvExecutor {
    private EnvExecutor() {}

    public static void runInEnv(Env env, Supplier<Runnable> runnable) {
        if ((env == Env.CLIENT) == (FMLEnvironment.dist == Dist.CLIENT)) {
            runnable.get().run();
        }
    }
}
