package drzhark.mocreatures.fabric;

import drzhark.mocreatures.MoCreatures;
import net.fabricmc.api.ModInitializer;

public final class MoCreaturesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MoCreatures.init();
        // Biome spawn lists are loader-specific plumbing; see MoCFabricBiomeSpawns.
        MoCFabricBiomeSpawns.register();
    }
}
