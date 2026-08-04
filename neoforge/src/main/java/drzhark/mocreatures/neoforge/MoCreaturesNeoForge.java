package drzhark.mocreatures.neoforge;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.neoforge.client.MoCLairSkyboxRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;

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

        if (dist.isClient()) {
            // Client-only: register the Wyvern Lair twin-suns skybox renderer.
            modBus.addListener(RegisterCustomEnvironmentEffectRendererEvent.class, this::onRegisterSky);
        }
    }

    /**
     * MOD-bus, client-only. Binds the twin-suns skybox renderer to the {@code mocreatures:wyvern_lair}
     * custom-skybox id (the value the Lair dimension_type sets on the {@code neoforge:custom_skybox}
     * environment attribute).
     */
    private void onRegisterSky(RegisterCustomEnvironmentEffectRendererEvent event) {
        event.registerSkyboxRenderer(
                Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "wyvern_lair"),
                new MoCLairSkyboxRenderer());
    }
}
