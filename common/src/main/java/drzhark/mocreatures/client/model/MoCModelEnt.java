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
 * Ent model, converted faithfully from the legacy {@code MoCModelEnt} ({@code ModelBase},
 * {@code MoCModelEnt.java}:8-300). Every cube, texture offset and baked rotation is preserved, as is the
 * legacy {@code setRotationAngles} animation (:254-298).
 *
 * <p>The Ent is a walking tree roughly 8 blocks tall: a trunk-like {@code Body} on two multi-segment legs,
 * two long arms that end in oversized {@code Fingers}, a face set into the trunk under a {@code TreeBase}
 * stump, and a crown of <em>sixteen</em> 16x16x16 leaf cubes stacked in two tiers. All sixteen share one
 * texture offset ({@code 0, 224}) and one pivot ({@code 0, -44, 0}) in the legacy model, so they are built
 * here from a table rather than sixteen near-identical statements.</p>
 *
 * <p>Legacy pivots are negative on Y (up to {@code -105}), i.e. far <em>above</em> the model origin — the
 * 24-units-per-block convention is unchanged in 26.2, so the legacy numbers are used verbatim: the feet
 * land on the ground and the leaf crown tops out around 8 blocks up (slightly above the 7-block hitbox,
 * exactly as in 1.12.2 where the Ent's {@code setSize(1.4F, 7F)} also left the crown proud of the box).</p>
 *
 * <p>Animation notes, from the legacy {@code setRotationAngles}:</p>
 * <ul>
 *   <li>Arms and legs swing on the walk cycle; the arm halves ({@code Arm}/{@code Wrist}/{@code Hand}/
 *       {@code Fingers}) and leg segments ({@code Leg}/{@code Thigh}/{@code Knee}/{@code Ankle}) are
 *       separate root-level parts in the legacy mesh, so each one is driven to the SAME angle to keep the
 *       limb rigid — there is no parent-child chain to inherit from.</li>
 *   <li>The wrists also sway on a slow {@code cos(ageInTicks * 0.09)} idle roll, so the hands drift even
 *       when the Ent stands still, and the arms carry a baked +/-10 degree outward tilt on top of it.</li>
 *   <li>The head yaw is applied to the neck and then copied to the face, nose, mouth, head, tree-base stump
 *       and every leaf cube, so the entire crown turns with the head. The legacy explicitly did NOT apply
 *       head pitch ({@code Neck.rotateAngleX} is commented out at :284) — a tree does not nod — so this
 *       port leaves pitch out as well.</li>
 * </ul>
 */
public class MoCModelEnt extends EntityModel<MoCEntityRenderState> {

    /** Legacy {@code radianF = 57.29578F}: degrees -> radians, used verbatim below. */
    private static final float RADIAN_F = 57.29578F;

    private final ModelPart lArm;
    private final ModelPart lWrist;
    private final ModelPart lHand;
    private final ModelPart lFingers;
    private final ModelPart rArm;
    private final ModelPart rWrist;
    private final ModelPart rHand;
    private final ModelPart rFingers;
    private final ModelPart lLeg;
    private final ModelPart lThigh;
    private final ModelPart lKnee;
    private final ModelPart lAnkle;
    private final ModelPart lFoot;
    private final ModelPart rLeg;
    private final ModelPart rThigh;
    private final ModelPart rKnee;
    private final ModelPart rAnkle;
    private final ModelPart rFoot;
    private final ModelPart neck;
    private final ModelPart face;
    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart mouth;
    private final ModelPart treeBase;
    /** The sixteen crown cubes; all follow the neck's yaw so the canopy turns with the head. */
    private final ModelPart[] leaves = new ModelPart[16];

    /**
     * Legacy leaf-cube origins ({@code MoCModelEnt}:148-195). Each entry is the {@code addBox} corner of one
     * 16x16x16 cube; all sixteen share texture offset {@code (0, 224)} and pivot {@code (0, -44, 0)}.
     * Entries 0-11 form the lower tier ({@code y = -45}) and 12-15 the upper tier ({@code y = -61}).
     */
    private static final float[][] LEAF_ORIGINS = {
            {-16.0F, -45.0F, -17.0F}, {0.0F, -45.0F, -17.0F}, {0.0F, -45.0F, -1.0F}, {-16.0F, -45.0F, -1.0F},
            {-16.0F, -45.0F, -33.0F}, {0.0F, -45.0F, -33.0F}, {16.0F, -45.0F, -17.0F}, {16.0F, -45.0F, -1.0F},
            {0.0F, -45.0F, 15.0F}, {-16.0F, -45.0F, 15.0F}, {-32.0F, -45.0F, -1.0F}, {-32.0F, -45.0F, -17.0F},
            {-16.0F, -61.0F, -17.0F}, {0.0F, -61.0F, -17.0F}, {0.0F, -61.0F, -1.0F}, {-16.0F, -61.0F, -1.0F}};

    public MoCModelEnt(ModelPart root) {
        super(root);
        this.lArm = root.getChild("LArm");
        this.lWrist = root.getChild("LWrist");
        this.lHand = root.getChild("LHand");
        this.lFingers = root.getChild("LFingers");
        this.rArm = root.getChild("RArm");
        this.rWrist = root.getChild("RWrist");
        this.rHand = root.getChild("RHand");
        this.rFingers = root.getChild("RFingers");
        this.lLeg = root.getChild("LLeg");
        this.lThigh = root.getChild("LThigh");
        this.lKnee = root.getChild("LKnee");
        this.lAnkle = root.getChild("LAnkle");
        this.lFoot = root.getChild("LFoot");
        this.rLeg = root.getChild("RLeg");
        this.rThigh = root.getChild("RThigh");
        this.rKnee = root.getChild("RKnee");
        this.rAnkle = root.getChild("RAnkle");
        this.rFoot = root.getChild("RFoot");
        this.neck = root.getChild("Neck");
        this.face = root.getChild("Face");
        this.head = root.getChild("Head");
        this.nose = root.getChild("Nose");
        this.mouth = root.getChild("Mouth");
        this.treeBase = root.getChild("TreeBase");
        for (int i = 0; i < this.leaves.length; i++) {
            this.leaves[i] = root.getChild("Leave" + (i + 1));
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---- trunk / torso ----
        root.addOrReplaceChild("Body",
                CubeListBuilder.create().texOffs(68, 36).addBox(-7.5F, -12.5F, -4.5F, 15.0F, 25.0F, 9.0F),
                PartPose.offset(0.0F, -31.0F, 0.0F));

        // ---- left arm chain (shoulder is static; the rest swings) ----
        root.addOrReplaceChild("LShoulder",
                CubeListBuilder.create().texOffs(48, 108).addBox(6.0F, -14.0F, -4.8F, 9.0F, 7.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -31.0F, 0.0F, 0.0F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("LArm",
                CubeListBuilder.create().texOffs(80, 108).addBox(0.0F, -4.0F, -5.0F, 6.0F, 24.0F, 6.0F),
                PartPose.offsetAndRotation(10.0F, -42.0F, 1.0F, 0.0F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("LWrist",
                CubeListBuilder.create().texOffs(0, 169).addBox(2.0F, 17.0F, -6.0F, 8.0F, 15.0F, 8.0F),
                PartPose.offset(10.0F, -42.0F, 1.0F));
        root.addOrReplaceChild("LHand",
                CubeListBuilder.create().texOffs(88, 241).addBox(1.0F, 28.0F, -7.0F, 10.0F, 5.0F, 10.0F),
                PartPose.offset(10.0F, -42.0F, 1.0F));
        root.addOrReplaceChild("LFingers",
                CubeListBuilder.create().texOffs(88, 176).addBox(1.0F, 33.0F, -7.0F, 10.0F, 15.0F, 10.0F),
                PartPose.offset(10.0F, -42.0F, 1.0F));

        // ---- right arm chain ----
        root.addOrReplaceChild("RShoulder",
                CubeListBuilder.create().texOffs(48, 122).addBox(-15.0F, -14.0F, -4.8F, 9.0F, 7.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -31.0F, 0.0F, 0.0F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("RArm",
                CubeListBuilder.create().texOffs(104, 108).addBox(-6.0F, -4.0F, -5.0F, 6.0F, 24.0F, 6.0F),
                PartPose.offsetAndRotation(-10.0F, -42.0F, 1.0F, 0.0F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("RWrist",
                CubeListBuilder.create().texOffs(32, 169).addBox(-10.0F, 17.0F, -6.0F, 8.0F, 15.0F, 8.0F),
                PartPose.offset(-10.0F, -42.0F, 1.0F));
        root.addOrReplaceChild("RHand",
                CubeListBuilder.create().texOffs(88, 226).addBox(-11.0F, 28.0F, -7.0F, 10.0F, 5.0F, 10.0F),
                PartPose.offset(-10.0F, -42.0F, 1.0F));
        root.addOrReplaceChild("RFingers",
                CubeListBuilder.create().texOffs(88, 201).addBox(-11.0F, 33.0F, -7.0F, 10.0F, 15.0F, 10.0F),
                PartPose.offset(-10.0F, -42.0F, 1.0F));

        // ---- left leg chain ----
        root.addOrReplaceChild("LLeg",
                CubeListBuilder.create().texOffs(0, 90).addBox(3.0F, 0.0F, -3.0F, 6.0F, 20.0F, 6.0F),
                PartPose.offset(0.0F, -21.0F, 0.0F));
        root.addOrReplaceChild("LThigh",
                CubeListBuilder.create().texOffs(24, 64).addBox(2.5F, 4.0F, -3.5F, 7.0F, 12.0F, 7.0F),
                PartPose.offset(0.0F, -21.0F, 0.0F));
        root.addOrReplaceChild("LKnee",
                CubeListBuilder.create().texOffs(0, 0).addBox(2.0F, 20.0F, -4.0F, 8.0F, 24.0F, 8.0F),
                PartPose.offset(0.0F, -21.0F, 0.0F));
        root.addOrReplaceChild("LAnkle",
                CubeListBuilder.create().texOffs(32, 29).addBox(1.5F, 25.0F, -4.5F, 9.0F, 20.0F, 9.0F),
                PartPose.offset(0.0F, -21.0F, 0.0F));
        root.addOrReplaceChild("LFoot",
                CubeListBuilder.create().texOffs(0, 206).addBox(1.5F, 38.0F, -23.5F, 9.0F, 5.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, -21.0F, 0.0F, 0.2617994F, 0.0F, 0.0F));

        // ---- right leg chain ----
        root.addOrReplaceChild("RLeg",
                CubeListBuilder.create().texOffs(0, 64).addBox(-9.0F, 0.0F, -3.0F, 6.0F, 20.0F, 6.0F),
                PartPose.offset(0.0F, -21.0F, 0.0F));
        root.addOrReplaceChild("RThigh",
                CubeListBuilder.create().texOffs(24, 83).addBox(-9.5F, 4.0F, -3.5F, 7.0F, 12.0F, 7.0F),
                PartPose.offset(0.0F, -21.0F, 0.0F));
        root.addOrReplaceChild("RKnee",
                CubeListBuilder.create().texOffs(0, 32).addBox(-10.0F, 20.0F, -4.0F, 8.0F, 24.0F, 8.0F),
                PartPose.offset(0.0F, -21.0F, 0.0F));
        root.addOrReplaceChild("RAnkle",
                CubeListBuilder.create().texOffs(32, 0).addBox(-10.5F, 25.0F, -4.5F, 9.0F, 20.0F, 9.0F),
                PartPose.offset(0.0F, -21.0F, 0.0F));
        root.addOrReplaceChild("RFoot",
                CubeListBuilder.create().texOffs(0, 192).addBox(-10.5F, 38.0F, -23.5F, 9.0F, 5.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, -21.0F, 0.0F, 0.2617994F, 0.0F, 0.0F));

        // ---- head group (all share the -44 pivot and follow the neck yaw) ----
        root.addOrReplaceChild("Neck",
                CubeListBuilder.create().texOffs(52, 90).addBox(-4.0F, -8.0F, -5.8F, 8.0F, 10.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -44.0F, 0.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("Face",
                CubeListBuilder.create().texOffs(52, 70).addBox(-4.5F, -11.0F, -9.0F, 9.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, -44.0F, 0.0F));
        root.addOrReplaceChild("Head",
                CubeListBuilder.create().texOffs(84, 88).addBox(-6.0F, -20.5F, -9.5F, 12.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, -44.0F, 0.0F));
        root.addOrReplaceChild("Nose",
                CubeListBuilder.create().texOffs(82, 88).addBox(-1.5F, -12.0F, -12.0F, 3.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -44.0F, 0.0F, -0.122173F, 0.0F, 0.0F));
        root.addOrReplaceChild("Mouth",
                CubeListBuilder.create().texOffs(77, 36).addBox(-3.0F, -8.0F, -6.8F, 6.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -44.0F, 0.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("TreeBase",
                CubeListBuilder.create().texOffs(0, 136).addBox(-10.0F, -31.5F, -11.5F, 20.0F, 13.0F, 20.0F),
                PartPose.offset(0.0F, -44.0F, 0.0F));

        // ---- leaf crown: sixteen identical cubes, one shared texture offset and pivot ----
        for (int i = 0; i < LEAF_ORIGINS.length; i++) {
            float[] o = LEAF_ORIGINS[i];
            root.addOrReplaceChild("Leave" + (i + 1),
                    CubeListBuilder.create().texOffs(0, 224).addBox(o[0], o[1], o[2], 16.0F, 16.0F, 16.0F),
                    PartPose.offset(0.0F, -44.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 128, 256);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        final float limbSwing = state.walkAnimationPos;
        final float limbAmount = state.walkAnimationSpeed;

        // Legacy :258-261 — arms swing at half amplitude and out of phase with each other; legs at full.
        float rArmXRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbAmount * 0.5F;
        float lArmXRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbAmount * 0.5F;
        float rLegXRot = Mth.cos(limbSwing * 0.6662F) * 1.0F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.0F * limbAmount;

        // Legacy :263-266 — a slow idle roll on the wrists so the hands drift even when standing still.
        this.lWrist.zRot = (Mth.cos(state.ageInTicks * 0.09F) * 0.05F) - 0.05F;
        this.lWrist.xRot = lArmXRot;
        this.rWrist.zRot = -(Mth.cos(state.ageInTicks * 0.09F) * 0.05F) + 0.05F;
        this.rWrist.xRot = rArmXRot;

        // Legacy :268-274 — the arm segments are separate root-level parts, so each is driven to the same
        // angle to keep the limb rigid; the upper arms add a baked +/-10 degree outward tilt.
        this.lHand.xRot = this.lFingers.xRot = this.lArm.xRot = this.lWrist.xRot;
        this.lHand.zRot = this.lFingers.zRot = this.lWrist.zRot;
        this.lArm.zRot = (-10.0F / RADIAN_F) + this.lWrist.zRot;

        this.rHand.xRot = this.rFingers.xRot = this.rArm.xRot = this.rWrist.xRot;
        this.rHand.zRot = this.rFingers.zRot = this.rWrist.zRot;
        this.rArm.zRot = (10.0F / RADIAN_F) + this.rWrist.zRot;

        // Legacy :276-282 — leg segments follow the leg; the feet keep a baked 15 degree toe-up tilt.
        this.rLeg.xRot = rLegXRot;
        this.lLeg.xRot = lLegXRot;
        this.lThigh.xRot = this.lKnee.xRot = this.lAnkle.xRot = this.lLeg.xRot;
        this.rThigh.xRot = this.rKnee.xRot = this.rAnkle.xRot = this.rLeg.xRot;
        this.lFoot.xRot = (15.0F / RADIAN_F) + this.lLeg.xRot;
        this.rFoot.xRot = (15.0F / RADIAN_F) + this.rLeg.xRot;

        // Legacy :283-296 — head yaw only (pitch is deliberately not applied, see the class javadoc); the
        // face, stump and the entire leaf crown turn with the neck.
        float headYaw = state.yRot / RADIAN_F;
        this.neck.yRot = headYaw;
        this.mouth.yRot = this.face.yRot = this.nose.yRot = this.head.yRot = this.treeBase.yRot = headYaw;
        for (ModelPart leaf : this.leaves) {
            leaf.yRot = headYaw;
        }
    }
}
