package drzhark.mocreatures.neoforge.client;

import drzhark.mocreatures.client.MoCTwinSuns;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import org.joml.Matrix4fc;

/**
 * NeoForge custom skybox renderer for the Lair dimensions. One instance per lair, each with its own
 * celestial texture: twin suns for {@code mocreatures:wyvern_lair}, the single ember sun for
 * {@code mocreatures:ogre_lair}.
 *
 * <p>Registered against the NeoForge custom-skybox environment attribute ({@code neoforge:custom_skybox}
 * → the lair's own id, set in the NeoForge-only copy of that lair's
 * {@code data/mocreatures/dimension_type/*.json} — the attribute is stripped from the common
 * copy because Fabric can't parse it). NeoForge only invokes each instance for its own lair.
 *
 * <p>Draws the vanilla lair sky first (colored sky + normal sun/moon/stars via {@code renderVanilla}),
 * then overlays the celestial disc via the shared, loader-agnostic
 * {@link MoCTwinSuns#draw(Matrix4fc, float, Identifier)}.
 */
public final class MoCLairSkyboxRenderer implements CustomSkyboxRenderer {

    /** The celestial-disc texture this instance binds (e.g. {@code MoCLairSky.TWIN_SUNS_TEXTURE}). */
    private final Identifier celestialTexture;

    public MoCLairSkyboxRenderer(Identifier celestialTexture) {
        this.celestialTexture = celestialTexture;
    }

    @Override
    public boolean renderSky(LevelRenderState level, SkyRenderState sky, Matrix4fc modelViewMatrix, Runnable renderVanilla) {
        renderVanilla.run();
        MoCTwinSuns.draw(modelViewMatrix, sky.sunAngle, celestialTexture);
        return true;
    }
}
