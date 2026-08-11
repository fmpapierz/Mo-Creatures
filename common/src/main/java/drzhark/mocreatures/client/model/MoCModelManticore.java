package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Manticore model — the winged, scorpion-tailed big cat. Converted from the 12.0.5
 * {@code MoCModelManticore}, which was not a model of its own at all: it was a one-method subclass of
 * {@code MoCModelNewBigCat} that flipped five flags on ({@code isFlyer}, {@code hasStinger},
 * {@code hasMane}, {@code hasSaberTeeth}, plus the {@code poisoning} tail-strike state) so the shared
 * big-cat geometry rendered its otherwise-dormant wing and stinger parts.
 *
 * <p><b>Why this class exists instead of reusing {@link MoCModelBigCat} directly.</b> The port's
 * {@code MoCModelBigCat} carries the whole feline body — head/snout/fangs/mane, chest + abdomen, the
 * five-segment cat tail (including the small {@code tail_tusk} tip cube), four legs with claws, the
 * saddle group and the pulling harness — but its {@link MoCModelBigCat#createBodyLayer()} does
 * <em>not</em> build the manticore's SIX wing panels ({@code InnerWing / MidWing / OuterWing} left and
 * right, legacy {@code MoCModelNewBigCat:264-292}) nor the SEVEN-segment scorpion sting tail
 * ({@code STailRoot..STail5 + StingerLump + Stinger}, legacy {@code :385-426}). Those parts simply are
 * not in the port's mesh, so the manticore cannot render through it as-is, and
 * {@code createBodyLayer()} hands back a {@link LayerDefinition} whose {@code MeshDefinition} is
 * private — there is no way to append parts to another model's baked layer from outside it.
 *
 * <p>So this class <em>extends</em> {@code MoCModelBigCat} (inheriting its whole walk gait, head
 * tracking, jaw drop and saddle visibility logic verbatim, and its constructor's part lookups) and
 * supplies a SUPERSET mesh: the big-cat mesh reproduced part-for-part — every child name the parent
 * constructor resolves must exist — plus the manticore's wings and sting tail. The alternative would
 * have been to edit the shared {@code MoCModelBigCat}, which this port's split-file workflow forbids.
 *
 * <p>Like the legacy model, the wings and the sting tail hang off the model ROOT rather than off the
 * chest: legacy rendered them with their own {@code rotationPoint}s in model space
 * ({@code MoCModelNewBigCat.render:594-611}) instead of as children of {@code Chest}, and their
 * animation code drives those rotation points directly, so parenting them to the body would double the
 * chest transform onto them.
 */
public class MoCModelManticore extends MoCModelBigCat {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    // Wing panels: three per side, chained by hand in setupAnim exactly as legacy did (each one is a
    // root-level part whose position is recomputed from the inner panel's, not a real parent chain).
    private final ModelPart innerWing;
    private final ModelPart midWing;
    private final ModelPart outerWing;
    private final ModelPart innerWingR;
    private final ModelPart midWingR;
    private final ModelPart outerWingR;

    // Scorpion sting tail: five arching segments + the bulb and the barb.
    private final ModelPart stingTailRoot;
    private final ModelPart stingTail2;
    private final ModelPart stingTail3;
    private final ModelPart stingTail4;
    private final ModelPart stingTail5;
    private final ModelPart stingerLump;
    private final ModelPart stinger;

    // Upper legs, re-resolved here because the parent keeps its own references private: the manticore
    // tucks all four legs while airborne (legacy MoCModelNewBigCat.setRotationAngles:776-789).
    private final ModelPart leftUpperLeg;
    private final ModelPart rightUpperLeg;
    private final ModelPart leftHindUpperLeg;
    private final ModelPart rightHindUpperLeg;

    public MoCModelManticore(ModelPart root) {
        super(root);
        this.innerWing = root.getChild("inner_wing");
        this.midWing = root.getChild("mid_wing");
        this.outerWing = root.getChild("outer_wing");
        this.innerWingR = root.getChild("inner_wing_right");
        this.midWingR = root.getChild("mid_wing_right");
        this.outerWingR = root.getChild("outer_wing_right");

        this.stingTailRoot = root.getChild("sting_tail_root");
        this.stingTail2 = root.getChild("sting_tail2");
        this.stingTail3 = root.getChild("sting_tail3");
        this.stingTail4 = root.getChild("sting_tail4");
        this.stingTail5 = root.getChild("sting_tail5");
        this.stingerLump = root.getChild("stinger_lump");
        this.stinger = root.getChild("stinger");

        ModelPart chest = root.getChild("chest");
        ModelPart abdomen = chest.getChild("abdomen");
        this.leftUpperLeg = chest.getChild("left_upper_leg");
        this.rightUpperLeg = chest.getChild("right_upper_leg");
        this.leftHindUpperLeg = abdomen.getChild("left_hind_upper_leg");
        this.rightHindUpperLeg = abdomen.getChild("right_hind_upper_leg");
    }

    /**
     * The big-cat mesh (identical to {@link MoCModelBigCat#createBodyLayer()}, part names included, so the
     * inherited constructor and {@code setupAnim} keep working) PLUS the manticore's wings and sting tail.
     * Still a {@code 128 x 128} layer: the {@code bcmanticore*} sheets are 256&times;256 two-times upscales
     * of the same 128-space layout the legacy {@code MoCModelNewBigCat} declared, so the UVs map correctly.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ================================================================ shared big-cat body
        // ---- Chest (root body part) ----
        PartDefinition chest = root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 18).addBox(-3.5F, 0.0F, -8.0F, 7, 8, 9),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        // ---- Neck / head chain ----
        PartDefinition neckBase = chest.addOrReplaceChild("neck_base",
                CubeListBuilder.create().texOffs(0, 7).addBox(-2.5F, 0.0F, -2.5F, 5, 6, 5),
                PartPose.offsetAndRotation(0.0F, -0.5F, -8.0F, -14.0F * DEG_TO_RAD, 0.0F, 0.0F));

        neckBase.addOrReplaceChild("collar",
                CubeListBuilder.create().texOffs(18, 0).addBox(-2.5F, 0.0F, 0.0F, 5, 4, 1),
                PartPose.offsetAndRotation(0.0F, 6.0F, -2.0F, 20.0F * DEG_TO_RAD, 0.0F, 0.0F));

        PartDefinition headBack = neckBase.addOrReplaceChild("head_back",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.51F, -2.5F, -1.0F, 5, 5, 2),
                PartPose.offsetAndRotation(0.0F, 2.7F, -2.9F, 14.0F * DEG_TO_RAD, 0.0F, 0.0F));

        headBack.addOrReplaceChild("neck_harness",
                CubeListBuilder.create().texOffs(85, 32).addBox(-3.0F, -3.0F, -2.0F, 6, 6, 2),
                PartPose.offset(0.0F, 0.0F, 0.95F));
        headBack.addOrReplaceChild("harness_stick",
                CubeListBuilder.create().texOffs(85, 42).addBox(-3.5F, -0.5F, -0.5F, 7, 1, 1),
                PartPose.offsetAndRotation(0.0F, -1.8F, 0.5F, 45.0F * DEG_TO_RAD, 0.0F, 0.0F));

        PartDefinition head = headBack.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(32, 0).addBox(-3.5F, -3.0F, -2.0F, 7, 6, 4),
                PartPose.offset(0.0F, 0.2F, -2.2F));

        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(46, 19).addBox(-1.5F, -1.0F, -2.0F, 3, 2, 4),
                PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 27.0F * DEG_TO_RAD, 0.0F, 0.0F));
        head.addOrReplaceChild("right_upper_lip",
                CubeListBuilder.create().texOffs(34, 19).addBox(-1.0F, -1.0F, -2.0F, 2, 2, 4),
                PartPose.offsetAndRotation(-1.25F, 1.0F, -2.8F, 10.0F * DEG_TO_RAD, 2.0F * DEG_TO_RAD, -15.0F * DEG_TO_RAD));
        head.addOrReplaceChild("left_upper_lip",
                CubeListBuilder.create().texOffs(34, 25).addBox(-1.0F, -1.0F, -2.0F, 2, 2, 4),
                PartPose.offsetAndRotation(1.25F, 1.0F, -2.8F, 10.0F * DEG_TO_RAD, -2.0F * DEG_TO_RAD, 15.0F * DEG_TO_RAD));
        head.addOrReplaceChild("upper_teeth",
                CubeListBuilder.create().texOffs(20, 7).addBox(-1.5F, -1.0F, -1.5F, 3, 2, 3),
                PartPose.offsetAndRotation(0.0F, 2.0F, -2.5F, 15.0F * DEG_TO_RAD, 0.0F, 0.0F));
        head.addOrReplaceChild("left_fang",
                CubeListBuilder.create().texOffs(44, 10).addBox(-0.5F, -1.5F, -0.5F, 1, 3, 1),
                PartPose.offsetAndRotation(1.2F, 2.8F, -3.4F, 15.0F * DEG_TO_RAD, 0.0F, 0.0F));
        head.addOrReplaceChild("right_fang",
                CubeListBuilder.create().texOffs(48, 10).addBox(-0.5F, -1.5F, -0.5F, 1, 3, 1),
                PartPose.offsetAndRotation(-1.2F, 2.8F, -3.4F, 15.0F * DEG_TO_RAD, 0.0F, 0.0F));
        head.addOrReplaceChild("inside_mouth",
                CubeListBuilder.create().texOffs(50, 0).addBox(-1.5F, -1.0F, -1.0F, 3, 2, 2),
                PartPose.offset(0.0F, 2.0F, -1.0F));

        PartDefinition lowerJaw = head.addOrReplaceChild("lower_jaw",
                CubeListBuilder.create().texOffs(46, 25).addBox(-1.5F, -1.0F, -4.0F, 3, 2, 4),
                PartPose.offset(0.0F, 2.1F, 0.0F));
        lowerJaw.addOrReplaceChild("lower_jaw_teeth",
                CubeListBuilder.create().texOffs(20, 12).mirror().addBox(-1.0F, 0.0F, -1.0F, 2, 1, 2),
                PartPose.offset(0.0F, -1.8F, -2.7F));
        lowerJaw.addOrReplaceChild("chin_hair",
                CubeListBuilder.create().texOffs(76, 7).addBox(-2.5F, 0.0F, -2.0F, 5, 6, 4),
                PartPose.offset(0.0F, 0.0F, 1.0F));

        head.addOrReplaceChild("left_chin_beard",
                CubeListBuilder.create().texOffs(48, 10).addBox(-1.0F, -2.5F, -2.0F, 2, 5, 4),
                PartPose.offsetAndRotation(3.6F, 0.0F, 0.25F, 0.0F, 30.0F * DEG_TO_RAD, 0.0F));
        head.addOrReplaceChild("right_chin_beard",
                CubeListBuilder.create().texOffs(36, 10).addBox(-1.0F, -2.5F, -2.0F, 2, 5, 4),
                PartPose.offsetAndRotation(-3.6F, 0.0F, 0.25F, 0.0F, -30.0F * DEG_TO_RAD, 0.0F));
        head.addOrReplaceChild("forehead_hair",
                CubeListBuilder.create().texOffs(88, 0).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3),
                PartPose.offsetAndRotation(0.0F, -3.2F, 0.0F, 10.0F * DEG_TO_RAD, 0.0F, 0.0F));
        head.addOrReplaceChild("mane",
                CubeListBuilder.create().texOffs(94, 0).addBox(-5.5F, -5.5F, -3.0F, 11, 11, 6),
                PartPose.offsetAndRotation(0.0F, 0.7F, 3.7F, -5.0F * DEG_TO_RAD, 0.0F, 0.0F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(54, 7).addBox(-1.0F, -1.0F, -0.5F, 2, 2, 1),
                PartPose.offsetAndRotation(-2.7F, -3.5F, 1.0F, 0.0F, 0.0F, -15.0F * DEG_TO_RAD));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(54, 4).addBox(-1.0F, -1.0F, -0.5F, 2, 2, 1),
                PartPose.offsetAndRotation(2.7F, -3.5F, 1.0F, 0.0F, 0.0F, 15.0F * DEG_TO_RAD));

        neckBase.addOrReplaceChild("neck_hair",
                CubeListBuilder.create().texOffs(108, 17).addBox(-2.0F, -1.0F, -3.0F, 4, 2, 6),
                PartPose.offsetAndRotation(0.0F, -0.5F, 3.0F, -10.6F * DEG_TO_RAD, 0.0F, 0.0F));

        // ---- Abdomen / rump ----
        PartDefinition abdomen = chest.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(0, 35).addBox(-3.0F, 0.0F, 0.0F, 6, 7, 7),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0523599F, 0.0F, 0.0F));

        abdomen.addOrReplaceChild("ass",
                CubeListBuilder.create().texOffs(0, 49).addBox(-2.5F, 0.0F, 0.0F, 5, 5, 3),
                PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, -20.0F * DEG_TO_RAD, 0.0F, 0.0F));

        abdomen.addOrReplaceChild("storage_chest",
                CubeListBuilder.create().texOffs(32, 59).addBox(-5.0F, -2.0F, -2.5F, 10, 4, 5),
                PartPose.offsetAndRotation(0.0F, -2.0F, 5.5F, -90.0F * DEG_TO_RAD, 0.0F, 0.0F));

        // ---- Cat tail chain (distinct from the scorpion sting tail added further down) ----
        PartDefinition tailRoot = abdomen.addOrReplaceChild("tail_root",
                CubeListBuilder.create().texOffs(96, 83).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
                PartPose.offsetAndRotation(0.0F, 1.0F, 7.0F, 87.0F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition tail2 = tailRoot.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(96, 75).addBox(-1.0F, 0.0F, -1.0F, 2, 6, 2),
                PartPose.offsetAndRotation(-0.01F, 3.5F, 0.0F, -30.0F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3",
                CubeListBuilder.create().texOffs(96, 67).addBox(-1.0F, 0.0F, -1.0F, 2, 6, 2),
                PartPose.offsetAndRotation(0.01F, 5.5F, 0.0F, -17.0F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail4",
                CubeListBuilder.create().texOffs(96, 61).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
                PartPose.offsetAndRotation(-0.01F, 5.5F, 0.0F, 21.0F * DEG_TO_RAD, 0.0F, 0.0F));
        tail4.addOrReplaceChild("tail_tip",
                CubeListBuilder.create().texOffs(96, 55).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
                PartPose.offsetAndRotation(0.01F, 3.5F, 0.0F, 21.0F * DEG_TO_RAD, 0.0F, 0.0F));
        tail4.addOrReplaceChild("tail_tusk",
                CubeListBuilder.create().texOffs(96, 49).addBox(-1.5F, 0.0F, -1.5F, 3, 3, 3),
                PartPose.offsetAndRotation(0.0F, 3.5F, 0.0F, 21.0F * DEG_TO_RAD, 0.0F, 0.0F));

        // ---- Saddle ----
        PartDefinition saddle = chest.addOrReplaceChild("saddle",
                CubeListBuilder.create().texOffs(79, 18).addBox(-4.0F, -1.0F, -3.0F, 8, 2, 6),
                PartPose.offset(0.0F, 0.5F, -1.0F));
        saddle.addOrReplaceChild("saddle_front",
                CubeListBuilder.create().texOffs(101, 26).addBox(-2.5F, -1.0F, -1.5F, 5, 2, 3),
                PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, -10.6F * DEG_TO_RAD, 0.0F, 0.0F));
        saddle.addOrReplaceChild("saddle_back",
                CubeListBuilder.create().texOffs(77, 26).addBox(-4.0F, -2.0F, -2.0F, 8, 2, 4),
                PartPose.offsetAndRotation(0.0F, 0.7F, 4.0F, 12.78F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition leftFootHarness = saddle.addOrReplaceChild("left_foot_harness",
                CubeListBuilder.create().texOffs(81, 18).addBox(-0.5F, 0.0F, -0.5F, 1, 5, 1),
                PartPose.offset(4.0F, 0.0F, 0.5F));
        leftFootHarness.addOrReplaceChild("left_foot_ring",
                CubeListBuilder.create().texOffs(107, 31).addBox(0.0F, 0.0F, 0.0F, 1, 2, 2),
                PartPose.offset(-0.5F, 5.0F, -1.0F));
        PartDefinition rightFootHarness = saddle.addOrReplaceChild("right_foot_harness",
                CubeListBuilder.create().texOffs(101, 18).addBox(-0.5F, 0.0F, -0.5F, 1, 5, 1),
                PartPose.offset(-4.0F, 0.0F, 0.5F));
        rightFootHarness.addOrReplaceChild("right_foot_ring",
                CubeListBuilder.create().texOffs(101, 31).addBox(0.0F, 0.0F, 0.0F, 1, 2, 2),
                PartPose.offset(-0.5F, 5.0F, -1.0F));

        // ---- Front legs ----
        PartDefinition leftUpperLeg = chest.addOrReplaceChild("left_upper_leg",
                CubeListBuilder.create().texOffs(0, 96).addBox(-1.5F, 0.0F, -2.0F, 3, 7, 4),
                PartPose.offsetAndRotation(3.99F, 3.0F, -7.0F, 15.0F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition leftLowerLeg = leftUpperLeg.addOrReplaceChild("left_lower_leg",
                CubeListBuilder.create().texOffs(0, 107).addBox(-1.5F, 0.0F, -1.5F, 3, 6, 3),
                PartPose.offsetAndRotation(-0.01F, 6.5F, 0.2F, -21.5F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition leftFrontFoot = leftLowerLeg.addOrReplaceChild("left_front_foot",
                CubeListBuilder.create().texOffs(0, 116).addBox(-2.0F, 0.0F, -2.0F, 4, 2, 4),
                PartPose.offsetAndRotation(0.0F, 5.0F, -1.0F, 6.5F * DEG_TO_RAD, 0.0F, 0.0F));
        leftFrontFoot.addOrReplaceChild("left_claw1",
                CubeListBuilder.create().texOffs(16, 125).addBox(-0.5F, 0.0F, -0.5F, 1, 1, 2),
                PartPose.offsetAndRotation(-1.3F, 1.2F, -3.0F, 45.0F * DEG_TO_RAD, 0.0F, -1.0F * DEG_TO_RAD));
        leftFrontFoot.addOrReplaceChild("left_claw2",
                CubeListBuilder.create().texOffs(16, 125).addBox(-0.5F, 0.0F, -0.5F, 1, 1, 2),
                PartPose.offsetAndRotation(0.0F, 1.1F, -3.0F, 45.0F * DEG_TO_RAD, 0.0F, 0.0F));
        leftFrontFoot.addOrReplaceChild("left_claw3",
                CubeListBuilder.create().texOffs(16, 125).addBox(-0.5F, 0.0F, -0.5F, 1, 1, 2),
                PartPose.offsetAndRotation(1.3F, 1.2F, -3.0F, 45.0F * DEG_TO_RAD, 0.0F, 1.0F * DEG_TO_RAD));

        PartDefinition rightUpperLeg = chest.addOrReplaceChild("right_upper_leg",
                CubeListBuilder.create().texOffs(14, 96).addBox(-1.5F, 0.0F, -2.0F, 3, 7, 4),
                PartPose.offsetAndRotation(-3.99F, 3.0F, -7.0F, 15.0F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition rightLowerLeg = rightUpperLeg.addOrReplaceChild("right_lower_leg",
                CubeListBuilder.create().texOffs(12, 107).addBox(-1.5F, 0.0F, -1.5F, 3, 6, 3),
                PartPose.offsetAndRotation(0.01F, 6.5F, 0.2F, -21.5F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition rightFrontFoot = rightLowerLeg.addOrReplaceChild("right_front_foot",
                CubeListBuilder.create().texOffs(0, 122).addBox(-2.0F, 0.0F, -2.0F, 4, 2, 4),
                PartPose.offsetAndRotation(0.0F, 5.0F, -1.0F, 6.5F * DEG_TO_RAD, 0.0F, 0.0F));
        rightFrontFoot.addOrReplaceChild("right_claw1",
                CubeListBuilder.create().texOffs(16, 125).addBox(-0.5F, 0.0F, -0.5F, 1, 1, 2),
                PartPose.offsetAndRotation(-1.3F, 1.2F, -3.0F, 45.0F * DEG_TO_RAD, 0.0F, -1.0F * DEG_TO_RAD));
        rightFrontFoot.addOrReplaceChild("right_claw2",
                CubeListBuilder.create().texOffs(16, 125).addBox(-0.5F, 0.0F, -0.5F, 1, 1, 2),
                PartPose.offsetAndRotation(0.0F, 1.1F, -3.0F, 45.0F * DEG_TO_RAD, 0.0F, 0.0F));
        rightFrontFoot.addOrReplaceChild("right_claw3",
                CubeListBuilder.create().texOffs(16, 125).addBox(-0.5F, 0.0F, -0.5F, 1, 1, 2),
                PartPose.offsetAndRotation(1.3F, 1.2F, -3.0F, 45.0F * DEG_TO_RAD, 0.0F, 1.0F * DEG_TO_RAD));

        // ---- Hind legs ----
        PartDefinition leftHindUpperLeg = abdomen.addOrReplaceChild("left_hind_upper_leg",
                CubeListBuilder.create().texOffs(0, 67).addBox(-2.0F, -1.0F, -1.5F, 3, 8, 5),
                PartPose.offsetAndRotation(3.0F, 3.0F, 6.8F, -25.0F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition leftAnkle = leftHindUpperLeg.addOrReplaceChild("left_ankle",
                CubeListBuilder.create().texOffs(0, 80).addBox(-1.0F, 0.0F, -1.5F, 2, 3, 3),
                PartPose.offset(-0.5F, 4.0F, 5.0F));
        PartDefinition leftHindLowerLeg = leftAnkle.addOrReplaceChild("left_hind_lower_leg",
                CubeListBuilder.create().texOffs(0, 86).addBox(-1.0F, 0.0F, -1.0F, 2, 3, 2),
                PartPose.offset(0.0F, 3.0F, 0.5F));
        leftHindLowerLeg.addOrReplaceChild("left_hind_foot",
                CubeListBuilder.create().texOffs(0, 91).addBox(-1.5F, 0.0F, -1.5F, 3, 2, 3),
                PartPose.offsetAndRotation(0.0F, 2.6F, -0.8F, 27.0F * DEG_TO_RAD, 0.0F, 0.0F));

        PartDefinition rightHindUpperLeg = abdomen.addOrReplaceChild("right_hind_upper_leg",
                CubeListBuilder.create().texOffs(16, 67).addBox(-2.0F, -1.0F, -1.5F, 3, 8, 5),
                PartPose.offsetAndRotation(-2.0F, 3.0F, 6.8F, -25.0F * DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition rightAnkle = rightHindUpperLeg.addOrReplaceChild("right_ankle",
                CubeListBuilder.create().texOffs(10, 80).addBox(-1.0F, 0.0F, -1.5F, 2, 3, 3),
                PartPose.offset(-0.5F, 4.0F, 5.0F));
        PartDefinition rightHindLowerLeg = rightAnkle.addOrReplaceChild("right_hind_lower_leg",
                CubeListBuilder.create().texOffs(8, 86).addBox(-1.0F, 0.0F, -1.0F, 2, 3, 2),
                PartPose.offset(0.0F, 3.0F, 0.5F));
        rightHindLowerLeg.addOrReplaceChild("right_hind_foot",
                CubeListBuilder.create().texOffs(12, 91).addBox(-1.5F, 0.0F, -1.5F, 3, 2, 3),
                PartPose.offsetAndRotation(0.0F, 2.6F, -0.8F, 27.0F * DEG_TO_RAD, 0.0F, 0.0F));

        // ================================================================ manticore-only geometry
        // ---- Wings (legacy MoCModelNewBigCat:264-292) ----
        // Root-level, exactly as legacy: three panels per side whose rotation points are recomputed each
        // frame from the inner panel's, so the wing folds/unfolds as a hinged chain without real parenting.
        root.addOrReplaceChild("inner_wing",
                CubeListBuilder.create().texOffs(26, 115).addBox(0.0F, 0.0F, 0.0F, 7, 2, 11),
                PartPose.offsetAndRotation(4.0F, 9.0F, -7.0F, 0.0F, -20.0F * DEG_TO_RAD, 0.0F));
        root.addOrReplaceChild("mid_wing",
                CubeListBuilder.create().texOffs(36, 89).addBox(1.0F, 0.1F, 1.0F, 12, 2, 11),
                PartPose.offsetAndRotation(4.0F, 9.0F, -7.0F, 0.0F, 5.0F * DEG_TO_RAD, 0.0F));
        root.addOrReplaceChild("outer_wing",
                CubeListBuilder.create().texOffs(62, 115).addBox(0.0F, 0.0F, 0.0F, 22, 2, 11),
                PartPose.offsetAndRotation(16.0F, 9.0F, -7.0F, 0.0F, -18.0F * DEG_TO_RAD, 0.0F));
        root.addOrReplaceChild("inner_wing_right",
                CubeListBuilder.create().texOffs(26, 102).addBox(-7.0F, 0.0F, 0.0F, 7, 2, 11),
                PartPose.offsetAndRotation(-4.0F, 9.0F, -7.0F, 0.0F, 20.0F * DEG_TO_RAD, 0.0F));
        root.addOrReplaceChild("mid_wing_right",
                CubeListBuilder.create().texOffs(82, 89).addBox(-13.0F, 0.1F, 1.0F, 12, 2, 11),
                PartPose.offsetAndRotation(-4.0F, 9.0F, -7.0F, 0.0F, -5.0F * DEG_TO_RAD, 0.0F));
        root.addOrReplaceChild("outer_wing_right",
                CubeListBuilder.create().texOffs(62, 102).addBox(-22.0F, 0.0F, 0.0F, 22, 2, 11),
                PartPose.offsetAndRotation(-16.0F, 9.0F, -7.0F, 0.0F, 18.0F * DEG_TO_RAD, 0.0F));

        // ---- Scorpion sting tail (legacy MoCModelNewBigCat:385-426) ----
        // Every segment shares the rotation point (0, 8, 0) and carries its arc entirely in its box offset
        // plus a baked pitch; the two poses (resting arch vs. forward strike) are swapped in setupAnim.
        // All seven were built mirrored in legacy, so keep .mirror().
        root.addOrReplaceChild("sting_tail_root",
                CubeListBuilder.create().texOffs(104, 79).mirror().addBox(-3.0F, 4.0F, 5.0F, 6, 4, 6),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.5796765F, 0.0F, 0.0F));
        root.addOrReplaceChild("sting_tail2",
                CubeListBuilder.create().texOffs(106, 69).mirror().addBox(-2.5F, 7.5F, 7.3F, 5, 4, 6),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.9514626F, 0.0F, 0.0F));
        root.addOrReplaceChild("sting_tail3",
                CubeListBuilder.create().texOffs(108, 60).mirror().addBox(-2.0F, 13.5F, 3.3F, 4, 3, 6),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 1.660128F, 0.0F, 0.0F));
        root.addOrReplaceChild("sting_tail4",
                CubeListBuilder.create().texOffs(108, 51).mirror().addBox(-2.0F, 15.2F, -5.3F, 4, 3, 6),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 2.478058F, 0.0F, 0.0F));
        root.addOrReplaceChild("sting_tail5",
                CubeListBuilder.create().texOffs(108, 42).mirror().addBox(-2.0F, 12.9F, -9.0F, 4, 3, 6),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 3.035737F, 0.0F, 0.0F));
        root.addOrReplaceChild("stinger_lump",
                CubeListBuilder.create().texOffs(112, 34).mirror().addBox(-1.5F, 7.9F, 6.0F, 3, 3, 5),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 2.031914F, 0.0F, 0.0F));
        root.addOrReplaceChild("stinger",
                CubeListBuilder.create().texOffs(118, 29).mirror().addBox(-0.5F, 1.9F, 8.0F, 1, 1, 4),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 1.213985F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        // The inherited big-cat pass resets every part to its baked pose, then lays down the walk gait,
        // head tracking, jaw drop and saddle visibility. Everything below is layered on top of that.
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        // ------------------------------------------------------------------ airborne leg tuck
        // Legacy setRotationAngles:776-789: a FLYER in the air with any movement swings all four legs
        // back and under (front 45deg, hind 10deg, both opening further with speed) instead of walking.
        if (state.manticoreAirborne && limbAmount > 0.0F) {
            float speedMov = limbAmount * 0.5F;
            this.rightUpperLeg.xRot = (45.0F * DEG_TO_RAD) + speedMov;
            this.leftUpperLeg.xRot = (45.0F * DEG_TO_RAD) + speedMov;
            this.rightHindUpperLeg.xRot = (10.0F * DEG_TO_RAD) + speedMov;
            this.leftHindUpperLeg.xRot = (10.0F * DEG_TO_RAD) + speedMov;
        }

        // ------------------------------------------------------------------ wings
        // Legacy setRotationAngles:902-966. A beating wing swings on the animation timer at full 1.2 rad
        // amplitude; a cruising one just breathes with the gait. On the ground the wings snap folded back
        // (outer panels rotated 90deg inward at a fixed 60deg lift).
        float wingRot;
        if (state.manticoreFlapping) {
            wingRot = Mth.cos((state.ageInTicks * 0.3F) + 3.141593F) * 1.2F;
        } else {
            wingRot = Mth.cos(limbSwing * 0.5F) * 0.1F;
        }
        if (state.manticoreAirborne) {
            this.outerWing.yRot = -0.3228859F + (wingRot / 2.0F);
            this.outerWingR.yRot = 0.3228859F - (wingRot / 2.0F);
        } else {
            wingRot = 60.0F * DEG_TO_RAD;
            this.outerWing.yRot = -90.0F * DEG_TO_RAD;
            this.outerWingR.yRot = 90.0F * DEG_TO_RAD;
        }

        // Hand-built hinge: the outer panel orbits the inner one at a radius of 12 (the gap between their
        // rotation points), so cos/sin of the lift angle place it on that arc. Legacy did exactly this
        // rather than nesting the parts, because each panel needed its own independent Z lift.
        float innerY = this.innerWing.y;
        float innerZ = this.innerWing.z;
        this.innerWingR.y = innerY;
        this.innerWingR.z = innerZ;
        this.outerWing.x = this.innerWing.x + (Mth.cos(wingRot) * 12.0F);
        this.outerWingR.x = this.innerWingR.x - (Mth.cos(wingRot) * 12.0F);
        this.midWing.y = innerY;
        this.midWingR.y = innerY;
        this.outerWing.y = innerY + (Mth.sin(wingRot) * 12.0F);
        this.outerWingR.y = innerY + (Mth.sin(wingRot) * 12.0F);
        this.midWing.z = innerZ;
        this.midWingR.z = innerZ;
        this.outerWing.z = innerZ;
        this.outerWingR.z = innerZ;
        this.innerWing.zRot = wingRot;
        this.midWing.zRot = wingRot;
        this.outerWing.zRot = wingRot;
        this.innerWingR.zRot = -wingRot;
        this.midWingR.zRot = -wingRot;
        this.outerWingR.zRot = -wingRot;

        // ------------------------------------------------------------------ scorpion sting tail
        // Legacy setRotationAngles:968-1030, verbatim: a resting arch curled over the back, and — while the
        // manticore is mid-sting — a forward strike that throws the barb out past the head. Legacy also
        // raised the whole tail (stingYOffset 8 -> 17, stingZOffset 0 -> -3) while SITTING; the port's
        // big-cat model has no sitting pose at all, so only the standing offsets are used here.
        if (!state.manticoreStinging) {
            poseSting(this.stingTailRoot, 33.0F, 8.0F, 0.0F);
            poseSting(this.stingTail2, 54.5F, 8.0F, 0.0F);
            poseSting(this.stingTail3, 95.1F, 8.0F, 0.0F);
            poseSting(this.stingTail4, 141.8F, 8.0F, 0.0F);
            poseSting(this.stingTail5, 173.9F, 8.0F, 0.0F);
            poseSting(this.stingerLump, 116.4F, 8.0F, 0.0F);
            poseSting(this.stinger, 69.5F, 8.0F, 0.0F);
        } else {
            poseSting(this.stingTailRoot, 95.2F, 14.5F, 2.0F);
            poseSting(this.stingTail2, 128.5F, 15.0F, 4.0F);
            poseSting(this.stingTail3, 169.0F, 14.0F, 3.8F);
            poseSting(this.stingTail4, 177.0F, 13.5F, -8.5F);
            poseSting(this.stingTail5, 180.0F, 11.5F, -17.0F);
            poseSting(this.stingerLump, 35.4F, -4.0F, -28.0F);
            poseSting(this.stinger, 25.5F, 4.0F, -29.0F);
        }
    }

    /** Places one sting-tail segment: pitch in degrees plus the shared per-pose Y/Z rotation point. */
    private void poseSting(ModelPart part, float pitchDegrees, float y, float z) {
        part.xRot = pitchDegrees * DEG_TO_RAD;
        part.y = y;
        part.z = z;
    }
}
