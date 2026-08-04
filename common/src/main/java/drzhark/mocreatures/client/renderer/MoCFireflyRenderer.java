package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelFirefly;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.passive.MoCEntityFirefly;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/**
 * Firefly renderer: the normal firefly.png body plus a full-bright glow layer (fireflyglow.png) that
 * makes the abdomen shine — the 26.2 port of the legacy additive glowing-belly pass (mirrors the golem
 * aura layer, the sanctioned emissive pattern in this codebase).
 */
public class MoCFireflyRenderer extends MoCMobRenderer<MoCEntityFirefly> {

    public MoCFireflyRenderer(EntityRendererProvider.Context context) {
        super(context, MoCModelLayers.FIREFLY, MoCModelFirefly::new, 0.3F);
        this.addLayer(new FireflyGlowLayer(this));
    }

    @Override
    protected void setupRotations(MoCEntityRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        // A flying firefly angles its body forward as it drifts through the air (legacy rotateFirefly:
        // glRotatef(40, -1, 0, 0)).
        if (state.flying) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-40.0F));
        }
    }

    /** Re-renders the firefly with the glow skin at full brightness so its belly reads as a light. */
    private static final class FireflyGlowLayer
            extends RenderLayer<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> {

        private static final Identifier GLOW =
                Identifier.fromNamespaceAndPath("mocreatures", "textures/models/fireflyglow.png");

        private FireflyGlowLayer(RenderLayerParent<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                MoCEntityRenderState state, float yRot, float xRot) {
            renderColoredCutoutModel(getParentModel(), GLOW, poseStack, collector,
                    0x00F000F0, state, 0xFFFFFFFF, 0);
        }
    }
}
