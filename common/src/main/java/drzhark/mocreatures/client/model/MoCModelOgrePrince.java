package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Ogre Prince model: {@link MoCModelOgre}'s geometry verbatim, on a 128x256 atlas whose top half
 * (v 0-127) is pixel-identical to the 128x128 ogre sheets — an unmodified ogre texture pasted into
 * the top half renders a correct ogre. The bottom half (v >= 128) holds the prince's regalia, new
 * parts toggled per variant from {@code state.typeMoC}:
 * <ul>
 *   <li><strong>1 green prince</strong> (default): single centre head wearing a gold points crown and
 *       a hanging beard.</li>
 *   <li><strong>2 fire prince</strong>: the two-headed ogre (centre head chain hidden), each head
 *       crowned by a row of horn spikes.</li>
 *   <li><strong>3 cave prince</strong>: single centre head with an upright crystal mohawk crest.</li>
 * </ul>
 * The regalia are real children of their head parts, so they inherit the look rotation for free and
 * vanish with a hidden parent ({@code ModelPart.render} skips the whole subtree when {@code visible}
 * is false — verified in the 26.2 mapped source). The ogre's own head sub-cubes are flat root-level
 * parts, so the per-variant toggling must (and does) flip every one of them each frame; {@code
 * Model.setupAnim} resets poses but never {@code visible}, which persists across frames. Both hand
 * clubs stay visible for every variant. Every regalia box is at least 1px thick on every axis
 * (unculled rendering tears zero-thickness boxes), and no new face is coplanar with a base-model face.
 *
 * <p>One deliberate divergence from the ogre's geometry: the fire prince's Head2/Head3 keep their own
 * poses rather than inheriting the ogre's. The ogre shows the centre head alongside them and needs its
 * side heads pushed out to x ±12.4; here the centre head is hidden, so the pair stays tight at x ±7.2
 * with a baked ±10° outward yaw — a clean symmetric fan with a ~2.5px gap between the two heads —
 * and is dropped 0.2 so the head bottoms sink into the shoulder tops instead of z-fighting them. The
 * full look rotation is added on top of the baked yaw in setupAnim; clearance in MOTION is
 * SAT-verified across yaw ±30° x pitch ±30° (zero head-box interpenetration at ±7.2), and
 * {@code MoCEntityOgrePrince} clamps the fire prince's look envelope ({@code getMaxHeadYRot} 25 /
 * {@code getMaxHeadXRot} 25) so the body turns beyond that.
 */
public class MoCModelOgrePrince extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    // Baked outward fan of the fire prince's two heads: ±10° of yaw at x ±7.2 (the centre head is
    // hidden for this variant, so the pair sits tighter than the ogre's ±12.4 side heads). The look
    // rotation is added on top in setupAnim, for the head cube and every sub-cube of its cluster
    // alike, so each cluster stays rigid at every look angle.
    private static final float HEAD2_BASE_YAW = 0.1745329F;
    private static final float HEAD3_BASE_YAW = -0.1745329F;

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

    // Regalia (children of their head, so they follow the look rotation automatically).
    private final ModelPart crownGold;
    private final ModelPart beard;
    private final ModelPart hornsLeft;
    private final ModelPart hornsRight;
    private final ModelPart crestCrystal;

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

    public MoCModelOgrePrince(ModelPart root) {
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

        this.crownGold = this.head.getChild("crown_gold");
        this.beard = this.head.getChild("beard");
        this.crestCrystal = this.head.getChild("crest_crystal");
        this.hornsLeft = this.head2.getChild("horns_left");
        this.hornsRight = this.head3.getChild("horns_right");

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

        // ================= Base ogre geometry — copied verbatim from MoCModelOgre ==================
        // Do not touch: texture rows 0-127 must stay pixel-compatible with the 128x128 ogre sheets.

        // ---- Single head (green/cave prince) ----
        PartDefinition head = root.addOrReplaceChild("Head",
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
        lftWeaponTip.addOrReplaceChild("LftHammerNeck",
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

        // ---- Third head (fire prince, entity-left at x +7) ----
        root.addOrReplaceChild("Head3RgtEar",
                CubeListBuilder.create().texOffs(110, 24).addBox(-8.0F, -9.0F, -1.0F, 3.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3LftEar",
                CubeListBuilder.create().texOffs(100, 24).addBox(5.0F, -9.0F, -1.0F, 3.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Eyelid",
                CubeListBuilder.create().texOffs(46, 3).addBox(-3.0F, -8.0F, -4.5F, 6.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.2617994F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Nose",
                CubeListBuilder.create().texOffs(60, 9).addBox(-1.5F, -8.5F, -3.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.4886922F, HEAD3_BASE_YAW, 0.0F));
        PartDefinition head3 = root.addOrReplaceChild("Head3",
                CubeListBuilder.create().texOffs(42, 83).addBox(-5.0F, -12.0F, -6.0F, 10.0F, 12.0F, 12.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Brow",
                CubeListBuilder.create().texOffs(46, 0).addBox(-3.0F, -9.0F, -8.5F, 6.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, -0.2617994F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Hair",
                CubeListBuilder.create().texOffs(80, 118).addBox(-2.0F, -17.0F, -5.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, -0.6108652F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Lip",
                CubeListBuilder.create().texOffs(22, 68).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3RgtTusk",
                CubeListBuilder.create().texOffs(83, 34).addBox(-3.5F, -6.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3RgtTooth",
                CubeListBuilder.create().texOffs(87, 34).addBox(-1.5F, -5.0F, -6.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3LftTooth",
                CubeListBuilder.create().texOffs(96, 34).addBox(0.5F, -5.0F, -6.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3LftTusk",
                CubeListBuilder.create().texOffs(100, 34).addBox(2.5F, -6.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.1745329F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3RingHole",
                CubeListBuilder.create().texOffs(26, 50).addBox(6.0F, -5.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head3Ring",
                CubeListBuilder.create().texOffs(32, 58).addBox(6.0F, -6.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(7.2F, -12.8F, 0.0F, 0.0F, HEAD3_BASE_YAW, 0.0F));

        // ---- Second head (fire prince, entity-right at x -7) ----
        root.addOrReplaceChild("Head2Chin",
                CubeListBuilder.create().texOffs(21, 24).addBox(-3.0F, -5.0F, -8.0F, 6.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, 0.2617994F, HEAD2_BASE_YAW, 0.0F));
        PartDefinition head2 = root.addOrReplaceChild("Head2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -12.0F, -6.0F, 10.0F, 12.0F, 12.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, 0.0F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2Lip",
                CubeListBuilder.create().texOffs(0, 24).addBox(-4.0F, -5.0F, -8.0F, 8.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, 0.0F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2LftTusk",
                CubeListBuilder.create().texOffs(46, 28).addBox(2.5F, -8.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, 0.1745329F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2RgtTusk",
                CubeListBuilder.create().texOffs(39, 28).addBox(-3.5F, -8.0F, -6.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, 0.1745329F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2Nose",
                CubeListBuilder.create().texOffs(116, 0).addBox(-2.0F, -7.0F, -7.0F, 4.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, 0.0872665F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2NoseBridge",
                CubeListBuilder.create().texOffs(116, 4).addBox(-1.0F, -7.0F, -8.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, -0.1745329F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2Brow",
                CubeListBuilder.create().texOffs(80, 24).addBox(-4.0F, -10.5F, -8.0F, 8.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, -0.0872665F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2RgtHorn",
                CubeListBuilder.create().texOffs(24, 30).addBox(-4.0F, -8.0F, -15.0F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, -0.5235988F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2LftHorn",
                CubeListBuilder.create().texOffs(24, 30).addBox(2.0F, -8.0F, -15.0F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, -0.5235988F, HEAD2_BASE_YAW, 0.0F));
        root.addOrReplaceChild("Head2DiamondHorn",
                CubeListBuilder.create().texOffs(120, 46).addBox(-1.0F, -17.0F, -6.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-7.2F, -12.8F, 0.0F, 0.0872665F, HEAD2_BASE_YAW, 0.0F));

        // ============================ Regalia — all texOffs at v >= 128 ============================
        // Children of their head part: they inherit the look rotation and vanish with a hidden parent.
        // Coordinates are head-local (the shared head pivot); the head cube spans x/z -6..6 (centre
        // head; side heads x -5..5), y -12 (crown of skull) .. 0 (chin). Nothing here is coplanar with
        // a base-model face (points are embedded or fractionally offset to dodge z-fighting).

        // Gold points crown (green prince): a 2px band ringing the head top with five points — a tall
        // centre point flanked by four corner points, all embedded 1px+ so no bottom face shows.
        head.addOrReplaceChild("crown_gold", CubeListBuilder.create()
                        .texOffs(0, 130).addBox(-6.5F, -13.0F, -6.5F, 13.0F, 2.0F, 1.0F)   // band, front
                        .texOffs(0, 130).addBox(-6.5F, -13.0F, 5.5F, 13.0F, 2.0F, 1.0F)    // band, back
                        .texOffs(30, 130).addBox(-6.5F, -13.0F, -5.5F, 1.0F, 2.0F, 11.0F)  // band, right side
                        .texOffs(30, 130).addBox(5.5F, -13.0F, -5.5F, 1.0F, 2.0F, 11.0F)   // band, left side
                        .texOffs(58, 130).addBox(-1.5F, -16.5F, -6.25F, 3.0F, 5.0F, 1.0F)  // centre point
                        .texOffs(70, 130).addBox(-5.5F, -14.5F, -6.25F, 2.0F, 3.0F, 1.0F)  // front-right point
                        .texOffs(70, 130).addBox(3.5F, -14.5F, -6.25F, 2.0F, 3.0F, 1.0F)   // front-left point
                        .texOffs(70, 130).addBox(-5.5F, -14.5F, 5.25F, 2.0F, 3.0F, 1.0F)   // back-right point
                        .texOffs(70, 130).addBox(3.5F, -14.5F, 5.25F, 2.0F, 3.0F, 1.0F),   // back-left point
                PartPose.ZERO);

        // Beard (green prince): three overlapping strand slabs hanging from the chin, each narrower
        // and further back than the one above so the layers read as separate strands. The upper slab
        // spans the same x ±4 as the base ogre's Lip, and both rotate about the same head pivot, so
        // their side faces would be exactly coplanar at EVERY look angle (rotation about the shared
        // pivot preserves the shared x-planes) — a permanent ~1px z-fighting seam per side. The
        // x-only CubeDeformation flares the geometry to ±4.15 without touching the UV layout, which
        // still samples the 8px-wide painted rects exactly.
        head.addOrReplaceChild("beard", CubeListBuilder.create()
                        .texOffs(0, 146).addBox(-4.0F, -2.0F, -7.1F, 8.0F, 5.0F, 1.0F,
                                new CubeDeformation(0.15F, 0.0F, 0.0F))                    // upper slab
                        .texOffs(22, 146).addBox(-3.0F, 2.5F, -6.7F, 6.0F, 4.0F, 1.0F)     // middle strands
                        .texOffs(40, 146).addBox(-1.5F, 6.0F, -6.4F, 3.0F, 3.0F, 1.0F),    // tip strand
                PartPose.ZERO);

        // Fire prince horn rows: three spikes along each head's midline crest, sunk 1px into the
        // skull. Head2's row sits back of its diamond horn; Head3's row sits ahead of its hair tuft.
        head2.addOrReplaceChild("horns_left", CubeListBuilder.create()
                        .texOffs(0, 156).addBox(-1.0F, -15.0F, -2.0F, 2.0F, 4.0F, 2.0F)    // front spike
                        .texOffs(12, 156).addBox(-1.0F, -16.0F, 0.5F, 2.0F, 5.0F, 2.0F)    // tall middle spike
                        .texOffs(0, 156).addBox(-1.0F, -15.0F, 3.0F, 2.0F, 4.0F, 2.0F),    // rear spike
                PartPose.ZERO);
        head3.addOrReplaceChild("horns_right", CubeListBuilder.create()
                        .texOffs(24, 156).addBox(-1.0F, -15.0F, -4.5F, 2.0F, 4.0F, 2.0F)   // front spike
                        .texOffs(36, 156).addBox(-1.0F, -16.0F, -2.0F, 2.0F, 5.0F, 2.0F)   // tall middle spike
                        .texOffs(24, 156).addBox(-1.0F, -15.0F, 0.5F, 2.0F, 4.0F, 2.0F),   // rear spike
                PartPose.ZERO);

        // Cave prince crystal crest: an upright centre shard with a forward- and a backward-leaning
        // shard as its children (they hide with it). The leaners pivot at the skull top and stay clear
        // of the DiamondHorn in front and the HairRope behind.
        PartDefinition crestCrystal = head.addOrReplaceChild("crest_crystal",
                CubeListBuilder.create().texOffs(48, 156).addBox(-1.5F, -18.0F, -1.5F, 3.0F, 7.0F, 3.0F),
                PartPose.ZERO);
        crestCrystal.addOrReplaceChild("crest_front",
                CubeListBuilder.create().texOffs(64, 156).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -12.0F, -2.0F, 0.2617994F, 0.0F, 0.0F));
        crestCrystal.addOrReplaceChild("crest_back",
                CubeListBuilder.create().texOffs(76, 156).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -12.0F, 2.0F, -0.2617994F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 256);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float headPitch = state.xRot * DEG_TO_RAD;
        float headYaw = state.yRot * DEG_TO_RAD;
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;

        // head look tracking (single head + both extra heads follow the look direction; the fire
        // pair swivels around its baked outward yaw); the regalia are children of the heads, so they
        // turn with them for free.
        this.head.xRot = headPitch;
        this.head.yRot = headYaw;
        this.head2.xRot = headPitch;
        this.head2.yRot = HEAD2_BASE_YAW + headYaw;
        this.head3.xRot = headPitch;
        this.head3.yRot = HEAD3_BASE_YAW + headYaw;

        // The head sub-cubes (brows/tusks/ears/hair/horns) share their head's pivot, so rotate them with
        // the look direction (base pitch + look pitch, base yaw + look yaw) to keep the whole face
        // turning as one — the base yaw matches their head's, keeping each cluster rigid at every angle.
        for (int i = 0; i < this.head1Sub.length; i++) {
            this.head1Sub[i].xRot = this.head1BaseX[i] + headPitch;
            this.head1Sub[i].yRot = headYaw;
        }
        for (int i = 0; i < this.head2Sub.length; i++) {
            this.head2Sub[i].xRot = this.head2BaseX[i] + headPitch;
            this.head2Sub[i].yRot = HEAD2_BASE_YAW + headYaw;
        }
        for (int i = 0; i < this.head3Sub.length; i++) {
            this.head3Sub[i].xRot = this.head3BaseX[i] + headPitch;
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

        // Per-variant visibility (typeMoC: 1 green, 2 fire, 3 cave; anything else falls back to green).
        // ModelPart.visible persists across frames and is NOT reset by super.setupAnim, so every toggled
        // part is set every frame. Hiding a head hides its child regalia too, but the ogre's own head
        // sub-cubes are flat root-level parts and must be flipped individually. Both weapon clubs are
        // untouched — visible for every variant.
        boolean fire = state.typeMoC == 2;
        boolean cave = state.typeMoC == 3;
        this.head.visible = !fire;
        for (ModelPart part : this.head1Sub) {
            part.visible = !fire;
        }
        this.head2.visible = fire;
        for (ModelPart part : this.head2Sub) {
            part.visible = fire;
        }
        this.head3.visible = fire;
        for (ModelPart part : this.head3Sub) {
            part.visible = fire;
        }
        this.crownGold.visible = !fire && !cave;
        this.beard.visible = !fire && !cave;
        this.hornsLeft.visible = fire;
        this.hornsRight.visible = fire;
        this.crestCrystal.visible = cave; // crest_front/crest_back are its children and follow it
    }
}
