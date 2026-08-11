package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import drzhark.mocreatures.entity.passive.MoCEntityMole;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Mole model, converted faithfully from the legacy {@code MoCModelMole} ({@code ModelBase}). All eleven
 * cubes keep their original texture offsets, box dimensions, rotation points and baked rotations, and the
 * legacy gait (front paws and hind legs swinging about <em>Y</em>, not X — a mole paddles sideways rather
 * than striding) is preserved exactly.
 *
 * <h2>The burrow transform</h2>
 * Legacy split the mole's burrow rendering across two files:
 * <ul>
 *   <li>{@code MoCModelMole.render} wrapped the whole model in
 *       {@code glTranslatef(0F, mole.getAdjustedYOffset(), 0F)}, sinking it 0.3 / 1.0 / 0.1 blocks for
 *       states 1 / 2 / 3;</li>
 *   <li>{@code MoCRenderMoC.adjustPitch} applied {@code glRotatef(mole.pitchRotationOffset(), -1F, 0F, 0F)}
 *       about the entity's <em>feet</em>, before the standard {@code translate(0, -1.5078125, 0)}.</li>
 * </ul>
 * 26.2's {@code LivingEntityRenderer} bakes {@code translate(0, -1.501, 0)} in before the model runs and
 * the port's shared {@link drzhark.mocreatures.client.renderer.MoCMobRenderer} is not per-species, so both
 * transforms are reproduced inside this model instead, using a two-node root:
 * <pre>
 *   root      -&gt; translate(0, +1.501, 0) then rotate about X   (puts the pivot back at the feet)
 *   body_root -&gt; translate(0, -1.501 + yOffset, 0)             (undoes it, plus the legacy sink)
 * </pre>
 * Composed with the renderer's own {@code translate(0, -1.501, 0)} this is algebraically identical to the
 * legacy {@code R(pitch) . T(0,-1.5078,0) . T(0,yOffset,0)} — the rotation still pivots at ground level and
 * the sink is still applied last. Model-space {@code +Y} is downward (the renderer has already applied
 * {@code scale(-1,-1,1)}), which is why {@code yOffset} is added rather than subtracted, and why the legacy
 * rotation about {@code -X} becomes {@code root.xRot = -pitchDegrees}.
 */
public class MoCModelMole extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    /** 1.501 blocks expressed in model units (1 unit = 1/16 block) — the renderer's baked Y translation. */
    private static final float FEET_PIVOT = 1.501F * 16.0F;

    private final ModelPart bodyRoot;
    private final ModelPart nose;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart back;
    private final ModelPart tail;
    private final ModelPart lLeg;
    private final ModelPart lFingers;
    private final ModelPart rLeg;
    private final ModelPart rFingers;
    private final ModelPart lRearLeg;
    private final ModelPart rRearLeg;

    public MoCModelMole(ModelPart root) {
        super(root);
        this.bodyRoot = root.getChild("body_root");
        this.nose = this.bodyRoot.getChild("nose");
        this.head = this.bodyRoot.getChild("head");
        this.body = this.bodyRoot.getChild("body");
        this.back = this.bodyRoot.getChild("back");
        this.tail = this.bodyRoot.getChild("tail");
        this.lLeg = this.bodyRoot.getChild("l_leg");
        this.lFingers = this.bodyRoot.getChild("l_fingers");
        this.rLeg = this.bodyRoot.getChild("r_leg");
        this.rFingers = this.bodyRoot.getChild("r_fingers");
        this.lRearLeg = this.bodyRoot.getChild("l_rear_leg");
        this.rRearLeg = this.bodyRoot.getChild("r_rear_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Transform-only node; carries every visible cube so the root above it can pivot at ground level.
        PartDefinition bodyRoot = root.addOrReplaceChild("body_root",
                CubeListBuilder.create(), PartPose.ZERO);

        bodyRoot.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 25).addBox(-1.0F, 0.0F, -4.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, -6.0F, 0.2617994F, 0.0F, 0.0F));
        bodyRoot.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 18).addBox(-3.0F, -2.0F, -2.0F, 6.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 20.0F, -6.0F));
        bodyRoot.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 17.0F, -6.0F));
        bodyRoot.addOrReplaceChild("back",
                CubeListBuilder.create().texOffs(18, 16).addBox(-4.0F, -3.0F, 0.0F, 8.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 21.0F, 4.0F));
        bodyRoot.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(52, 8).addBox(-0.5F, 0.0F, 1.0F, 1.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 21.0F, 6.0F, -0.3490659F, 0.0F, 0.0F));
        // Front digging paws: the broad shovel (l_leg/r_leg) plus its separate claw strip (l_fingers/r_fingers).
        bodyRoot.addOrReplaceChild("l_leg",
                CubeListBuilder.create().texOffs(10, 25).addBox(0.0F, -2.0F, -1.0F, 6.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, 21.0F, -4.0F, 0.0F, 0.0F, 0.2268928F));
        bodyRoot.addOrReplaceChild("l_fingers",
                CubeListBuilder.create().texOffs(44, 8).addBox(5.0F, -2.0F, 1.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 21.0F, -4.0F, 0.0F, 0.0F, 0.2268928F));
        bodyRoot.addOrReplaceChild("r_leg",
                CubeListBuilder.create().texOffs(26, 25).addBox(-6.0F, -2.0F, -1.0F, 6.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, 21.0F, -4.0F, 0.0F, 0.0F, -0.2268928F));
        bodyRoot.addOrReplaceChild("r_fingers",
                CubeListBuilder.create().texOffs(48, 8).addBox(-6.0F, -2.0F, 1.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 21.0F, -4.0F, 0.0F, 0.0F, -0.2268928F));
        bodyRoot.addOrReplaceChild("l_rear_leg",
                CubeListBuilder.create().texOffs(36, 0).addBox(0.0F, -2.0F, -1.0F, 2.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(3.0F, 22.0F, 5.0F, -0.2792527F, 0.5235988F, 0.0F));
        bodyRoot.addOrReplaceChild("r_rear_leg",
                CubeListBuilder.create().texOffs(50, 0).addBox(-2.0F, -2.0F, -1.0F, 2.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(-3.0F, 22.0F, 5.0F, -0.2792527F, -0.5235988F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state); // resetPose(): restores every part, including both root nodes

        // ---- legacy setRotationAngles ----------------------------------------------------------------
        float headYaw = state.yRot * DEG_TO_RAD;   // legacy f3 / 57.29578F
        float headPitch = state.xRot * DEG_TO_RAD; // legacy f4 / 57.29578F
        this.head.yRot = headYaw;
        this.head.xRot = headPitch;
        // The snout keeps its baked 15-degree downward tilt and rides on top of the head's aim.
        this.nose.xRot = 0.2617994F + this.head.xRot;
        this.nose.yRot = this.head.yRot;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float rLegSwing = Mth.cos((limbSwing * 1.0F) + 3.141593F) * 0.8F * limbAmount;
        float lLegSwing = Mth.cos(limbSwing * 1.0F) * 0.8F * limbAmount;

        // Front paws sweep about Y (a mole shovels sideways); the claw strips track their own paw.
        this.rLeg.yRot = rLegSwing;
        this.rFingers.yRot = this.rLeg.yRot;
        this.lLeg.yRot = lLegSwing;
        this.lFingers.yRot = this.lLeg.yRot;
        // Hind legs keep their baked 30-degree splay and counter-swing against the opposite front paw.
        this.rRearLeg.yRot = -0.5235988F + lLegSwing;
        this.lRearLeg.yRot = 0.5235988F + rLegSwing;
        // Legacy line 126 verbatim. lLeg.rotateAngleX is never assigned anywhere in the legacy model, so
        // this always evaluated to 0 and the tail never actually waggled; kept for exact parity rather than
        // "fixed" to lLeg.yRot, which would introduce motion the original never had.
        this.tail.zRot = this.lLeg.xRot * 0.625F;

        // ---- burrow transform (see the class javadoc for the algebra) ---------------------------------
        int moleState = state.moleState;
        this.root.y = FEET_PIVOT;
        this.root.xRot = -MoCEntityMole.renderPitch(moleState) * DEG_TO_RAD;
        this.bodyRoot.y = -FEET_PIVOT + MoCEntityMole.renderYOffset(moleState) * 16.0F;
    }
}
