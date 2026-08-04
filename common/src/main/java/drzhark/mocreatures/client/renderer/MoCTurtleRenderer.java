package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelTurtle;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.passive.MoCEntityTurtle;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Turtle renderer. Adds the legacy on-its-back flip: when the turtle is knocked upside-down the whole
 * model is rotated 180° about the Z axis (legacy {@code glRotatef(180, 0,0,-1)}). This flip cannot live
 * in the model's {@code setupAnim} — that only rotates individual parts about their own pivots — so it is
 * applied here to the whole {@link PoseStack}. The head/leg retract for the hiding pose IS a per-part
 * change and stays in {@link MoCModelTurtle#setupAnim}.
 */
public class MoCTurtleRenderer extends MoCMobRenderer<MoCEntityTurtle> {

    public MoCTurtleRenderer(EntityRendererProvider.Context context) {
        super(context, MoCModelLayers.TURTLE, MoCModelTurtle::new, 0.3F);
    }

    @Override
    protected void setupRotations(MoCEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        // Smooth roll: the turtle rotates from upright (0) to fully on its back (180°) over the flip
        // window, so a knocked-back turtle tumbles through the air and lands on its shell rather than
        // snapping over instantly. The lift ramps in with the roll so the shell settles on the ground.
        float p = state.turtleFlipProgress;
        if (p > 0.0F) {
            poseStack.translate(0.0F, 0.55F * p, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F * p));
        }
    }
}
