package drzhark.mocreatures.fabric.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import drzhark.mocreatures.client.MoCLairSky;
import drzhark.mocreatures.client.MoCTwinSuns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric twin-suns sky for the Wyvern Lair. Fabric API's {@code DimensionRenderingRegistry} can't be used
 * here — it ships intermediary-mapped and this project compiles against Mojang mappings under
 * {@code loom-no-remap}, so the API classes don't resolve. A Mixin instead references only Mojang-mapped
 * Minecraft (which equals the runtime names on unobfuscated 26.2) plus the mod's own {@link MoCTwinSuns},
 * so it needs no Fabric API. This is the loader-native counterpart of the NeoForge {@code CustomSkyboxRenderer}.
 *
 * <p>Two injections, both gated to the Lair:
 * <ul>
 *   <li>{@code renderSun} HEAD (cancellable) — suppress the vanilla sun so the twin suns fully replace it;
 *       the moon and stars still render normally.</li>
 *   <li>{@code renderSunMoonAndStars} TAIL — overlay the twin-suns disc, reusing the shared GPU draw. The
 *       injected {@code sunAngle} is the exact value (radians) vanilla feeds to {@code Axis.XP.rotation},
 *       so the disc tracks the day at the correct speed and sits where the vanilla sun would be.</li>
 * </ul>
 */
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    private static boolean mocreatures$inLair() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null
                && mc.level.dimension().equals(MoCLairSky.WYVERN_LAIR);
    }

    @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
    private void mocreatures$noVanillaSun(float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (mocreatures$inLair()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
    private void mocreatures$twinSuns(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle,
            MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci) {
        if (!mocreatures$inLair()) {
            return;
        }
        // The full sky model-view is the current model-view stack (camera base) composed with the sky
        // pose — the same base vanilla's renderSun multiplies in (getModelViewStack().mul(poseStack.pose())).
        Matrix4f full = new Matrix4f(RenderSystem.getModelViewStack()).mul(poseStack.last().pose());
        MoCTwinSuns.draw(full, sunAngle);
    }
}
