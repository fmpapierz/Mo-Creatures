package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelSmallFish;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.passive.MoCEntitySmallFish;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Small fish renderer. Everything about a small fish except one transform is generic, so this is
 * {@link MoCMobRenderer} plus the legacy beached roll.
 *
 * <p>Legacy {@code MoCEntitySmallFish.rollRotationOffset()}:166-172 returned {@code -90} the moment the fish
 * left the water and {@code 0} while swimming, and {@code MoCRenderMoC.adjustRoll}:140-146 fed that to
 * {@code glRotatef(f, 0, 0, -1)} — a +90 degree turn about +Z, which lays the fish flat on its side on the
 * shore instead of leaving it standing upright in the air. That rotation has to pivot about the entity's own
 * origin (its feet), which sits above the model root, so it belongs here rather than on a {@code ModelPart};
 * the same reason {@code MoCJellyFishRenderer} applies the beached jellyfish flop in
 * {@code setupRotations}. A Z rotation is unaffected by the {@code scale(-1, -1, 1)} model flip that follows,
 * so applying it here is exactly equivalent to the legacy {@code preRenderCallback} slot.</p>
 *
 * <p>The complementary 0.5-block drop that puts the rolled body ON the sand (legacy
 * {@code getAdjustedYOffset()}) lives in {@link MoCModelSmallFish#setupAnim}, matching the legacy split
 * between the renderer's rotations and the model's own translate.</p>
 */
public class MoCSmallFishRenderer extends MoCMobRenderer<MoCEntitySmallFish> {

    public MoCSmallFishRenderer(EntityRendererProvider.Context context) {
        // Legacy shadow size: MoCClientProxy:321-328 registers every small fish with MoCRenderMoC(..., 0.1F).
        super(context, MoCModelLayers.SMALL_FISH, MoCModelSmallFish::new, 0.1F);
    }

    @Override
    protected void setupRotations(MoCEntityRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        if (!state.isInWater) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
