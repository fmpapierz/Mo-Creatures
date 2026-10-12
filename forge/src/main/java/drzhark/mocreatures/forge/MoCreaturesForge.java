package drzhark.mocreatures.forge;

import com.mojang.serialization.MapCodec;
import dev.architectury.impl.ForgeShim;
import dev.architectury.networking.NetworkManager;
import drzhark.mocreatures.MoCreatures;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * MinecraftForge entry point. Architectury has no Forge build after 1.20.4, so the shared code runs here on this
 * module's Forge implementation of the Architectury classes it calls (src/shim, see dev.architectury.impl.ForgeShim):
 * the mod's bus group is handed to it first, then the shared init registers everything, then the network channel is
 * built with every payload the init registered.
 *
 * <p>Biome spawn lists are Forge's biome modifiers, as on NeoForge: one modifier type, mocreatures:spawns, applied to
 * every biome by data/mocreatures/forge/biome_modifier/spawns.json, which feeds the shared MoCSpawns rules. The lair
 * skies come from the sky renderer mixin (Forge has no sky event), as on Fabric.</p>
 */
@Mod(MoCreatures.MOD_ID)
public final class MoCreaturesForge {

    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MoCreatures.MOD_ID);

    static {
        BIOME_MODIFIERS.register("spawns", () -> MoCForgeBiomeSpawns.CODEC);
    }

    public MoCreaturesForge(FMLJavaModLoadingContext context) {
        ForgeShim.setModBusGroup(context.getModBusGroup());
        MoCreatures.init();
        NetworkManager.buildChannel();
        BIOME_MODIFIERS.register(context.getModBusGroup());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            // The Mods screen's Config button opens the settings screen.
            drzhark.mocreatures.forge.client.MoCForgeClient.registerConfigScreen(context);
        }
    }
}
