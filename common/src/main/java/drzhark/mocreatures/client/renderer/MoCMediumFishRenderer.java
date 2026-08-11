package drzhark.mocreatures.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import drzhark.mocreatures.client.MoCModelLayers;
import drzhark.mocreatures.client.model.MoCModelMediumFish;
import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.passive.MoCEntityMediumFish;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Medium fish renderer, reproducing the two whole-body rotations the legacy {@code MoCRenderMoC} pulled off
 * the entity in {@code preRenderCallback}: {@code adjustRoll(rollRotationOffset())} then
 * {@code adjustYaw(yawRotationOffset())}, then {@code stretch(getSizeFactor())}.
 *
 * <ul>
 *   <li><b>Yaw, always 90 degrees.</b> The legacy model is built along the X axis (nose at x=-11, tail fin at
 *       x=+16) rather than vanilla's -Z, so {@code MoCEntityMediumFish.yawRotationOffset()} swung it a
 *       quarter turn to face the direction of travel. Without this the fish would swim permanently sideways.</li>
 *   <li><b>The swim wiggle.</b> On top of that 90, a submerged fish adds the shared aquatic waggle
 *       {@code sin(ticksExisted * 0.5) * 8} degrees whenever it is moving ({@code MoCEntityAquatic}:993-1001)
 *       — the fishtailing that sells a swim. A beached fish returns a flat 90 with no wiggle
 *       ({@code MoCEntityMediumFish}:116-123).</li>
 *   <li><b>The beached roll.</b> {@code rollRotationOffset()} returns -90 out of water
 *       ({@code MoCEntityMediumFish}:125-131), rolling the fish onto its side on the sand; in water it is 0.</li>
 * </ul>
 *
 * <p>Both rotations go in {@code scale()} rather than {@code setupRotations()} because that is the hook
 * called at the same point in the matrix stack as the legacy {@code preRenderCallback}: after
 * {@code poseStack.scale(-1, -1, 1)} and before {@code translate(0, -1.501, 0)}
 * ({@code LivingEntityRenderer}:86-90), so the axes match legacy's one-for-one. They are issued in the same
 * order legacy issued its {@code glRotatef} calls, which is what decides that the roll acts on the
 * already-turned fish (a barrel roll about its own length) rather than tipping it nose-over-tail. The size
 * stretch that {@code MoCMobRenderer.scale} applies first is uniform, so running it before rather than after
 * the rotations makes no difference.</p>
 *
 * <p>Legacy's third whole-body offset — the in-water / out-of-water body translate — is <em>not</em> here:
 * legacy applied it inside the model, beneath these rotations, so it lives on the model's {@code fish} pivot
 * (see {@link MoCModelMediumFish}).</p>
 */
public class MoCMediumFishRenderer extends MoCMobRenderer<MoCEntityMediumFish> {

    public MoCMediumFishRenderer(EntityRendererProvider.Context context) {
        // Legacy shadow radius: MoCClientProxy registered all three fish with new MoCRenderMoC(..., 0.2F).
        super(context, MoCModelLayers.MEDIUM_FISH, MoCModelMediumFish::new, 0.2F);
    }

    @Override
    protected void scale(MoCEntityRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack); // legacy stretch(): the age-driven size factor

        // legacy adjustRoll: GL11.glRotatef(f, 0F, 0F, -1F) with f = -90 out of water, 0 in water.
        if (!state.isInWater) {
            poseStack.mulPose(Axis.ZN.rotationDegrees(-90.0F));
        }

        // legacy adjustYaw: GL11.glRotatef(f, 0.0F, -1.0F, 0.0F) with f = 90 (+ the swim wiggle in water).
        float yaw = 90.0F;
        if (state.isInWater && state.walkAnimationSpeed > 0.0F) {
            // Legacy gated the wiggle on motionX/motionZ != 0; walkAnimationSpeed is the render state's
            // equivalent "is actually moving" signal, and it is already interpolated for the partial tick.
            yaw += (float) (Math.sin(state.ageInTicks * 0.5F) * 8.0D);
        }
        poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
    }
}
