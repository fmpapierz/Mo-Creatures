package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Ogre model, converted faithfully from the legacy {@code MoCModelNewOgre} ({@code ModelBase}).
 * Geometry, texture offsets and the legacy parent-child hierarchy are preserved. The head sub-cubes
 * (brow/tusks/ears/hair/horns) share each head's pivot and are rotated to follow the look direction so
 * the whole face turns as one; the arms drive a melee smash from the swing progress and the legs add a
 * running kick — matching the legacy {@code setRotationAngles}.
 *
 * <p>One deliberate divergence from legacy: the multi-head variants (type 2/4/6) show all three heads,
 * and legacy parked the two extra 10-wide heads only 7px off-centre, burying 4px of each inside the
 * 12-wide centre head. Here they are fanned out — pivots pushed over the shoulders (x ±12.4) with a
 * baked ±3° outward yaw, and dropped 0.2 so their bottom faces sink into the shoulder tops instead of
 * z-fighting them. Clearance in MOTION (not just at rest) is guaranteed by three coupled choices,
 * SAT-verified across the whole reachable envelope plus interpolation margin (yaw ±21°, pitch ±30°):
 * the ±12.4/±3° fan, the side clusters taking the look pitch at half rate ({@link #SIDE_PITCH_FACTOR} —
 * a 12x12x12 head pitched 40° and yawed sweeps a ~10.4px radius, more than any wearable fan can
 * absorb at full rate), and {@code MoCEntityOgre} clamping the look envelope for multi-head types
 * ({@code getMaxHeadYRot} 15 / {@code getMaxHeadXRot} 25) so vanilla {@code BodyRotationControl}
 * swings the whole body around beyond that instead of owl-necking the heads into each other. The
 * residual contact left anywhere in the envelope is the accepted ~2-3px ear/ring seam class (see
 * HANDOFF_CONTINUE §3d); head boxes and faces never interpenetrate.
 */
public class MoCModelOgre extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    // Baked outward fan of the two extra heads (multi-head variants): pivots at x ±12.4 over the
    // shoulders plus ±3° of yaw (user feedback: ±13.5/±10° read too spread; this is the tightest
    // zero-clip candidate from the SAT search). The look yaw is added on top of these in setupAnim,
    // for the head cube and every flat sub-cube of its cluster alike, so each cluster stays rigid at
    // every look angle; the look PITCH reaches the side clusters at half rate (SIDE_PITCH_FACTOR),
    // which the clearance sweep needs (see the class javadoc) and which reads as the flanking heads
    // lazily half-following the centre head's gaze.
    private static final float HEAD2_BASE_YAW = 0.0523599F;
    private static final float HEAD3_BASE_YAW = -0.0523599F;
    private static final float SIDE_PITCH_FACTOR = 0.5F;

    private final ModelPart head;
    private final ModelPart head2;
    private final ModelPart head3;
    private final ModelPart rgtThigh;
    private final ModelPart lftThigh;
    private final ModelPart rgtLeg;
    private final ModelPart lftLeg;
    private final ModelPart rgtShoulder;
    private final ModelPart lftShoulder;
    private final ModelPart loinCloth;
    private final ModelPart buttCover;

    // Head sub-cubes grouped by head, with each part's baked base pitch, so they can track the look
    // direction about the shared head pivot without detaching from the head cube.
    private final ModelPart[] head1Sub;
    private final ModelPart[] head2Sub;
    private final ModelPart[] head3Sub;
    private final float[] head1BaseX;
    private final float[] head2BaseX;
    private final float[] head3BaseX;

    private static final String[] HEAD1_SUB = {
            "Brow", "NoseBridge", "Nose", "RgtTusk", "RgtTooth", "LftTooth", "LftTusk", "Lip",
            "RgtEar", "RgtRing", "RgtRingHole", "LftEar", "LftRing", "LftRingHole",
            "HairRope", "Hair1", "Hair2", "Hair3", "DiamondHorn", "RgtHorn", "RgtHornTip", "LftHorn", "LftHornTip"};
    private static final String[] HEAD2_SUB = {
            "Head2Chin", "Head2Lip", "Head2LftTusk", "Head2RgtTusk", "Head2Nose", "Head2NoseBridge",
            "Head2Brow", "Head2RgtHorn", "Head2LftHorn", "Head2DiamondHorn"};
    private static final String[] HEAD3_SUB = {
            "Head3RgtEar", "Head3LftEar", "Head3Eyelid", "Head3Nose", "Head3Brow", "Head3Hair", "Head3Lip",
            "Head3RgtTusk", "Head3RgtTooth", "Head3LftTooth", "Head3LftTusk", "Head3RingHole", "Head3Ring"};

    public MoCModelOgre(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("Head");
        this.head2 = root.getChild("Head2");
        this.head3 = root.getChild("Head3");
        this.rgtThigh = root.getChild("RgtThigh");
        this.lftThigh = root.getChild("LftThigh");
        this.rgtLeg = this.rgtThigh.getChild("RgtLeg");
        this.lftLeg = this.lftThigh.getChild("LftLeg");
        this.rgtShoulder = root.getChild("RgtShoulder");
        this.lftShoulder = root.getChild("LftShoulder");
        this.loinCloth = root.getChild("LoinCloth");
        this.buttCover = root.getChild("ButtCover");

        this.head1Sub = new ModelPart[HEAD1_SUB.length];
        this.head1BaseX = new float[HEAD1_SUB.length];
        for (int i = 0; i < HEAD1_SUB.length; i++) {
            this.head1Sub[i] = root.getChild(HEAD1_SUB[i]);
            this.head1BaseX[i] = this.head1Sub[i].xRot;
        }
        this.head2Sub = new ModelPart[HEAD2_SUB.length];
        this.head2BaseX = new float[HEAD2_SUB.length];
        for (int i = 0; i < HEAD2_SUB.length; i++) {
            this.head2Sub[i] = root.getChild(HEAD2_SUB[i]);
            this.head2BaseX[i] = this.head2Sub[i].xRot;
        }
        this.head3Sub = new ModelPart[HEAD3_SUB.length];
        this.head3BaseX = new float[HEAD3_SUB.length];
        for (int i = 0; i < HEAD3_SUB.length; i++) {
            this.head3Sub[i] = root.getChild(HEAD3_SUB[i]);
            this.head3BaseX[i] = this.head3Sub[i].xRot;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---- Single head (type 1/3/5) ----
        root.addOrReplaceChild("Head",
                CubeListBuilder.create().texOffs(80, 0).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("Brow",
                CubeListBuilder.create().texOffs(68, 7).addBox(-5.0F, -10.5F, -8.0F, 10.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, -0.0872665F, 0.0F, 0.0F));
        root.addOrReplaceChild("NoseBridge",
                CubeListBuilder.create().texOffs(80, 4).addBox(-1.0F, -7.0F, -8.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, -0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("Nose",
                CubeListBuilder.create().texOffs(80, 0).addBox(-2.0F, -7.0F, -7.0F, 4.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.0872665F, 0.0F, 0.0F));
        root.addOrReplaceChild("RgtTusk",
                CubeListBuilder.create().texOffs(60, 4).addBox(-3.5F, -6.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("RgtTooth",
                CubeListBuilder.create().texOffs(64, 4).addBox(-1.5F, -5.0F, -6.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("LftTooth",
                CubeListBuilder.create().texOffs(72, 4).addBox(0.5F, -5.0F, -6.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("LftTusk",
                CubeListBuilder.create().texOffs(76, 4).addBox(2.5F, -6.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("Lip",
                CubeListBuilder.create().texOffs(60, 0).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("RgtEar",
                CubeListBuilder.create().texOffs(60, 12).addBox(-9.0F, -9.0F, -1.0F, 3.0F, 5.0F, 2.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("RgtRing",
                CubeListBuilder.create().texOffs(32, 58).addBox(-8.0F, -6.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("RgtRingHole",
                CubeListBuilder.create().texOffs(26, 50).addBox(-8.0F, -5.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("LftEar",
                CubeListBuilder.create().texOffs(70, 12).addBox(6.0F, -9.0F, -1.0F, 3.0F, 5.0F, 2.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("LftRing",
                CubeListBuilder.create().texOffs(32, 58).addBox(7.0F, -6.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("LftRingHole",
                CubeListBuilder.create().texOffs(26, 50).addBox(7.0F, -5.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("HairRope",
                CubeListBuilder.create().texOffs(82, 83).addBox(-2.0F, -8.0F, 9.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.6108652F, 0.0F, 0.0F));
        root.addOrReplaceChild("Hair1",
                CubeListBuilder.create().texOffs(78, 107).addBox(-3.0F, -9.0F, 13.0F, 6.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.6108652F, 0.0F, 0.0F));
        root.addOrReplaceChild("Hair2",
                CubeListBuilder.create().texOffs(60, 107).addBox(-3.0F, -6.5F, 11.6F, 6.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("Hair3",
                CubeListBuilder.create().texOffs(42, 107).addBox(-3.0F, -2.4F, 11.4F, 6.0F, 8.0F, 3.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("DiamondHorn",
                CubeListBuilder.create().texOffs(120, 31).addBox(-1.0F, -17.0F, -6.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.0872665F, 0.0F, 0.0F));
        root.addOrReplaceChild("RgtHorn",
                CubeListBuilder.create().texOffs(46, 6).addBox(-6.0F, -12.0F, -11.0F, 2.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("RgtHornTip",
                CubeListBuilder.create().texOffs(44, 13).addBox(-6.0F, -15.0F, -11.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("LftHorn",
                CubeListBuilder.create().texOffs(46, 6).addBox(4.0F, -12.0F, -11.0F, 2.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        root.addOrReplaceChild("LftHornTip",
                CubeListBuilder.create().texOffs(52, 13).addBox(4.0F, -15.0F, -11.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));

        // ---- Body ----
        root.addOrReplaceChild("NeckRest",
                CubeListBuilder.create().texOffs(39, 20).addBox(-7.0F, -19.0F, -3.0F, 14.0F, 3.0F, 11.0F),
                PartPose.offset(0.0F, 5.0F, 0.0F));
        root.addOrReplaceChild("Chest",
                CubeListBuilder.create().texOffs(32, 34).addBox(-9.5F, -17.8F, -7.3F, 19.0F, 11.0F, 13.0F),
                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, -0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("Stomach",
                CubeListBuilder.create().texOffs(28, 58).addBox(-11.0F, -8.0F, -6.0F, 22.0F, 11.0F, 14.0F),
                PartPose.offset(0.0F, 5.0F, 0.0F));
        root.addOrReplaceChild("ButtCover",
                CubeListBuilder.create().texOffs(32, 118).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 8.0F, 2.0F),
                PartPose.offset(0.0F, 8.0F, 6.0F));
        root.addOrReplaceChild("LoinCloth",
                CubeListBuilder.create().texOffs(32, 118).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 2.0F),
                PartPose.offset(0.0F, 8.0F, -4.0F));

        // ---- Right leg ----
        PartDefinition rgtThigh = root.addOrReplaceChild("RgtThigh",
                CubeListBuilder.create().texOffs(0, 83).addBox(-10.0F, 0.0F, -5.0F, 10.0F, 11.0F, 10.0F),
                PartPose.offset(-2.0F, 4.0F, 1.0F));
        PartDefinition rgtLeg = rgtThigh.addOrReplaceChild("RgtLeg",
                CubeListBuilder.create().texOffs(0, 104).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 11.0F, 8.0F),
                PartPose.offset(-5.0F, 10.0F, 0.0F));
        rgtLeg.addOrReplaceChild("RgtKnee",
                CubeListBuilder.create().texOffs(0, 88).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 2.0F, -4.25F));
        rgtLeg.addOrReplaceChild("RgtToes",
                CubeListBuilder.create().texOffs(0, 123).addBox(-2.5F, -1.0F, -3.0F, 5.0F, 2.0F, 3.0F),
                PartPose.offset(-1.5F, 9.0F, -3.5F));
        rgtLeg.addOrReplaceChild("RgtBigToe",
                CubeListBuilder.create().texOffs(20, 123).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(2.5F, 9.0F, -4.0F));

        // ---- Left leg ----
        PartDefinition lftThigh = root.addOrReplaceChild("LftThigh",
                CubeListBuilder.create().texOffs(88, 83).addBox(0.0F, 0.0F, -5.0F, 10.0F, 11.0F, 10.0F),
                PartPose.offset(2.0F, 4.0F, 1.0F));
        PartDefinition lftLeg = lftThigh.addOrReplaceChild("LftLeg",
                CubeListBuilder.create().texOffs(96, 104).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 11.0F, 8.0F),
                PartPose.offset(5.0F, 10.0F, 0.0F));
        lftLeg.addOrReplaceChild("LftKnee",
                CubeListBuilder.create().texOffs(118, 88).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 2.0F, -4.25F));
        lftLeg.addOrReplaceChild("LftToes",
                CubeListBuilder.create().texOffs(112, 123).addBox(-2.5F, -1.0F, -3.0F, 5.0F, 2.0F, 3.0F),
                PartPose.offset(1.5F, 9.0F, -3.5F));
        lftLeg.addOrReplaceChild("LftBigToe",
                CubeListBuilder.create().texOffs(96, 123).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(-2.5F, 9.0F, -4.0F));

        // ---- Left arm ----
        PartDefinition lftShoulder = root.addOrReplaceChild("LftShoulder",
                CubeListBuilder.create().texOffs(96, 31).addBox(0.0F, -3.0F, -4.0F, 8.0F, 7.0F, 8.0F),
                PartPose.offset(7.0F, -10.0F, 2.0F));
        PartDefinition lftArm = lftShoulder.addOrReplaceChild("LftArm",
                CubeListBuilder.create().texOffs(100, 66).addBox(0.0F, 0.0F, -4.0F, 6.0F, 9.0F, 8.0F),
                PartPose.offset(6.0F, -1.0F, 1.0F));
        PartDefinition lftHand = lftArm.addOrReplaceChild("LftHand",
                CubeListBuilder.create().texOffs(96, 46).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 12.0F, 8.0F),
                PartPose.offset(3.0F, 8.0F, -1.0F));
        lftHand.addOrReplaceChild("LftElbow",
                CubeListBuilder.create().texOffs(86, 64).addBox(-2.0F, -1.5F, -0.5F, 4.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 2.5F, 4.0F));
        PartDefinition lftWeaponRoot = lftHand.addOrReplaceChild("LftWeaponRoot",
                CubeListBuilder.create().texOffs(24, 104).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(-0.5F, 8.5F, -4.0F));
        lftWeaponRoot.addOrReplaceChild("LftWeaponEnd",
                CubeListBuilder.create().texOffs(74, 90).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 8.0F));
        PartDefinition lftWeaponLump = lftWeaponRoot.addOrReplaceChild("LftWeaponLump",
                CubeListBuilder.create().texOffs(30, 83).addBox(-2.5F, -2.5F, -4.0F, 5.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition lftWeaponBetween = lftWeaponLump.addOrReplaceChild("LftWeaponBetween",
                CubeListBuilder.create().texOffs(83, 42).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition lftWeaponTip = lftWeaponBetween.addOrReplaceChild("LftWeaponTip",
                CubeListBuilder.create().texOffs(60, 118).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition lftHammerNeck = lftWeaponTip.addOrReplaceChild("LftHammerNeck",
                CubeListBuilder.create().texOffs(32, 39).addBox(-0.5F, -4.0F, -4.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -2.5F, -1.0F));
        PartDefinition lftHammerHeadSupport = lftWeaponTip.addOrReplaceChild("LftHammerHeadSupport",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 2.5F, -3.0F));
        lftHammerHeadSupport.addOrReplaceChild("LftHammerHead",
                CubeListBuilder.create().texOffs(32, 3).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 2.0F, 0.0F));
        lftWeaponTip.addOrReplaceChild("LftSpike",
                CubeListBuilder.create().texOffs(52, 118).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, -5.0F));
        lftWeaponTip.addOrReplaceChild("LftSpike1",
                CubeListBuilder.create().texOffs(52, 118).addBox(-3.0F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(-2.5F, 0.0F, -3.0F));
        lftWeaponTip.addOrReplaceChild("LftSpike2",
                CubeListBuilder.create().texOffs(52, 118).addBox(3.0F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(-0.5F, 0.0F, -3.0F));
        lftWeaponTip.addOrReplaceChild("LftSpike3",
                CubeListBuilder.create().texOffs(52, 118).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 2.5F, -3.0F));
        lftWeaponTip.addOrReplaceChild("LftSpike4",
                CubeListBuilder.create().texOffs(52, 118).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -2.5F, -3.0F));

        // ---- Right arm ----
        PartDefinition rgtShoulder = root.addOrReplaceChild("RgtShoulder",
                CubeListBuilder.create().texOffs(0, 31).addBox(0.0F, -3.0F, -4.0F, 8.0F, 7.0F, 8.0F),
                PartPose.offset(-15.0F, -10.0F, 2.0F));
        PartDefinition rgtArm = rgtShoulder.addOrReplaceChild("RgtArm",
                CubeListBuilder.create().texOffs(0, 66).addBox(0.0F, 0.0F, -4.0F, 6.0F, 9.0F, 8.0F),
                PartPose.offset(-4.0F, -1.0F, 1.0F));
        PartDefinition rgtHand = rgtArm.addOrReplaceChild("RgtHand",
                CubeListBuilder.create().texOffs(0, 46).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 12.0F, 8.0F),
                PartPose.offset(3.0F, 8.0F, -1.0F));
        rgtHand.addOrReplaceChild("RgtElbow",
                CubeListBuilder.create().texOffs(86, 64).addBox(-2.0F, -1.5F, -0.5F, 4.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 2.5F, 4.0F));
        PartDefinition rgtWeaponRoot = rgtHand.addOrReplaceChild("RgtWeaponRoot",
                CubeListBuilder.create().texOffs(24, 104).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(-0.5F, 8.5F, -4.0F));
        rgtWeaponRoot.addOrReplaceChild("RgtWeaponEnd",
                CubeListBuilder.create().texOffs(74, 90).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 8.0F));
        PartDefinition rgtWeaponLump = rgtWeaponRoot.addOrReplaceChild("RgtWeaponLump",
                CubeListBuilder.create().texOffs(30, 83).addBox(-2.5F, -2.5F, -4.0F, 5.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition rgtWeaponBetween = rgtWeaponLump.addOrReplaceChild("RgtWeaponBetween",
                CubeListBuilder.create().texOffs(83, 42).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition rgtWeaponTip = rgtWeaponBetween.addOrReplaceChild("RgtWeaponTip",
                CubeListBuilder.create().texOffs(60, 118).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, -2.0F));
        rgtWeaponTip.addOrReplaceChild("RgtHammerNeck",
                CubeListBuilder.create().texOffs(32, 39).addBox(-0.5F, -4.0F, -4.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -2.5F, -1.0F));
        PartDefinition rgtHammerHeadSupport = rgtWeaponTip.addOrReplaceChild("RgtHammerHeadSupport",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 2.5F, -3.0F));
        rgtHammerHeadSupport.addOrReplaceChild("RgtHammerHead",
                CubeListBuilder.create().texOffs(32, 3).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 2.0F, 0.0F));
        rgtWeaponTip.addOrReplaceChild("RgtSpike",
                CubeListBuilder.create().texOffs(52, 118).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, -5.0F));
        rgtWeaponTip.addOrReplaceChild("RgtSpike1",
                CubeListBuilder.create().texOffs(52, 118).addBox(-3.0F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(-2.5F, 0.0F, -3.0F));
        rgtWeaponTip.addOrReplaceChild("RgtSpike2",
                CubeListBuilder.create().texOffs(52, 118).addBox(3.0F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(-0.5F, 0.0F, -3.0F));
        rgtWeaponTip.addOrReplaceChild("RgtSpike3",
                CubeListBuilder.create().texOffs(52, 118).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 2.5F, -3.0F));
        rgtWeaponTip.addOrReplaceChild("RgtSpike4",
                CubeListBuilder.create().texOffs(52, 118).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -2.5F, -3.0F));

        // ---- Third head (multi-head variant, type 2/4/6) ----
        root.addOrReplaceChild("Head3RgtEar",
                CubeListBuilder.create().texOffs(110, 24).addBox(-8.0F, -9.0F, -1.0F, 3.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3LftEar",
                CubeListBuilder.create().texOffs(100, 24).addBox(5.0F, -9.0F, -1.0F, 3.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Eyelid",
                CubeListBuilder.create().texOffs(46, 3).addBox(-3.0F, -8.0F, -4.5F, 6.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.2617994F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Nose",
                CubeListBuilder.create().texOffs(60, 9).addBox(-1.5F, -8.5F, -3.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.4886922F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3",
                CubeListBuilder.create().texOffs(42, 83).addBox(-5.0F, -12.0F, -6.0F, 10.0F, 12.0F, 12.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Brow",
                CubeListBuilder.create().texOffs(46, 0).addBox(-3.0F, -9.0F, -8.5F, 6.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, -0.2617994F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Hair",
                CubeListBuilder.create().texOffs(80, 118).addBox(-2.0F, -17.0F, -5.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, -0.6108652F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Lip",
                CubeListBuilder.create().texOffs(22, 68).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3RgtTusk",
                CubeListBuilder.create().texOffs(83, 34).addBox(-3.5F, -6.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3RgtTooth",
                CubeListBuilder.create().texOffs(87, 34).addBox(-1.5F, -5.0F, -6.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3LftTooth",
                CubeListBuilder.create().texOffs(96, 34).addBox(0.5F, -5.0F, -6.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3LftTusk",
                CubeListBuilder.create().texOffs(100, 34).addBox(2.5F, -6.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3RingHole",
                CubeListBuilder.create().texOffs(26, 50).addBox(6.0F, -5.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Ring",
                CubeListBuilder.create().texOffs(32, 58).addBox(6.0F, -6.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(12.4F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));

        // ---- Second head (multi-head variant, type 2/4/6) ----
        root.addOrReplaceChild("Head2Chin",
                CubeListBuilder.create().texOffs(21, 24).addBox(-3.0F, -5.0F, -8.0F, 6.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, 0.2617994F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -12.0F, -6.0F, 10.0F, 12.0F, 12.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, 0.0F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2Lip",
                CubeListBuilder.create().texOffs(0, 24).addBox(-4.0F, -5.0F, -8.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, 0.0F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2LftTusk",
                CubeListBuilder.create().texOffs(46, 28).addBox(2.5F, -8.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, 0.1745329F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2RgtTusk",
                CubeListBuilder.create().texOffs(39, 28).addBox(-3.5F, -8.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, 0.1745329F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2Nose",
                CubeListBuilder.create().texOffs(116, 0).addBox(-2.0F, -7.0F, -7.0F, 4.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, 0.0872665F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2NoseBridge",
                CubeListBuilder.create().texOffs(116, 4).addBox(-1.0F, -7.0F, -8.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, -0.1745329F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2Brow",
                CubeListBuilder.create().texOffs(80, 24).addBox(-4.0F, -10.5F, -8.0F, 8.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, -0.0872665F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2RgtHorn",
                CubeListBuilder.create().texOffs(24, 30).addBox(-4.0F, -8.0F, -15.0F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, -0.5235988F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2LftHorn",
                CubeListBuilder.create().texOffs(24, 30).addBox(2.0F, -8.0F, -15.0F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, -0.5235988F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2DiamondHorn",
                CubeListBuilder.create().texOffs(120, 46).addBox(-1.0F, -17.0F, -6.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-12.4F, -12.8F, 0.0F, 0.0872665F, HEAD2_BASE_YAW, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float headPitch = state.xRot * DEG_TO_RAD;
        float headYaw = state.yRot * DEG_TO_RAD;
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;

        // head look tracking (single head + both extra heads follow the look direction; the extra
        // heads swivel around their baked outward yaw and take the pitch at half rate — both are part
        // of the verified clearance budget, see the class javadoc)
        float sidePitch = headPitch * SIDE_PITCH_FACTOR;
        this.head.xRot = headPitch;
        this.head.yRot = headYaw;
        this.head2.xRot = sidePitch;
        this.head2.yRot = HEAD2_BASE_YAW + headYaw;
        this.head3.xRot = sidePitch;
        this.head3.yRot = HEAD3_BASE_YAW + headYaw;

        // The head sub-cubes (brows/tusks/ears/hair/horns) share their head's pivot, so rotate them with
        // the look direction (base pitch + look pitch, base yaw + look yaw) to keep the whole face
        // turning as one — the base yaw and pitch rate match their head's, keeping each cluster rigid
        // at every angle.
        for (int i = 0; i < this.head1Sub.length; i++) {
            this.head1Sub[i].xRot = this.head1BaseX[i] + headPitch;
            this.head1Sub[i].yRot = headYaw;
        }
        for (int i = 0; i < this.head2Sub.length; i++) {
            this.head2Sub[i].xRot = this.head2BaseX[i] + sidePitch;
            this.head2Sub[i].yRot = HEAD2_BASE_YAW + headYaw;
        }
        for (int i = 0; i < this.head3Sub.length; i++) {
            this.head3Sub[i].xRot = this.head3BaseX[i] + sidePitch;
            this.head3Sub[i].yRot = HEAD3_BASE_YAW + headYaw;
        }

        // leg gait — with a stomping running kick that lifts the trailing leg higher at speed (legacy: an
        // extra ~25deg bend once the ogre is moving faster than a walk).
        float runKick = limbAmount > 0.15F ? 0.4363F * limbAmount : 0.0F;
        float rLegXRot = Mth.cos((limbSwing * 0.6662F) + (float) Math.PI) * 0.8F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;
        float clothRot = Mth.cos(limbSwing * 0.9F) * 0.6F * limbAmount;

        this.rgtThigh.xRot = rLegXRot;
        this.lftThigh.xRot = lLegXRot;
        this.rgtLeg.xRot = rLegXRot - Math.max(0.0F, runKick * Mth.cos(limbSwing * 0.6662F));
        this.lftLeg.xRot = lLegXRot - Math.max(0.0F, runKick * Mth.cos((limbSwing * 0.6662F) + (float) Math.PI));
        this.loinCloth.xRot = clothRot;
        this.buttCover.xRot = clothRot;

        // Arms: idle sway blended with a two-fisted overhead smash driven by the melee swing progress
        // (legacy leftAttack/rightAttack raised the shoulders and punched the fists down).
        float smash = Mth.sin(state.attackSwing * (float) Math.PI); // 0 at rest, 1 mid-swing
        this.lftShoulder.zRot = (Mth.cos(ageInTicks * 0.09F) * 0.05F) - 0.05F;
        this.lftShoulder.xRot = rLegXRot - smash * 2.0F;
        this.rgtShoulder.zRot = -(Mth.cos(ageInTicks * 0.09F) * 0.05F) + 0.05F;
        this.rgtShoulder.xRot = lLegXRot - smash * 2.0F;
    }
}
