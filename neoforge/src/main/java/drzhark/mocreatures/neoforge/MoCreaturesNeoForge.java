package drzhark.mocreatures.neoforge;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.client.MoCLairSky;
import drzhark.mocreatures.neoforge.client.MoCLairSkyboxRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(MoCreatures.MOD_ID)
public final class MoCreaturesNeoForge {

    /**
     * FML injects a subset of {@code (IEventBus, ModContainer, Dist)} into the {@code @Mod} constructor
     * (verified via {@code javap} of {@code FMLModContainer} — it matches these three types by
     * parameter class). We take the mod event bus + the running {@link Dist} so the client-only sky
     * hook can be registered on the MOD bus without pulling any client class onto the dedicated server.
     */
    public MoCreaturesNeoForge(IEventBus modBus, Dist dist) {
        MoCreatures.init();

        // Biome spawn lists: register the serializer for our BiomeModifier. The datapack entry that instantiates
        // it lives at data/mocreatures/neoforge/biome_modifier/spawns.json. See MoCBiomeSpawnModifier for why
        // Architectury's BiomeModifications cannot carry these on NeoForge.
        modBus.addListener(RegisterEvent.class, this::onRegister);

        if (dist.isClient()) {
            // Client-only: register the Lair skybox renderers (Wyvern twin suns, Ogre ember sun).
            modBus.addListener(RegisterCustomEnvironmentEffectRendererEvent.class, this::onRegisterSky);
        }
    }

    /**
     * MOD-bus. Registers the {@code mocreatures:spawns} biome-modifier serializer so the datapack entry at
     * {@code data/mocreatures/neoforge/biome_modifier/spawns.json} can be decoded into
     * {@link MoCBiomeSpawnModifier}.
     */
    private void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, registry ->
                registry.register(Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "spawns"),
                        MoCBiomeSpawnModifier.CODEC));
    }

    /**
     * MOD-bus, client-only. Binds a Lair skybox renderer to each lair's custom-skybox id (the value
     * that lair's dimension_type sets on the {@code neoforge:custom_skybox} environment attribute):
     * twin suns for {@code mocreatures:wyvern_lair}, the single ember sun for
     * {@code mocreatures:ogre_lair}.
     */
    private void onRegisterSky(RegisterCustomEnvironmentEffectRendererEvent event) {
        event.registerSkyboxRenderer(
                Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "wyvern_lair"),
                new MoCLairSkyboxRenderer(MoCLairSky.TWIN_SUNS_TEXTURE));
        event.registerSkyboxRenderer(
                Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "ogre_lair"),
                new MoCLairSkyboxRenderer(MoCLairSky.EMBER_SUN_TEXTURE));
    }
}
