package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelJellyFish;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.passive.MoCEntityJellyFish;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Jellyfish renderer: a translucent, faintly grey-tinted body (legacy {@code glColor4f(0.8,0.8,0.8,0.7)})
 * with a gentle bell throb driven by {@code ageInTicks} (legacy pulsingSize). Only at NIGHT
 * ({@code state.jellyfishGlowing}, legacy {@code setGlowing(!isDaytime)}) does the bell switch to an
 * emissive (self-illuminating) render type and drop the grey tint for a full-bright bioluminescent glow;
 * by day it is a plain grey translucent jelly.
 */
public class MoCJellyFishRenderer extends MoCMobRenderer<MoCEntityJellyFish> {

    public MoCJellyFishRenderer(EntityRendererProvider.Context context) {
        super(context, MoCModelLayers.JELLYFISH, MoCModelJellyFish::new, 0.3F);
    }

    @Override
    protected RenderType getRenderType(MoCEntityRenderState state, boolean bodyVisible, boolean translucent,
            boolean glowingOutline) {
        Identifier tex = this.getTextureLocation(state);
        if (translucent) {
            return RenderTypes.entityTranslucentCullItemTarget(tex);
        } else if (bodyVisible) {
            // Emissive + no grey tint at night (bioluminescence); plain grey translucent by day.
            return state.jellyfishGlowing
                    ? RenderTypes.entityTranslucentEmissive(tex, false)
                    : RenderTypes.entityTranslucent(tex);
        } else {
            return glowingOutline ? RenderTypes.outline(tex) : null;
        }
    }

    @Override
    protected int getModelTint(MoCEntityRenderState state) {
        // No grey tint while glowing (full-bright glow); the daytime jelly keeps the 0.8-grey / 0.70-alpha wash.
        return state.jellyfishGlowing ? 0xFFFFFFFF : 0xB3CCCCCC;
    }

    @Override
    protected void setupRotations(MoCEntityRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        // A beached jellyfish (out of water, on the ground) flops onto its side (legacy on-ground pose).
        if (state.jellyfishBeached) {
            poseStack.translate(0.0F, 0.15F, 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
        }
    }

    @Override
    protected void scale(MoCEntityRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        float pulse = Mth.sin(state.ageInTicks * 0.15F) * 0.5F + 0.5F;
        float s = 1.0F + pulse * 0.10F; // ~±10% bell pulse
        poseStack.scale(s, s, s);
    }
}
