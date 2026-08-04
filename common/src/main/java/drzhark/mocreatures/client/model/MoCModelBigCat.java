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
 * BigCat model, converted faithfully from the original 12.0.5 {@code MoCModelNewBigCat}
 * ({@code ModelBase}). This is the model the {@code bc*} (lion / tiger / panther / etc.)
 * textures are authored for: a 128&times;128 texture sheet with a detailed feline body
 * (head with snout, lips, fangs, teeth, ears, mane / beard hair; a horizontal chest +
 * abdomen + rump; a five-segment tail; four legs with claws; plus optional saddle, storage
 * chest, manticore wings and scorpion stinger-tail).
 *
 * <p>The legacy hierarchy is preserved exactly via nested {@link PartDefinition}s so that the
 * parent transforms propagate the same way they did under the old {@code ModelRenderer} tree.
 * Critically, {@link #createBodyLayer()} returns a {@code 128 x 128} layer definition — the
 * real texture size of this model — which is what makes the {@code bc*} textures map correctly.
 *
 * <p>Degrees are converted with {@link #DEG_TO_RAD} (the legacy code used {@code value / radianF}
 * where {@code radianF = 180/PI}, i.e. multiply by PI/180).
 */
public class MoCModelBigCat extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart chest;
    private final ModelPart neckBase;
    private final ModelPart headBack;
    private final ModelPart head;
    private final ModelPart lowerJaw;
    private final ModelPart abdomen;
    private final ModelPart tailRoot;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart tail4;
    private final ModelPart tailTip;
    private final ModelPart tailTusk;
    private final ModelPart leftUpperLeg;
    private final ModelPart leftLowerLeg;
    private final ModelPart rightUpperLeg;
    private final ModelPart rightLowerLeg;
    private final ModelPart leftHindUpperLeg;
    private final ModelPart leftHindFoot;
    private final ModelPart rightHindUpperLeg;
    private final ModelPart rightHindFoot;
    private final ModelPart collar;
    private final ModelPart leftFootHarness;
    private final ModelPart rightFootHarness;

    // Rideable saddle group (saddle pad + front/back + stirrups) -> shown only when saddled.
    private final ModelPart saddle;
    private final ModelPart saddleFront;
    private final ModelPart saddleBack;

    // Pack/harness equipment with no equip system in this port -> always hidden.
    private final ModelPart neckHarness;
    private final ModelPart harnessStick;
    private final ModelPart storageChest;

    public MoCModelBigCat(ModelPart root) {
        super(root);
        this.chest = root.getChild("chest");
        this.neckBase = this.chest.getChild("neck_base");
        this.headBack = this.neckBase.getChild("head_back");
        this.head = this.headBack.getChild("head");
        this.lowerJaw = this.head.getChild("lower_jaw");
        this.collar = this.neckBase.getChild("collar");
        this.abdomen = this.chest.getChild("abdomen");
        this.tailRoot = this.abdomen.getChild("tail_root");
        this.tail2 = this.tailRoot.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");
        this.tail4 = this.tail3.getChild("tail4");
        this.tailTip = this.tail4.getChild("tail_tip");
        this.tailTusk = this.tail4.getChild("tail_tusk");
        this.leftUpperLeg = this.chest.getChild("left_upper_leg");
        this.leftLowerLeg = this.leftUpperLeg.getChild("left_lower_leg");
        this.rightUpperLeg = this.chest.getChild("right_upper_leg");
        this.rightLowerLeg = this.rightUpperLeg.getChild("right_lower_leg");
        this.leftHindUpperLeg = this.abdomen.getChild("left_hind_upper_leg");
        this.leftHindFoot = this.leftHindUpperLeg.getChild("left_ankle")
                .getChild("left_hind_lower_leg").getChild("left_hind_foot");
        this.rightHindUpperLeg = this.abdomen.getChild("right_hind_upper_leg");
        this.rightHindFoot = this.rightHindUpperLeg.getChild("right_ankle")
                .getChild("right_hind_lower_leg").getChild("right_hind_foot");
        this.saddle = this.chest.getChild("saddle");
        this.saddleFront = this.saddle.getChild("saddle_front");
        this.saddleBack = this.saddle.getChild("saddle_back");
        this.leftFootHarness = this.saddle.getChild("left_foot_harness");
        this.rightFootHarness = this.saddle.getChild("right_foot_harness");

        this.neckHarness = this.headBack.getChild("neck_harness");
        this.harnessStick = this.headBack.getChild("harness_stick");
        this.storageChest = this.abdomen.getChild("storage_chest");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

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

        // ---- Tail chain ----
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

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        // Legacy gait: cos(f * 0.8 [+PI]) * 0.8 * f1 for the standing (non-galloping) walk.
        float rLegXRot = Mth.cos(limbSwing * 0.8F + 3.141593F) * 0.8F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.8F) * 0.8F * limbAmount;

        // Front legs (baseline +15deg) and hind legs (baseline -25deg), diagonally paired.
        this.rightUpperLeg.xRot = 15.0F * DEG_TO_RAD + rLegXRot;
        this.leftHindUpperLeg.xRot = -25.0F * DEG_TO_RAD + rLegXRot;
        this.leftUpperLeg.xRot = 15.0F * DEG_TO_RAD + lLegXRot;
        this.rightHindUpperLeg.xRot = -25.0F * DEG_TO_RAD + lLegXRot;

        // Lower legs and feet keep their baked baseline pitch.
        this.rightLowerLeg.xRot = -21.5F * DEG_TO_RAD;
        this.leftLowerLeg.xRot = -21.5F * DEG_TO_RAD;
        this.rightHindFoot.xRot = 27.0F * DEG_TO_RAD;
        this.leftHindFoot.xRot = 27.0F * DEG_TO_RAD;

        // Stirrup harness sway (legacy: RLegXRot / 3 pitch, +/- RLegXRot / 5 roll).
        this.leftFootHarness.xRot = rLegXRot / 3.0F;
        this.rightFootHarness.xRot = rLegXRot / 3.0F;
        this.leftFootHarness.zRot = rLegXRot / 5.0F;
        this.rightFootHarness.zRot = -rLegXRot / 5.0F;

        // Tail sway on top of the baked baseline pitches.
        float tailXRot = Mth.cos(limbSwing * 0.4F) * 0.15F * limbAmount;
        this.tailRoot.xRot = 87.0F * DEG_TO_RAD + tailXRot;
        this.tail2.xRot = -30.0F * DEG_TO_RAD + tailXRot;
        this.tail3.xRot = -17.0F * DEG_TO_RAD + tailXRot;
        this.tail4.xRot = 21.0F * DEG_TO_RAD + tailXRot;
        this.tailTip.xRot = 21.0F * DEG_TO_RAD + tailXRot;
        this.tailTusk.xRot = 21.0F * DEG_TO_RAD + tailXRot;

        // Collar bob (tamed-only part; geometry is always present).
        this.collar.xRot = 20.0F * DEG_TO_RAD + Mth.cos(limbSwing * 0.8F) * 0.5F * limbAmount;

        // Head tracking: head_back carries the look angles (baked baseline +14deg pitch).
        float headPitch = state.xRot * DEG_TO_RAD;
        float headYaw = state.yRot * DEG_TO_RAD;
        this.headBack.xRot = 14.0F * DEG_TO_RAD + headPitch;
        this.headBack.yRot = headYaw;

        // Lower jaw drops for a roar/bite when the cat is biting (legacy mouth-open counter), else stays shut.
        this.lowerJaw.xRot = state.bigcatJawOpen ? 0.6F : 0.0F;

        // Equipment visibility. Big cats in this port are rideable (saddle) but have no harness/chest
        // equip system, so the saddle group (pad + front/back + stirrup harnesses) shows only when
        // saddled, and the pulling harness (neck_harness, harness_stick) and storage_chest are always
        // hidden. The saddle pad is the parent of the stirrups, but each part carries its own visible
        // flag, so toggle the stirrups explicitly too.
        boolean saddled = state.saddled;
        this.saddle.visible = saddled;
        this.saddleFront.visible = saddled;
        this.saddleBack.visible = saddled;
        this.leftFootHarness.visible = saddled;
        this.rightFootHarness.visible = saddled;

        this.neckHarness.visible = false;
        this.harnessStick.visible = false;
        this.storageChest.visible = false;
    }
}
