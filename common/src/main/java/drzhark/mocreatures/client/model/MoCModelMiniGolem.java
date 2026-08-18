package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Mini golem model, converted faithfully from the legacy {@code MoCModelMiniGolem} ({@code ModelBase},
 * 12.0.5). Every box origin, texture offset and pivot is carried over verbatim; only the 1.12 {@code
 * ModelRenderer} plumbing is replaced by the 26.2 {@link ModelPart} / {@link LayerDefinition} pipeline.
 *
 * <p>Two legacy details are worth calling out, because they are the model's whole personality:</p>
 * <ul>
 *   <li><strong>The angry skin swap.</strong> Legacy {@code render()} (MoCModelMiniGolem:112-118) chose
 *       between two <em>duplicate</em> head/body boxes drawn from different regions of the same 64x64
 *       sheet: the calm pair ({@code Head} texOffs 30,0 / {@code Body} texOffs 0,0) and the red-hot pair
 *       ({@code HeadRed} texOffs 30,29 / {@code BodyRed} texOffs 0,28). 26.2 models submit the whole part
 *       tree in one pass, so the swap is expressed by toggling {@link ModelPart#visible} on the two pairs
 *       — exactly the pattern the port already uses for the ray's manta/sting parts and the litter box's
 *       clean/used cube.</li>
 *   <li><strong>The overhead heave.</strong> While the golem is hoisting a ripped-up block
 *       (MoCModelMiniGolem:153-157) both shoulders snap to {@code xRot = -180 degrees / 57.29578}, i.e.
 *       exactly {@code -PI}: the arms rotate a full half-turn about the pivot and end up pointing straight
 *       up, holding the boulder over its head. The arms and their two stone rings simply copy the
 *       shoulder's rotation, so the whole limb moves as one rigid piece (MoCModelMiniGolem:165-169).</li>
 * </ul>
 *
 * <p>The head sits corner-on: its base pose is a -45 degree yaw ({@code -0.7853982}), and the tracked head
 * yaw is <em>added</em> to that base. Legacy deliberately ignored head pitch ({@code f4} is never read), so
 * the mini golem never looks up or down — that is preserved here rather than "fixed".</p>
 */
public class MoCModelMiniGolem extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /** Legacy base head yaw: the cube head is mounted corner-forward, rotated -45 degrees about Y. */
    private static final float HEAD_BASE_YAW = -0.7853982F;

    /**
     * Legacy overhead-carry shoulder pitch. The legacy source wrote it as {@code -180F / this.radianF}
     * with {@code radianF = 57.29578F}, i.e. -180 degrees expressed in radians = -PI. Kept in that exact
     * form so the value is traceable to the legacy line.
     */
    private static final float ARMS_OVERHEAD = -180.0F / 57.29578F;

    private final ModelPart head;
    private final ModelPart headRed;
    private final ModelPart body;
    private final ModelPart bodyRed;
    private final ModelPart leftShoulder;
    private final ModelPart leftArm;
    private final ModelPart leftArmRingA;
    private final ModelPart leftArmRingB;
    private final ModelPart rightShoulder;
    private final ModelPart rightArm;
    private final ModelPart rightArmRingA;
    private final ModelPart rightArmRingB;
    private final ModelPart rightLeg;
    private final ModelPart rightFoot;
    private final ModelPart leftLeg;
    private final ModelPart leftFoot;

    public MoCModelMiniGolem(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.headRed = root.getChild("head_red");
        this.body = root.getChild("body");
        this.bodyRed = root.getChild("body_red");
        this.leftShoulder = root.getChild("left_shoulder");
        this.leftArm = root.getChild("left_arm");
        this.leftArmRingA = root.getChild("left_arm_ring_a");
        this.leftArmRingB = root.getChild("left_arm_ring_b");
        this.rightShoulder = root.getChild("right_shoulder");
        this.rightArm = root.getChild("right_arm");
        this.rightArmRingA = root.getChild("right_arm_ring_a");
        this.rightArmRingB = root.getChild("right_arm_ring_b");
        this.rightLeg = root.getChild("right_leg");
        this.rightFoot = root.getChild("right_foot");
        this.leftLeg = root.getChild("left_leg");
        this.leftFoot = root.getChild("left_foot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head: the calm skin and, at the same pivot and box, the red-hot "angry" skin.
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(30, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, HEAD_BASE_YAW, 0.0F));
        root.addOrReplaceChild("head_red",
                CubeListBuilder.create().texOffs(30, 29).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, HEAD_BASE_YAW, 0.0F));

        // Body: the big 10x10x10 stone block, again duplicated for the angry skin.
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("body_red",
                CubeListBuilder.create().texOffs(0, 28).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        // Left arm assembly: a tiny shoulder stud, the arm itself, and two stone rings around it.
        // All four share the pivot (5, 11, 0) so they swing as one rigid limb.
        root.addOrReplaceChild("left_shoulder",
                CubeListBuilder.create().texOffs(0, 4).addBox(0.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(5.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 48).addBox(1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("left_arm_ring_a",
                CubeListBuilder.create().texOffs(20, 20).addBox(0.5F, 1.0F, -2.5F, 5.0F, 3.0F, 5.0F),
                PartPose.offset(5.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("left_arm_ring_b",
                CubeListBuilder.create().texOffs(20, 20).addBox(0.5F, 5.0F, -2.5F, 5.0F, 3.0F, 5.0F),
                PartPose.offset(5.0F, 11.0F, 0.0F));

        // Right arm assembly, mirrored about X at pivot (-5, 11, 0).
        root.addOrReplaceChild("right_shoulder",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(-5.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(16, 48).addBox(-5.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("right_arm_ring_a",
                CubeListBuilder.create().texOffs(0, 20).addBox(-5.5F, 1.0F, -2.5F, 5.0F, 3.0F, 5.0F),
                PartPose.offset(-5.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("right_arm_ring_b",
                CubeListBuilder.create().texOffs(0, 20).addBox(-5.5F, 5.0F, -2.5F, 5.0F, 3.0F, 5.0F),
                PartPose.offset(-5.0F, 11.0F, 0.0F));

        // Legs: a stubby pillar each, with a separate front lip ("foot") that swings with it.
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(40, 9).addBox(-2.5F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(-2.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("right_foot",
                CubeListBuilder.create().texOffs(15, 22).addBox(-2.5F, 5.0F, -3.0F, 4.0F, 1.0F, 1.0F),
                PartPose.offset(-2.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(40, 19).addBox(-1.5F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(2.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("left_foot",
                CubeListBuilder.create().texOffs(15, 20).addBox(-1.5F, 5.0F, -3.0F, 4.0F, 1.0F, 1.0F),
                PartPose.offset(2.0F, 18.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        // --- legs (legacy setRotationAngles:142-148) ------------------------------------------------
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float rightLegXRot = Mth.cos((limbSwing * 0.6662F) + 3.141593F) * 0.8F * limbAmount;
        float leftLegXRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;

        this.rightLeg.xRot = rightLegXRot;
        this.rightFoot.xRot = rightLegXRot;
        this.leftLeg.xRot = leftLegXRot;
        this.leftFoot.xRot = leftLegXRot;

        // --- head (legacy setRotationAngles:141, 150-151) -------------------------------------------
        // Tracked yaw only; legacy never applied head pitch to this model.
        float headYaw = HEAD_BASE_YAW + (state.yRot * DEG_TO_RAD);
        this.head.yRot = headYaw;
        this.headRed.yRot = headYaw;

        // --- angry skin swap (legacy render:112-118) ------------------------------------------------
        // Legacy picked one of two duplicate head/body boxes at draw time; here the unused pair is hidden.
        boolean angry = state.miniGolemAngry;
        this.head.visible = !angry;
        this.body.visible = !angry;
        this.headRed.visible = angry;
        this.bodyRed.visible = angry;

        // --- arms (legacy setRotationAngles:153-169) ------------------------------------------------
        if (state.miniGolemHasRock) {
            // Hoisting a block: both arms rotate a full half-turn about the pivot, ending straight up.
            this.leftShoulder.zRot = 0.0F;
            this.leftShoulder.xRot = ARMS_OVERHEAD;
            this.rightShoulder.zRot = 0.0F;
            this.rightShoulder.xRot = ARMS_OVERHEAD;
        } else {
            // Idle/walking: a slow outward sway on Z (an animation-timer breath, not tied to the gait)
            // plus a counter-phase forward swing that mirrors the opposite leg.
            float sway = Mth.cos(state.ageInTicks * 0.09F) * 0.05F;
            this.leftShoulder.zRot = sway - 0.05F;
            this.leftShoulder.xRot = rightLegXRot;
            this.rightShoulder.zRot = -sway + 0.05F;
            this.rightShoulder.xRot = leftLegXRot;
        }

        // The arm and its two rings are rigidly bolted to the shoulder (legacy copied the angles verbatim
        // rather than parenting the parts, and this port keeps the same flat part list).
        this.rightArm.xRot = this.rightArmRingA.xRot = this.rightArmRingB.xRot = this.rightShoulder.xRot;
        this.rightArm.zRot = this.rightArmRingA.zRot = this.rightArmRingB.zRot = this.rightShoulder.zRot;
        this.leftArm.xRot = this.leftArmRingA.xRot = this.leftArmRingB.xRot = this.leftShoulder.xRot;
        this.leftArm.zRot = this.leftArmRingA.zRot = this.leftArmRingB.zRot = this.leftShoulder.zRot;
    }
}
