package dev.architectury.platform;

import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge stand-in (see dev.architectury.impl.ForgeShim). */
public final class Platform {
    private Platform() {}

    public static Path getConfigFolder() {
        return FMLPaths.CONFIGDIR.get();
    }
}
