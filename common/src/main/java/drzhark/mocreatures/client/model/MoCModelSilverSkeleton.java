package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Silver skeleton model, converted faithfully from the legacy {@code MoCModelSilverSkeleton}
 * ({@code ModelBase}, 12.0.5 {@code client/model/MoCModelSilverSkeleton.java}). Geometry, texture offsets
 * and the legacy parent-child hierarchy (thigh -&gt; shin -&gt; foot) are preserved verbatim, as is the
 * animation:
 *
 * <ul>
 *   <li><b>Two katanas.</b> Each arm carries a hand plus three blade cubes ({@code SwordA} guard,
 *       {@code SwordB} hilt, {@code SwordC} blade). {@code SwordC} is a zero-thickness box — the legacy
 *       flat double-sided blade plane — which converts as {@code addBox(..., 0.0F, 3.0F, 10.0F)} and still
 *       renders from both sides in 26.2. Every one of those cubes is a SIBLING of the arm with the same
 *       pivot (legacy never parented them), so {@link #setupAnim} copies the arm's rotation onto all four
 *       parts by hand, exactly as {@code setRotationAngles:218-226} did.</li>
 *   <li><b>Per-arm swing.</b> The legacy model read {@code attackCounterLeft/Right} straight off the entity;
 *       the port reads the same two counters out of the render state. While a counter is non-zero that arm
 *       (and its blade) sweeps through {@code -(cos(tick * 0.18) * 3)}, taking it from raised-overhead to
 *       chopped-down over the 10-tick window; while it is zero the arm falls back to the walking swing plus
 *       an idle sway.</li>
 *   <li><b>Sprint lean.</b> Legacy {@code render:143-148} pushed a {@code glRotatef(limbAmount * -20, -1, 0, 0)}
 *       around the whole model while sprinting at a decent clip, i.e. up to a 20-degree forward pitch. The port
 *       applies the same pitch to the model root, which is the same transform in the same space.</li>
 *   <li><b>Seated pose.</b> Legacy {@code setRotationAngles:228-239} folded the legs up when the skeleton was
 *       riding (thighs and knees at -60 degrees, splayed 20 degrees outward), which is what an undead-horse
 *       cavalry rider looked like. Kept. The accompanying legacy {@code glTranslatef(0, 0.5F, 0)} seat nudge is
 *       NOT ported: 26.2 positions a passenger from the mount's own passenger attachment point, so shifting
 *       the rider's model would double-correct it.</li>
 * </ul>
 */
public class MoCModelSilverSkeleton extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float PI = 3.141593F;

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart rightHand;
    private final ModelPart rightSwordA;
    private final ModelPart rightSwordB;
    private final ModelPart rightSwordC;
    private final ModelPart leftArm;
    private final ModelPart leftHand;
    private final ModelPart leftSwordA;
    private final ModelPart leftSwordB;
    private final ModelPart leftSwordC;
    private final ModelPart rightThigh;
    private final ModelPart rightKnee;
    private final ModelPart rightLeg;
    private final ModelPart leftThigh;
    private final ModelPart leftKnee;
    private final ModelPart leftLeg;

    public MoCModelSilverSkeleton(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.rightHand = root.getChild("right_hand");
        this.rightSwordA = root.getChild("right_sword_a");
        this.rightSwordB = root.getChild("right_sword_b");
        this.rightSwordC = root.getChild("right_sword_c");
        this.leftArm = root.getChild("left_arm");
        this.leftHand = root.getChild("left_hand");
        this.leftSwordA = root.getChild("left_sword_a");
        this.leftSwordB = root.getChild("left_sword_b");
        this.leftSwordC = root.getChild("left_sword_c");
        this.rightThigh = root.getChild("right_thigh");
        this.rightKnee = root.getChild("right_knee");
        this.rightLeg = this.rightThigh.getChild("right_leg");
        this.leftThigh = root.getChild("left_thigh");
        this.leftKnee = root.getChild("left_knee");
        this.leftLeg = this.leftThigh.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        // The samurai back-banner (sashimono), pitched slightly back off the spine.
        root.addOrReplaceChild("back",
                CubeListBuilder.create().texOffs(44, 54).addBox(-4.0F, -4.0F, 0.5F, 8.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 2.0F, 2.0F, -0.1570796F, 0.0F, 0.0F));

        // ---- Right arm + katana (arm, forearm/hand and the three blade cubes all share the shoulder pivot) ----
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(48, 31).addBox(-3.0F, -2.5F, -2.5F, 4.0F, 11.0F, 4.0F),
                PartPose.offset(-5.0F, 1.0F, 0.0F));
        root.addOrReplaceChild("right_hand",
                CubeListBuilder.create().texOffs(24, 16).addBox(-2.5F, -2.0F, -2.0F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-5.0F, 1.0F, 0.0F));
        root.addOrReplaceChild("right_sword_a",
                CubeListBuilder.create().texOffs(52, 46).addBox(-1.5F, 8.5F, -3.0F, 1.0F, 1.0F, 5.0F),
                PartPose.offset(-5.0F, 1.0F, 0.0F));
        root.addOrReplaceChild("right_sword_b",
                CubeListBuilder.create().texOffs(48, 50).addBox(-1.5F, 7.5F, -4.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-5.0F, 1.0F, 0.0F));
        // Zero-thickness blade plane (legacy addBox(..., 0, 3, 10)) — renders from both sides.
        root.addOrReplaceChild("right_sword_c",
                CubeListBuilder.create().texOffs(28, 28).addBox(-1.0F, 7.5F, -14.0F, 0.0F, 3.0F, 10.0F),
                PartPose.offset(-5.0F, 1.0F, 0.0F));

        // ---- Left arm + katana ----
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(48, 16).addBox(-1.0F, -2.5F, -2.5F, 4.0F, 11.0F, 4.0F),
                PartPose.offset(5.0F, 1.0F, 0.0F));
        root.addOrReplaceChild("left_hand",
                CubeListBuilder.create().texOffs(36, 16).addBox(-0.5F, -2.0F, -2.0F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(5.0F, 1.0F, 0.0F));
        root.addOrReplaceChild("left_sword_a",
                CubeListBuilder.create().texOffs(52, 46).addBox(0.5F, 8.5F, -3.0F, 1.0F, 1.0F, 5.0F),
                PartPose.offset(5.0F, 1.0F, 0.0F));
        root.addOrReplaceChild("left_sword_b",
                CubeListBuilder.create().texOffs(48, 46).addBox(0.5F, 7.5F, -4.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(5.0F, 1.0F, 0.0F));
        root.addOrReplaceChild("left_sword_c",
                CubeListBuilder.create().texOffs(28, 31).addBox(1.0F, 7.5F, -14.0F, 0.0F, 3.0F, 10.0F),
                PartPose.offset(5.0F, 1.0F, 0.0F));

        // ---- Right leg: thigh -> shin -> foot, with a separate knee-plate cube on the thigh pivot ----
        PartDefinition rightThigh = root.addOrReplaceChild("right_thigh",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(-2.0F, 10.5F, 0.0F));
        root.addOrReplaceChild("right_knee",
                CubeListBuilder.create().texOffs(0, 46).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(-2.0F, 10.5F, 0.0F));
        PartDefinition rightLeg = rightThigh.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 25).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        rightLeg.addOrReplaceChild("right_foot",
                CubeListBuilder.create().texOffs(0, 54).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 2.0F, 0.0F));

        // ---- Left leg ----
        PartDefinition leftThigh = root.addOrReplaceChild("left_thigh",
                CubeListBuilder.create().texOffs(12, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(2.0F, 10.5F, 0.0F));
        root.addOrReplaceChild("left_knee",
                CubeListBuilder.create().texOffs(16, 46).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(2.0F, 10.5F, 0.0F));
        PartDefinition leftLeg = leftThigh.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(12, 25).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        leftLeg.addOrReplaceChild("left_foot",
                CubeListBuilder.create().texOffs(16, 54).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        this.head.yRot = state.yRot * DEG_TO_RAD;
        this.head.xRot = state.xRot * DEG_TO_RAD;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;

        // Legacy leg phases: the right thigh leads by half a cycle. Note the ARMS are driven by the OPPOSITE
        // leg's phase (left arm <- right leg phase), the usual contra-lateral walk swing.
        float rLegXRot = Mth.cos((limbSwing * 0.6662F) + PI) * 0.8F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;
        float rLegXRotB = rLegXRot;
        float lLegXRotB = lLegXRot;

        int leftSwing = state.silverSkeletonLeftSwing;
        int rightSwing = state.silverSkeletonRightSwing;

        // Idle sway (legacy): a slow ~0.05 rad breathing roll on both shoulders. Legacy only assigned zRot in
        // the non-attacking branch and let the previous frame's value persist mid-swing; 26.2 resets every part
        // to its baked pose each frame, so the sway is applied unconditionally — visually identical, and it
        // stops the arm snapping to zero roll the instant a swing starts.
        float sway = Mth.cos(ageInTicks * 0.09F) * 0.05F;
        this.leftArm.zRot = sway - 0.05F;
        this.rightArm.zRot = -sway + 0.05F;

        if (leftSwing == 0) {
            this.leftArm.xRot = rLegXRot;
        } else {
            // Legacy: -(cos(counter * 0.18) * 3) over counter 1..10 — a full overhead-to-downward chop.
            this.leftArm.xRot = -(Mth.cos(leftSwing * 0.18F) * 3.0F);
        }
        if (rightSwing == 0) {
            this.rightArm.xRot = lLegXRot;
        } else {
            this.rightArm.xRot = -(Mth.cos(rightSwing * 0.18F) * 3.0F);
        }

        // Hand + blade cubes are siblings sharing the shoulder pivot, so they must copy the arm's rotation
        // to stay attached to it (legacy setRotationAngles:218-226).
        this.leftHand.xRot = this.leftSwordA.xRot = this.leftSwordB.xRot = this.leftSwordC.xRot = this.leftArm.xRot;
        this.leftHand.zRot = this.leftSwordA.zRot = this.leftSwordB.zRot = this.leftSwordC.zRot = this.leftArm.zRot;
        this.rightHand.xRot = this.rightSwordA.xRot = this.rightSwordB.xRot = this.rightSwordC.xRot = this.rightArm.xRot;
        this.rightHand.zRot = this.rightSwordA.zRot = this.rightSwordB.zRot = this.rightSwordC.zRot = this.rightArm.zRot;

        if (state.riding) {
            // Seated cavalry pose: thighs and knee plates folded up 60 degrees and splayed 20 degrees outward,
            // shins hanging straight down.
            float thighPitch = -60.0F * DEG_TO_RAD;
            float splay = 20.0F * DEG_TO_RAD;
            this.rightLeg.xRot = 0.0F;
            this.rightThigh.xRot = thighPitch;
            this.rightThigh.yRot = splay;
            this.rightKnee.xRot = thighPitch;
            this.rightKnee.yRot = splay;
            this.leftLeg.xRot = 0.0F;
            this.leftThigh.xRot = thighPitch;
            this.leftThigh.yRot = -splay;
            this.leftKnee.xRot = thighPitch;
            this.leftKnee.yRot = -splay;
        } else {
            this.rightThigh.yRot = 0.0F;
            this.rightKnee.yRot = 0.0F;
            this.leftThigh.yRot = 0.0F;
            this.leftKnee.yRot = 0.0F;
            this.rightThigh.xRot = rLegXRot;
            this.leftThigh.xRot = lLegXRot;
            this.rightKnee.xRot = this.rightThigh.xRot;   // the knee plate rides on its thigh
            this.leftKnee.xRot = this.leftThigh.xRot;

            // Legacy stride kick: sample the same wave a tenth of a step LATER; if the leg is already past its
            // peak (i.e. travelling backwards) bend the shin an extra 25 degrees, so the trailing foot kicks up
            // behind the skeleton instead of the legs staying rigid. Only above a walking pace (limbAmount > 0.15).
            float rLegXRot2 = Mth.cos(((limbSwing + 0.1F) * 0.6662F) + PI) * 0.8F * limbAmount;
            float lLegXRot2 = Mth.cos((limbSwing + 0.1F) * 0.6662F) * 0.8F * limbAmount;
            if (limbAmount > 0.15F) {
                if (rLegXRot > rLegXRot2) {
                    rLegXRotB = rLegXRot + (25.0F * DEG_TO_RAD);
                }
                if (lLegXRot > lLegXRot2) {
                    lLegXRotB = lLegXRot + (25.0F * DEG_TO_RAD);
                }
            }
            // Legacy assigns each shin the OPPOSITE leg's value (right shin <- left phase). Kept verbatim:
            // against the thigh's own phase it reads as a knee bending through the stride.
            this.rightLeg.xRot = lLegXRotB;
            this.leftLeg.xRot = rLegXRotB;
        }

        // Sprint lean: legacy rotated the whole model up to 20 degrees forward once it was charging fast enough
        // (glRotatef(limbAmount * -20, -1, 0, 0) == a positive pitch about +X, i.e. head-down/forward).
        if (state.sprinting && limbAmount > 0.3F) {
            this.root.xRot += limbAmount * 20.0F * DEG_TO_RAD;
        }
    }
}
