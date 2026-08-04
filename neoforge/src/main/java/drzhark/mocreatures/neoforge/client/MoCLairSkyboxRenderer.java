package drzhark.mocreatures.neoforge.client;

import drzhark.mocreatures.client.MoCTwinSuns;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import org.joml.Matrix4fc;

/**
 * NeoForge custom skybox renderer for the Wyvern Lair ({@code mocreatures:wyvern_lair}).
 *
 * <p>Registered against the NeoForge custom-skybox environment attribute ({@code neoforge:custom_skybox}
 * → {@code "mocreatures:wyvern_lair"}, set in the NeoForge-only copy of
 * {@code data/mocreatures/dimension_type/wyvern_lair.json} — the attribute is stripped from the common
 * copy because Fabric can't parse it). NeoForge only invokes this renderer for the Lair.
 *
 * <p>Draws the vanilla lair sky first (colored sky + normal sun/moon/stars via {@code renderVanilla}),
 * then overlays the twin-suns disc via the shared, loader-agnostic {@link MoCTwinSuns#draw}.
 */
public final class MoCLairSkyboxRenderer implements CustomSkyboxRenderer {

    @Override
    public boolean renderSky(LevelRenderState level, SkyRenderState sky, Matrix4fc modelViewMatrix, Runnable renderVanilla) {
        renderVanilla.run();
        MoCTwinSuns.draw(modelViewMatrix, sky.sunAngle);
        return true;
    }
}
