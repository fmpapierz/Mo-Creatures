package drzhark.mocreatures.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Loader-agnostic GPU draw of the Wyvern Lair twin-suns celestial disc ({@code twinsuns.png}). Both the
 * NeoForge {@code CustomSkyboxRenderer} and the Fabric {@code DimensionRenderingRegistry.SkyRenderer}
 * call {@link #draw(Matrix4fc, float)} after the vanilla sky is drawn, so only the loader-specific hook
 * (and the vanilla-sky handling) differs between the two.
 *
 * <p>The GPU sequence mirrors {@code net.minecraft.client.renderer.SkyRenderer.renderSun} (verified via
 * {@code javap} against the 26.2 merged deobf jar): build a POSITION_TEX quad once, place it on the
 * model-view stack (rotated by the sun angle so the disc tracks day/night), then issue a CELESTIAL
 * pipeline indexed draw against the main render target, binding {@code twinsuns.png} as {@code Sampler0}.
 * The draw is wrapped so a wrong GPU call logs once and is skipped rather than crashing.
 */
public final class MoCTwinSuns {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Lazily built + cached twin-suns quad (POSITION_TEX, 4 verts). Shared across loaders. */
    private static GpuBuffer quad;
    /** Ensures the fallback-on-error message logs only once, not every frame. */
    private static boolean loggedError;

    private MoCTwinSuns() {}

    /**
     * Draws the twin-suns disc into the current sky pass.
     *
     * @param modelView the sky model-view matrix (camera rotation; no world translation)
     * @param sunAngle  the celestial sun angle in radians, so the disc tracks the day like the vanilla sun
     */
    public static void draw(Matrix4fc modelView, float sunAngle) {
        try {
            drawInternal(modelView, sunAngle);
        } catch (Throwable t) {
            if (!loggedError) {
                loggedError = true;
                LOGGER.error("[MoCreatures] Wyvern Lair twin-suns sky draw failed; skipping the disc.", t);
            }
        }
    }

    private static void drawInternal(Matrix4fc modelView, float sunAngle) {
        Minecraft mc = Minecraft.getInstance();

        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        try {
            // Use the passed sky model-view as the BASE (set, not mul: mul double-composed the stale
            // stack and inverted the vertical camera axis). Then mirror renderSunMoonAndStars (YP -90) +
            // renderSun (translate up 100, scale 30).
            mv.set(modelView);
            mv.rotateY((float) Math.toRadians(-90.0));
            mv.rotateX(sunAngle);
            mv.translate(0.0F, 100.0F, 0.0F);
            mv.scale(30.0F, 30.0F, 30.0F);

            GpuBufferSlice dynTransform = RenderSystem.getDynamicUniforms()
                    .writeTransform(new Matrix4f(mv), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F));

            RenderTarget rt = mc.gameRenderer.mainRenderTarget();
            GpuTextureView color = rt.getColorTextureView();
            GpuTextureView depth = rt.getDepthTextureView();

            AbstractTexture tex = mc.getTextureManager().getTexture(MoCLairSky.TWIN_SUNS_TEXTURE);
            GpuTextureView texView = tex.getTextureView();
            GpuSampler texSampler = tex.getSampler();

            RenderSystem.AutoStorageIndexBuffer idx = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            GpuBuffer idxBuffer = idx.getBuffer(6);

            GpuDevice device = RenderSystem.getDevice();
            CommandEncoder encoder = device.createCommandEncoder();
            GpuBuffer quadBuffer = getOrBuildQuad(device);

            try (RenderPass pass = encoder.createRenderPass(
                    () -> "mocreatures twin suns",
                    color, Optional.empty(),
                    depth, OptionalDouble.empty())) {
                pass.setPipeline(RenderPipelines.CELESTIAL);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynTransform);
                pass.bindTexture("Sampler0", texView, texSampler);
                pass.setVertexBuffer(0, quadBuffer.slice());
                pass.setIndexBuffer(idxBuffer, idx.type());
                pass.drawIndexed(6, 1, 0, 0, 0);
            }
        } finally {
            mv.popMatrix();
        }
    }

    private static GpuBuffer getOrBuildQuad(GpuDevice device) {
        GpuBuffer cached = quad;
        if (cached != null) {
            return cached;
        }
        VertexFormat format = DefaultVertexFormat.POSITION_TEX;
        ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(format.getVertexSize() * 4);
        BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, format);
        builder.addVertex(-1.0F, 0.0F, -1.0F).setUv(0.0F, 0.0F);
        builder.addVertex(1.0F, 0.0F, -1.0F).setUv(1.0F, 0.0F);
        builder.addVertex(1.0F, 0.0F, 1.0F).setUv(1.0F, 1.0F);
        builder.addVertex(-1.0F, 0.0F, 1.0F).setUv(0.0F, 1.0F);

        MeshData mesh = builder.buildOrThrow();
        try {
            GpuBuffer buffer = device.createBuffer(
                    () -> "mocreatures twin suns quad",
                    GpuBuffer.USAGE_VERTEX,
                    mesh.vertexBuffer());
            quad = buffer;
            return buffer;
        } finally {
            mesh.close();
            bytes.close();
        }
    }
}
