package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelFlameWraith;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.monster.MoCEntityFlameWraith;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/**
 * Flame-wraith renderer: an opaque, warm-tinted apparition re-rendered at full brightness so it glows like
 * it is ablaze (the entity also spawns flame + smoke particles). This replaces the earlier translucent-
 * emissive material, which sorted incorrectly against terrain, and the vanilla fire overlay, which drew a
 * solid wall of fire that hid the wraith entirely.
 */
public class MoCFlameWraithRenderer extends MoCMobRenderer<MoCEntityFlameWraith> {

    public MoCFlameWraithRenderer(EntityRendererProvider.Context context) {
        super(context, MoCModelLayers.FLAME_WRAITH, MoCModelFlameWraith::new, 0.75F);
        this.addLayer(new FlameGlowLayer(this));
    }

    @Override
    protected int getModelTint(MoCEntityRenderState state) {
        return 0xFFFFCC99; // warm orange bias
    }

    /** Re-renders the wraith with its own skin at full brightness so it self-illuminates (fiery glow). */
    private static final class FlameGlowLayer
            extends RenderLayer<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> {

        private static final Identifier GLOW =
                Identifier.fromNamespaceAndPath("mocreatures", "textures/models/flamewraith.png");

        private FlameGlowLayer(RenderLayerParent<MoCEntityRenderState, EntityModel<MoCEntityRenderState>> parent) {
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
