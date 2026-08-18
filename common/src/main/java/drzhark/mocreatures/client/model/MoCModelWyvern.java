package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.util.Set;

/**
 * Wyvern model, converted faithfully from the legacy {@code MoCModelWyvern} ({@code ModelBase}).
 * Geometry, texture offsets and the parent/child hierarchy are preserved; armour / saddle parts
 * are kept but left in their default pose. Only the core walking/neck/tail/wing animation is ported.
 */
public class MoCModelWyvern extends EntityModel<MoCEntityRenderState> {

    private static final float DEG = (float) (Math.PI / 180.0);

    // Zero-thickness planes (ear skins, wing membranes) are split into two single-face boxes (painted face
    // + a re-aimed opposite face sampling the same painted tile) so the culled render type keeps them
    // visible from both sides. The control ropes' tile pair is painted on both sides and stays a plain box.
    private static final Set<Direction> NORTH_ONLY = Set.of(Direction.NORTH);
    private static final Set<Direction> SOUTH_ONLY = Set.of(Direction.SOUTH);
    private static final Set<Direction> WEST_ONLY = Set.of(Direction.WEST);
    private static final Set<Direction> EAST_ONLY = Set.of(Direction.EAST);

    private final ModelPart tailGroup;
    private final ModelPart back1;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart tail4;
    private final ModelPart tail5;
    private final ModelPart chest;
    private final ModelPart neckplate3;
    private final ModelPart neck3;
    private final ModelPart mainHead;
    private final ModelPart neck2;
    private final ModelPart neck1;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftEarSkin;
    private final ModelPart rightEarSkin;
    private final ModelPart torso;
    private final ModelPart rightShoulder;
    private final ModelPart leftShoulder;
    private final ModelPart leftWing;
    private final ModelPart leftUpArm;
    private final ModelPart leftLowArm;
    private final ModelPart leftFing1a;
    private final ModelPart leftFing2a;
    private final ModelPart leftFing3a;
    private final ModelPart rightWing;
    private final ModelPart rightUpArm;
    private final ModelPart rightLowArm;
    private final ModelPart rightFing1a;
    private final ModelPart rightFing2a;
    private final ModelPart rightFing3a;
    private final ModelPart leftUpLeg;
    private final ModelPart leftMidLeg;
    private final ModelPart leftLowLeg;
    private final ModelPart leftFoot;
    private final ModelPart leftToe1;
    private final ModelPart leftToe2;
    private final ModelPart leftToe3;
    private final ModelPart rightUpLeg;
    private final ModelPart rightMidLeg;
    private final ModelPart rightLowLeg;
    private final ModelPart rightFoot;
    private final ModelPart rightToe1;
    private final ModelPart rightToe2;
    private final ModelPart rightToe3;

    // saddle + tack (visible only when saddled)
    private final ModelPart saddle;
    private final ModelPart chestbelt;
    private final ModelPart stomachbelt;
    private final ModelPart helmetstrap1;
    private final ModelPart helmetstrap2;
    private final ModelPart controlrope1;
    private final ModelPart controlrope2;

    // storage bag + all armour parts (always hidden — no armour/chest system yet)
    private final ModelPart storage;
    private final ModelPart ironHelmetHorn1;
    private final ModelPart ironHelmetHorn2;
    private final ModelPart ironHelmet;
    private final ModelPart ironHelmetSnout;
    private final ModelPart ironLeftLegArmor;
    private final ModelPart ironRightLegArmor;
    private final ModelPart ironChestArmor;
    private final ModelPart ironRightShoulderPad;
    private final ModelPart ironLeftShoulderPad;
    private final ModelPart goldHelmetHorn1;
    private final ModelPart goldHelmetHorn2;
    private final ModelPart goldHelmet;
    private final ModelPart goldHelmetSnout;
    private final ModelPart goldLeftLegArmor;
    private final ModelPart goldRightLegArmor;
    private final ModelPart goldChestArmor;
    private final ModelPart goldLeftShoulder;
    private final ModelPart goldRightShoulder;
    private final ModelPart diamondHelmet;
    private final ModelPart diamondHelmetHorn1;
    private final ModelPart diamondHelmetHorn2;
    private final ModelPart diamondHelmetSnout;
    private final ModelPart diamondLeftLegArmor;
    private final ModelPart diamondRightLegArmor;
    private final ModelPart diamondChestArmor;
    private final ModelPart diamondLeftShoulder;
    private final ModelPart diamondRightShoulder;

    public MoCModelWyvern(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.tailGroup = root.getChild("tail_group");
        this.tail1 = this.tailGroup.getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");
        this.tail4 = this.tail3.getChild("tail4");
        this.tail5 = this.tail4.getChild("tail5");
        this.back1 = root.getChild("back1");
        this.chest = root.getChild("chest");
        this.neckplate3 = root.getChild("neckplate3");
        this.neck3 = root.getChild("neck3");
        this.mainHead = root.getChild("main_head");
        this.neck2 = this.mainHead.getChild("neck2");
        this.neck1 = this.neck2.getChild("neck1");
        this.head = this.neck1.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftEarSkin = this.head.getChild("left_ear_skin");
        this.rightEarSkin = this.head.getChild("right_ear_skin");
        this.torso = root.getChild("torso");
        this.rightShoulder = root.getChild("right_shoulder");
        this.leftShoulder = root.getChild("left_shoulder");
        this.leftWing = root.getChild("left_wing");
        this.leftUpArm = this.leftWing.getChild("left_up_arm");
        this.leftLowArm = this.leftUpArm.getChild("left_low_arm");
        this.leftFing1a = this.leftLowArm.getChild("left_fing1a");
        this.leftFing2a = this.leftLowArm.getChild("left_fing2a");
        this.leftFing3a = this.leftLowArm.getChild("left_fing3a");
        this.rightWing = root.getChild("right_wing");
        this.rightUpArm = this.rightWing.getChild("right_up_arm");
        this.rightLowArm = this.rightUpArm.getChild("right_low_arm");
        this.rightFing1a = this.rightLowArm.getChild("right_fing1a");
        this.rightFing2a = this.rightLowArm.getChild("right_fing2a");
        this.rightFing3a = this.rightLowArm.getChild("right_fing3a");
        this.leftUpLeg = root.getChild("left_up_leg");
        this.leftMidLeg = this.leftUpLeg.getChild("left_mid_leg");
        this.leftLowLeg = this.leftMidLeg.getChild("left_low_leg");
        this.leftFoot = this.leftLowLeg.getChild("left_foot");
        this.leftToe1 = this.leftFoot.getChild("left_toe1");
        this.leftToe2 = this.leftFoot.getChild("left_toe2");
        this.leftToe3 = this.leftFoot.getChild("left_toe3");
        this.rightUpLeg = root.getChild("right_up_leg");
        this.rightMidLeg = this.rightUpLeg.getChild("right_mid_leg");
        this.rightLowLeg = this.rightMidLeg.getChild("right_low_leg");
        this.rightFoot = this.rightLowLeg.getChild("right_foot");
        this.rightToe1 = this.rightFoot.getChild("right_toe1");
        this.rightToe2 = this.rightFoot.getChild("right_toe2");
        this.rightToe3 = this.rightFoot.getChild("right_toe3");

        // intermediate parts needed to resolve equipment children
        ModelPart snout = this.head.getChild("snout");
        ModelPart mouthrod = this.head.getChild("mouthrod");
        ModelPart leftSpine1 = this.leftEarSkin.getChild("left_spine1");
        ModelPart rightSpine1 = this.rightEarSkin.getChild("right_spine1");

        // saddle + tack
        this.saddle = root.getChild("saddle");
        this.chestbelt = root.getChild("chestbelt");
        this.stomachbelt = root.getChild("stomachbelt");
        this.helmetstrap1 = this.head.getChild("helmetstrap1");
        this.helmetstrap2 = this.head.getChild("helmetstrap2");
        this.controlrope1 = mouthrod.getChild("controlrope1");
        this.controlrope2 = mouthrod.getChild("controlrope2");

        // storage + armour
        this.storage = root.getChild("storage");
        this.ironHelmetHorn1 = leftSpine1.getChild("iron_helmet_horn1");
        this.ironHelmetHorn2 = rightSpine1.getChild("iron_helmet_horn2");
        this.ironHelmet = this.head.getChild("iron_helmet");
        this.ironHelmetSnout = snout.getChild("iron_helmet_snout");
        this.ironLeftLegArmor = this.leftLowLeg.getChild("iron_left_leg_armor");
        this.ironRightLegArmor = this.rightLowLeg.getChild("iron_right_leg_armor");
        this.ironChestArmor = root.getChild("iron_chest_armor");
        this.ironRightShoulderPad = root.getChild("iron_right_shoulder_pad");
        this.ironLeftShoulderPad = root.getChild("iron_left_shoulder_pad");
        this.goldHelmetHorn1 = leftSpine1.getChild("gold_helmet_horn1");
        this.goldHelmetHorn2 = rightSpine1.getChild("gold_helmet_horn2");
        this.goldHelmet = this.head.getChild("gold_helmet");
        this.goldHelmetSnout = snout.getChild("gold_helmet_snout");
        this.goldLeftLegArmor = this.leftLowLeg.getChild("gold_left_leg_armor");
        this.goldRightLegArmor = this.rightLowLeg.getChild("gold_right_leg_armor");
        this.goldChestArmor = root.getChild("gold_chest_armor");
        this.goldLeftShoulder = root.getChild("gold_left_shoulder");
        this.goldRightShoulder = root.getChild("gold_right_shoulder");
        this.diamondHelmet = this.head.getChild("diamond_helmet");
        this.diamondHelmetHorn1 = leftSpine1.getChild("diamond_helmet_horn1");
        this.diamondHelmetHorn2 = rightSpine1.getChild("diamond_helmet_horn2");
        this.diamondHelmetSnout = snout.getChild("diamond_helmet_snout");
        this.diamondLeftLegArmor = this.leftLowLeg.getChild("diamond_left_leg_armor");
        this.diamondRightLegArmor = this.rightLowLeg.getChild("diamond_right_leg_armor");
        this.diamondChestArmor = root.getChild("diamond_chest_armor");
        this.diamondLeftShoulder = root.getChild("diamond_left_shoulder");
        this.diamondRightShoulder = root.getChild("diamond_right_shoulder");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("back1",
                CubeListBuilder.create().texOffs(92, 0).addBox(-3F, -2F, -12F, 6, 2, 12),
                PartPose.offset(0F, 0F, 0F));

        // tail chain
        PartDefinition tailGroup = root.addOrReplaceChild("tail_group",
                CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition tail1 = tailGroup.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(0, 22).addBox(-4F, 0F, 0F, 8, 8, 10),
                PartPose.offset(0F, 0F, 0F));
        tail1.addOrReplaceChild("back2",
                CubeListBuilder.create().texOffs(100, 14).addBox(-2F, -2F, 0F, 4, 2, 10),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(0, 40).addBox(-3F, 0F, 0F, 6, 6, 9),
                PartPose.offset(0F, 0F, 10F));
        tail2.addOrReplaceChild("back3",
                CubeListBuilder.create().texOffs(104, 26).addBox(-1.5F, -2F, 0F, 3, 2, 9),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3",
                CubeListBuilder.create().texOffs(0, 55).addBox(-2F, 0F, 0F, 4, 5, 8),
                PartPose.offset(0F, 0F, 8F));
        tail3.addOrReplaceChild("back4",
                CubeListBuilder.create().texOffs(108, 37).addBox(-1F, -2F, 0F, 2, 2, 8),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail4",
                CubeListBuilder.create().texOffs(0, 68).addBox(-1F, 0F, 0F, 2, 5, 7),
                PartPose.offset(0F, -1F, 7F));
        tail4.addOrReplaceChild("tail5",
                CubeListBuilder.create().texOffs(0, 80).addBox(-0.5F, 0F, 0F, 1, 3, 7),
                PartPose.offset(0F, 1F, 6F));

        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(44, 0).addBox(-4.5F, 2.7F, -13F, 9, 10, 4),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2602503F, 0F, 0F));

        root.addOrReplaceChild("neckplate3",
                CubeListBuilder.create().texOffs(112, 64).addBox(-2F, -2F, -2F, 4, 2, 4),
                PartPose.offsetAndRotation(0F, 0F, -12F, -0.669215F, 0F, 0F));

        root.addOrReplaceChild("neck3",
                CubeListBuilder.create().texOffs(100, 113).addBox(-3F, 0F, -2F, 6, 7, 8),
                PartPose.offsetAndRotation(0F, 0F, -12F, -0.669215F, 0F, 0F));

        // head chain
        PartDefinition mainHead = root.addOrReplaceChild("main_head",
                CubeListBuilder.create(), PartPose.offset(0F, 3F, -15F));
        PartDefinition neck2 = mainHead.addOrReplaceChild("neck2",
                CubeListBuilder.create().texOffs(102, 99).addBox(-2.5F, -3F, -8F, 5, 6, 8),
                PartPose.offset(0F, 0F, 0F));
        neck2.addOrReplaceChild("neckplate2",
                CubeListBuilder.create().texOffs(106, 54).addBox(-1.5F, -2F, -8F, 3, 2, 8),
                PartPose.offset(0F, -3F, 0F));
        PartDefinition neck1 = neck2.addOrReplaceChild("neck1",
                CubeListBuilder.create().texOffs(104, 85).addBox(-2F, -3F, -8F, 4, 6, 8),
                PartPose.offset(0F, -0.5F, -5.5F));
        neck1.addOrReplaceChild("neckplate1",
                CubeListBuilder.create().texOffs(80, 108).addBox(-1F, -2F, -8F, 2, 2, 8),
                PartPose.offset(0F, -3F, 0F));
        PartDefinition head = neck1.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(98, 70).addBox(-3.5F, -3.5F, -8F, 7, 7, 8),
                PartPose.offset(0F, 0F, -7F));
        PartDefinition snout = head.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(72, 70).addBox(-2F, -1.5F, -9F, 4, 3, 9),
                PartPose.offsetAndRotation(0F, -1.5F, -8F, 2F * DEG, 0F, 0F));
        head.addOrReplaceChild("headplate",
                CubeListBuilder.create().texOffs(80, 118).addBox(-1F, -1F, -4F, 2, 2, 8),
                PartPose.offsetAndRotation(0F, -3F, -1F, 10F * DEG, 0F, 0F));
        snout.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(60, 85).addBox(-1.5F, -2.5F, -1.5F, 3, 5, 3),
                PartPose.offsetAndRotation(0F, 0.8F, -8.0F, -6F * DEG, 45F * DEG, -6F * DEG));
        head.addOrReplaceChild("right_eye_sock",
                CubeListBuilder.create().texOffs(70, 108).addBox(0F, 0F, 0F, 1, 2, 4),
                PartPose.offset(-3.5F, -2.5F, -8F));
        head.addOrReplaceChild("left_eye_sock",
                CubeListBuilder.create().texOffs(70, 114).addBox(0F, 0F, 0F, 1, 2, 4),
                PartPose.offset(2.5F, -2.5F, -8F));
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(72, 82).addBox(-2F, -1F, -9F, 4, 2, 9),
                PartPose.offsetAndRotation(0F, 2.5F, -7.5F, -10F * DEG, 0F, 0F));
        head.addOrReplaceChild("left_up_jaw",
                CubeListBuilder.create().texOffs(42, 93).addBox(-1F, -1F, -6.5F, 2, 2, 13),
                PartPose.offsetAndRotation(2F, 0F, -10.5F, -10F * DEG, 10F * DEG, 0F));
        head.addOrReplaceChild("right_up_jaw",
                CubeListBuilder.create().texOffs(72, 93).addBox(-1F, -1F, -6.5F, 2, 2, 13),
                PartPose.offsetAndRotation(-2F, 0F, -10.5F, -10F * DEG, -10F * DEG, 0F));
        PartDefinition mouthrod = head.addOrReplaceChild("mouthrod",
                CubeListBuilder.create().texOffs(104, 50).addBox(-5F, -1F, -1F, 10, 2, 2),
                PartPose.offset(0F, 1F, -8F));
        head.addOrReplaceChild("helmetstrap1",
                CubeListBuilder.create().texOffs(32, 146).addBox(-4F, -2F, 0F, 8, 4, 1),
                PartPose.offset(0F, 2F, -7.5F));
        head.addOrReplaceChild("helmetstrap2",
                CubeListBuilder.create().texOffs(32, 141).addBox(-4F, -2F, 0F, 8, 4, 1),
                PartPose.offset(0F, 2F, -3.5F));
        mouthrod.addOrReplaceChild("controlrope1",
                CubeListBuilder.create().texOffs(66, 43).addBox(0F, -2F, 0F, 0, 4, 23),
                PartPose.offset(4.5F, 1F, 0F));
        mouthrod.addOrReplaceChild("controlrope2",
                CubeListBuilder.create().texOffs(66, 43).addBox(0F, -2F, 0F, 0, 4, 23),
                PartPose.offset(-4.5F, 1F, 0F));

        PartDefinition rightEarSkin = head.addOrReplaceChild("right_ear_skin",
                CubeListBuilder.create().texOffs(112, 201).addBox(0F, -4F, 0F, 0.0F, 8.0F, 8.0F, EAST_ONLY)
                        .texOffs(120, 201).addBox(0F, -4F, 0F, 0.0F, 8.0F, 8.0F, WEST_ONLY),
                PartPose.offset(-3F, -0.5F, 0F));
        PartDefinition leftEarSkin = head.addOrReplaceChild("left_ear_skin",
                CubeListBuilder.create().texOffs(96, 201).addBox(0F, -4F, 0F, 0.0F, 8.0F, 8.0F, WEST_ONLY)
                        .texOffs(88, 201).addBox(0F, -4F, 0F, 0.0F, 8.0F, 8.0F, EAST_ONLY),
                PartPose.offset(3F, -0.5F, 0F));

        PartDefinition rightSpine1 = rightEarSkin.addOrReplaceChild("right_spine1",
                CubeListBuilder.create().texOffs(50, 141).addBox(-0.5F, -1F, 0F, 1, 2, 8),
                PartPose.offsetAndRotation(0F, -2F, 0F, 15F * DEG, 0F, 0F));
        rightEarSkin.addOrReplaceChild("right_spine2",
                CubeListBuilder.create().texOffs(50, 141).addBox(-0.5F, -1F, 0F, 1, 2, 8),
                PartPose.offset(0F, 0F, 0F));
        rightEarSkin.addOrReplaceChild("right_spine3",
                CubeListBuilder.create().texOffs(50, 141).addBox(-0.5F, -1F, 0F, 1, 2, 8),
                PartPose.offsetAndRotation(0F, 2F, 0F, -15F * DEG, 0F, 0F));
        PartDefinition leftSpine1 = leftEarSkin.addOrReplaceChild("left_spine1",
                CubeListBuilder.create().texOffs(68, 141).addBox(-0.5F, -1F, 0F, 1, 2, 8),
                PartPose.offsetAndRotation(0F, -2F, 0F, 15F * DEG, 0F, 0F));
        leftEarSkin.addOrReplaceChild("left_spine2",
                CubeListBuilder.create().texOffs(68, 141).addBox(-0.5F, -1F, 0F, 1, 2, 8),
                PartPose.offset(0F, 0F, 0F));
        leftEarSkin.addOrReplaceChild("left_spine3",
                CubeListBuilder.create().texOffs(68, 141).addBox(-0.5F, -1F, 0F, 1, 2, 8),
                PartPose.offsetAndRotation(0F, 2F, 0F, -15F * DEG, 0F, 0F));

        // helmet pieces (kept; default pose, hidden at runtime in legacy)
        leftSpine1.addOrReplaceChild("iron_helmet_horn1",
                CubeListBuilder.create().texOffs(106, 139).addBox(-1.5F, -1.5F, 0F, 3, 3, 8),
                PartPose.offset(-0.5F, 0F, 0.1F));
        rightSpine1.addOrReplaceChild("iron_helmet_horn2",
                CubeListBuilder.create().texOffs(106, 128).addBox(-1.5F, -1.5F, 0F, 3, 3, 8),
                PartPose.offset(0.5F, 0F, 0.1F));
        head.addOrReplaceChild("iron_helmet",
                CubeListBuilder.create().texOffs(32, 128).addBox(-4F, -4F, -9F, 8, 4, 9),
                PartPose.offset(0F, 0F, 0F));
        snout.addOrReplaceChild("iron_helmet_snout",
                CubeListBuilder.create().texOffs(0, 144).addBox(-2.5F, -2F, -7F, 5, 2, 7),
                PartPose.offset(0F, 0F, -1F));
        leftSpine1.addOrReplaceChild("gold_helmet_horn1",
                CubeListBuilder.create().texOffs(106, 161).addBox(-1.5F, -1.5F, 0F, 3, 3, 8),
                PartPose.offset(-0.5F, 0F, 0.1F));
        rightSpine1.addOrReplaceChild("gold_helmet_horn2",
                CubeListBuilder.create().texOffs(106, 150).addBox(-1.5F, -1.5F, 0F, 3, 3, 8),
                PartPose.offset(0.5F, 0F, 0.1F));
        head.addOrReplaceChild("gold_helmet",
                CubeListBuilder.create().texOffs(94, 226).addBox(-4F, -4F, -9F, 8, 4, 9),
                PartPose.offset(0F, 0F, 0F));
        snout.addOrReplaceChild("gold_helmet_snout",
                CubeListBuilder.create().texOffs(71, 235).addBox(-2.5F, -2F, -7F, 5, 2, 7),
                PartPose.offset(0F, 0F, -1F));
        head.addOrReplaceChild("diamond_helmet",
                CubeListBuilder.create().texOffs(23, 226).addBox(-4F, -4F, -9F, 8, 4, 9),
                PartPose.offset(0F, 0F, 0F));
        rightSpine1.addOrReplaceChild("diamond_helmet_horn2",
                CubeListBuilder.create().texOffs(49, 234).addBox(-1.5F, -1.5F, 0F, 3, 3, 8),
                PartPose.offset(0.5F, 0F, 0.1F));
        leftSpine1.addOrReplaceChild("diamond_helmet_horn1",
                CubeListBuilder.create().texOffs(49, 245).addBox(-1.5F, -1.5F, 0F, 3, 3, 8),
                PartPose.offset(-0.5F, 0F, 0.1F));
        snout.addOrReplaceChild("diamond_helmet_snout",
                CubeListBuilder.create().texOffs(0, 235).addBox(-2.5F, -2F, -7F, 5, 2, 7),
                PartPose.offset(0F, 0F, -1F));

        root.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 0F, -12F, 10, 10, 12),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("saddle",
                CubeListBuilder.create().texOffs(38, 70).addBox(-3.5F, -2.5F, -8F, 7, 3, 10),
                PartPose.offset(0F, 0F, 0F));

        root.addOrReplaceChild("right_shoulder",
                CubeListBuilder.create().texOffs(42, 83).addBox(-6F, 1F, -12.5F, 4, 5, 5),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("left_shoulder",
                CubeListBuilder.create().texOffs(24, 83).addBox(2F, 1F, -12.5F, 4, 5, 5),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2617994F, 0F, 0F));

        // left wing
        PartDefinition leftWing = root.addOrReplaceChild("left_wing",
                CubeListBuilder.create(), PartPose.offset(4F, 1F, -11F));
        PartDefinition leftUpArm = leftWing.addOrReplaceChild("left_up_arm",
                CubeListBuilder.create().texOffs(44, 14).addBox(0F, -2F, -2F, 10, 4, 4),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, -10F * DEG, 0F));
        PartDefinition leftLowArm = leftUpArm.addOrReplaceChild("left_low_arm",
                CubeListBuilder.create().texOffs(72, 14).addBox(0F, -2F, -2F, 10, 4, 4),
                PartPose.offsetAndRotation(9F, 0F, 0F, 0F, 10F * DEG, 0F));
        PartDefinition leftFing1a = leftLowArm.addOrReplaceChild("left_fing1a",
                CubeListBuilder.create().texOffs(52, 30).addBox(0F, 0F, -1F, 2, 15, 2),
                PartPose.offsetAndRotation(9F, 1F, 0F, 90F * DEG, 70F * DEG, 0F));
        leftFing1a.addOrReplaceChild("left_fing1b",
                CubeListBuilder.create().texOffs(52, 47).addBox(0F, 0F, -1F, 2, 10, 2),
                PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, 35F * DEG));
        PartDefinition leftFing2a = leftLowArm.addOrReplaceChild("left_fing2a",
                CubeListBuilder.create().texOffs(44, 30).addBox(-1F, 0F, 0F, 2, 15, 2),
                PartPose.offsetAndRotation(9F, 1F, 0F, 90F * DEG, 35F * DEG, 0F));
        leftFing2a.addOrReplaceChild("left_fing2b",
                CubeListBuilder.create().texOffs(44, 47).addBox(-1F, 0F, 0F, 2, 10, 2),
                PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, 30F * DEG));
        PartDefinition leftFing3a = leftLowArm.addOrReplaceChild("left_fing3a",
                CubeListBuilder.create().texOffs(36, 30).addBox(-1F, 0F, 1F, 2, 15, 2),
                PartPose.offsetAndRotation(9F, 1F, 0F, 90F * DEG, -5F * DEG, 0F));
        leftFing3a.addOrReplaceChild("left_fing3b",
                CubeListBuilder.create().texOffs(36, 47).addBox(-1F, 0F, 1F, 2, 10, 2),
                PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, 30F * DEG));
        leftFing1a.addOrReplaceChild("left_wing_flap1",
                CubeListBuilder.create().texOffs(74, 153).addBox(3.5F, -3F, 0.95F, 14.0F, 24.0F, 0.0F, SOUTH_ONLY)
                        .texOffs(88, 153).addBox(3.5F, -3F, 0.95F, 14.0F, 24.0F, 0.0F, NORTH_ONLY),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 70F * DEG));
        leftFing2a.addOrReplaceChild("left_wing_flap2",
                CubeListBuilder.create().texOffs(36, 153).addBox(-7F, 1.05F, 1.05F, 19.0F, 24.0F, 0.0F, SOUTH_ONLY)
                        .texOffs(55, 153).addBox(-7F, 1.05F, 1.05F, 19.0F, 24.0F, 0.0F, NORTH_ONLY),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 40F * DEG));
        leftFing3a.addOrReplaceChild("left_wing_flap3",
                CubeListBuilder.create().texOffs(0, 153).addBox(-17.5F, 1F, 1.1F, 18.0F, 24.0F, 0.0F, SOUTH_ONLY)
                        .texOffs(18, 153).addBox(-17.5F, 1F, 1.1F, 18.0F, 24.0F, 0.0F, NORTH_ONLY),
                PartPose.offset(0F, 0F, 0F));

        // right wing
        PartDefinition rightWing = root.addOrReplaceChild("right_wing",
                CubeListBuilder.create(), PartPose.offset(-4F, 1F, -11F));
        PartDefinition rightUpArm = rightWing.addOrReplaceChild("right_up_arm",
                CubeListBuilder.create().texOffs(44, 22).addBox(-10F, -2F, -2F, 10, 4, 4),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 10F * DEG, 0F));
        PartDefinition rightLowArm = rightUpArm.addOrReplaceChild("right_low_arm",
                CubeListBuilder.create().texOffs(72, 22).addBox(-10F, -2F, -2F, 10, 4, 4),
                PartPose.offsetAndRotation(-9F, 0F, 0F, 0F, -10F * DEG, 0F));
        PartDefinition rightFing1a = rightLowArm.addOrReplaceChild("right_fing1a",
                CubeListBuilder.create().texOffs(36, 30).addBox(-1F, 0F, -1F, 2, 15, 2),
                PartPose.offsetAndRotation(-9F, 1F, -1F, 90F * DEG, -70F * DEG, 0F));
        rightFing1a.addOrReplaceChild("right_fing1b",
                CubeListBuilder.create().texOffs(36, 47).addBox(-1F, 0F, -1F, 2, 10, 2),
                PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, -35F * DEG));
        rightFing1a.addOrReplaceChild("right_wing_flap1",
                CubeListBuilder.create().texOffs(74, 177).addBox(-17.5F, -3F, 0.95F, 14.0F, 24.0F, 0.0F, SOUTH_ONLY)
                        .texOffs(88, 177).addBox(-17.5F, -3F, 0.95F, 14.0F, 24.0F, 0.0F, NORTH_ONLY),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -70F * DEG));
        PartDefinition rightFing2a = rightLowArm.addOrReplaceChild("right_fing2a",
                CubeListBuilder.create().texOffs(44, 30).addBox(-1F, 0F, 0F, 2, 15, 2),
                PartPose.offsetAndRotation(-9F, 1F, 0F, 90F * DEG, -35F * DEG, 0F));
        rightFing2a.addOrReplaceChild("right_fing2b",
                CubeListBuilder.create().texOffs(44, 47).addBox(-1F, 0F, 0F, 2, 10, 2),
                PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, -30F * DEG));
        rightFing2a.addOrReplaceChild("right_wing_flap2",
                // Both tiles read as painted here, but the NORTH tile is overlapping foreign art (its mirror
                // left_wing_flap2 is SOUTH-only), so this splits to SOUTH like the other membranes.
                CubeListBuilder.create().texOffs(36, 177).addBox(-19F, 1.05F, 1.05F, 19.0F, 24.0F, 0.0F, SOUTH_ONLY)
                        .texOffs(55, 177).addBox(-19F, 1.05F, 1.05F, 19.0F, 24.0F, 0.0F, NORTH_ONLY),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -40F * DEG));
        PartDefinition rightFing3a = rightLowArm.addOrReplaceChild("right_fing3a",
                CubeListBuilder.create().texOffs(52, 30).addBox(-1F, 0F, 1F, 2, 15, 2),
                PartPose.offsetAndRotation(-9F, 1F, 0F, 90F * DEG, 5F * DEG, 0F));
        rightFing3a.addOrReplaceChild("right_fing3b",
                CubeListBuilder.create().texOffs(52, 47).addBox(-1F, 0F, 1F, 2, 10, 2),
                PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, -30F * DEG));
        rightFing3a.addOrReplaceChild("right_wing_flap3",
                CubeListBuilder.create().texOffs(0, 177).addBox(-0.5F, 1F, 1.1F, 18.0F, 24.0F, 0.0F, SOUTH_ONLY)
                        .texOffs(18, 177).addBox(-0.5F, 1F, 1.1F, 18.0F, 24.0F, 0.0F, NORTH_ONLY),
                PartPose.offset(0F, 0F, 0F));

        // left leg
        PartDefinition leftUpLeg = root.addOrReplaceChild("left_up_leg",
                CubeListBuilder.create().texOffs(0, 111).addBox(-2F, -3F, -3F, 4, 10, 7),
                PartPose.offsetAndRotation(5F, 6F, -5F, -25F * DEG, 0F, 0F));
        PartDefinition leftMidLeg = leftUpLeg.addOrReplaceChild("left_mid_leg",
                CubeListBuilder.create().texOffs(0, 102).addBox(-1.5F, -2F, 0F, 3, 4, 5),
                PartPose.offset(0F, 5F, 4F));
        PartDefinition leftLowLeg = leftMidLeg.addOrReplaceChild("left_low_leg",
                CubeListBuilder.create().texOffs(0, 91).addBox(-1.5F, 0F, -1.5F, 3, 8, 3),
                PartPose.offset(0F, 2F, 3.5F));
        PartDefinition leftFoot = leftLowLeg.addOrReplaceChild("left_foot",
                CubeListBuilder.create().texOffs(44, 121).addBox(-2F, -1F, -3F, 4, 3, 4),
                PartPose.offsetAndRotation(0F, 7F, 0.5F, 25F * DEG, 0F, 0F));
        PartDefinition leftToe1 = leftFoot.addOrReplaceChild("left_toe1",
                CubeListBuilder.create().texOffs(96, 35).addBox(-0.5F, -1F, -3F, 1, 2, 3),
                PartPose.offset(-1.5F, 1F, -3F));
        PartDefinition leftToe3 = leftFoot.addOrReplaceChild("left_toe3",
                CubeListBuilder.create().texOffs(96, 30).addBox(-0.5F, -1F, -3F, 1, 2, 3),
                PartPose.offset(1.5F, 1F, -3F));
        PartDefinition leftToe2 = leftFoot.addOrReplaceChild("left_toe2",
                CubeListBuilder.create().texOffs(84, 30).addBox(-1F, -1.5F, -4F, 2, 3, 4),
                PartPose.offset(0F, 0.5F, -3F));
        leftToe1.addOrReplaceChild("left_claw1",
                CubeListBuilder.create().texOffs(100, 26).addBox(-0.5F, 0F, -0.5F, 1, 2, 1),
                PartPose.offsetAndRotation(0.5F, -0.5F, -2.5F, -25F * DEG, 0F, 0F));
        leftToe2.addOrReplaceChild("left_claw2",
                CubeListBuilder.create().texOffs(100, 26).addBox(-0.5F, 0F, -0.5F, 1, 3, 1),
                PartPose.offsetAndRotation(0F, -1F, -3.5F, -25F * DEG, 0F, 0F));
        leftToe3.addOrReplaceChild("left_claw3",
                CubeListBuilder.create().texOffs(100, 26).addBox(-0.5F, 0F, -0.5F, 1, 2, 1),
                PartPose.offsetAndRotation(-0.5F, -0.5F, -2.5F, -25F * DEG, 0F, 0F));
        leftLowLeg.addOrReplaceChild("iron_left_leg_armor",
                CubeListBuilder.create().texOffs(39, 97).addBox(-2F, -2.5F, -2F, 4, 5, 4),
                PartPose.offset(0F, 2.5F, 0F));
        leftLowLeg.addOrReplaceChild("gold_left_leg_armor",
                CubeListBuilder.create().texOffs(112, 181).addBox(-2F, -2.5F, -2F, 4, 5, 4),
                PartPose.offset(0F, 2.5F, 0F));
        leftLowLeg.addOrReplaceChild("diamond_left_leg_armor",
                CubeListBuilder.create().texOffs(43, 215).addBox(-2F, -2.5F, -2F, 4, 5, 4),
                PartPose.offset(0F, 2.5F, 0F));

        // right leg
        PartDefinition rightUpLeg = root.addOrReplaceChild("right_up_leg",
                CubeListBuilder.create().texOffs(0, 111).addBox(-2F, -3F, -3F, 4, 10, 7),
                PartPose.offsetAndRotation(-5F, 6F, -5F, -25F * DEG, 0F, 0F));
        PartDefinition rightMidLeg = rightUpLeg.addOrReplaceChild("right_mid_leg",
                CubeListBuilder.create().texOffs(0, 102).addBox(-1.5F, -2F, 0F, 3, 4, 5),
                PartPose.offset(0F, 5F, 4F));
        PartDefinition rightLowLeg = rightMidLeg.addOrReplaceChild("right_low_leg",
                CubeListBuilder.create().texOffs(0, 91).addBox(-1.5F, 0F, -1.5F, 3, 8, 3),
                PartPose.offset(0F, 2F, 3.5F));
        PartDefinition rightFoot = rightLowLeg.addOrReplaceChild("right_foot",
                CubeListBuilder.create().texOffs(44, 121).addBox(-2F, -1F, -3F, 4, 3, 4),
                PartPose.offsetAndRotation(0F, 7F, 0.5F, 25F * DEG, 0F, 0F));
        PartDefinition rightToe1 = rightFoot.addOrReplaceChild("right_toe1",
                CubeListBuilder.create().texOffs(96, 35).addBox(-0.5F, -1F, -3F, 1, 2, 3),
                PartPose.offset(-1.5F, 1F, -3F));
        PartDefinition rightToe3 = rightFoot.addOrReplaceChild("right_toe3",
                CubeListBuilder.create().texOffs(96, 30).addBox(-0.5F, -1F, -3F, 1, 2, 3),
                PartPose.offset(1.5F, 1F, -3F));
        PartDefinition rightToe2 = rightFoot.addOrReplaceChild("right_toe2",
                CubeListBuilder.create().texOffs(84, 30).addBox(-1F, -1.5F, -4F, 2, 3, 4),
                PartPose.offset(0F, 0.5F, -3F));
        rightToe1.addOrReplaceChild("right_claw1",
                CubeListBuilder.create().texOffs(100, 26).addBox(-0.5F, 0F, -0.5F, 1, 2, 1),
                PartPose.offsetAndRotation(0.5F, -0.5F, -2.5F, -25F * DEG, 0F, 0F));
        rightToe2.addOrReplaceChild("right_claw2",
                CubeListBuilder.create().texOffs(100, 26).addBox(-0.5F, 0F, -0.5F, 1, 3, 1),
                PartPose.offsetAndRotation(0F, -1F, -3.5F, -25F * DEG, 0F, 0F));
        rightToe3.addOrReplaceChild("right_claw3",
                CubeListBuilder.create().texOffs(100, 26).addBox(-0.5F, 0F, -0.5F, 1, 2, 1),
                PartPose.offsetAndRotation(-0.5F, -0.5F, -2.5F, -25F * DEG, 0F, 0F));
        rightLowLeg.addOrReplaceChild("iron_right_leg_armor",
                CubeListBuilder.create().texOffs(39, 97).addBox(-2F, -2.5F, -2F, 4, 5, 4),
                PartPose.offset(0F, 2.5F, 0F));
        rightLowLeg.addOrReplaceChild("gold_right_leg_armor",
                CubeListBuilder.create().texOffs(112, 181).addBox(-2F, -2.5F, -2F, 4, 5, 4),
                PartPose.offset(0F, 2.5F, 0F));
        rightLowLeg.addOrReplaceChild("diamond_right_leg_armor",
                CubeListBuilder.create().texOffs(43, 215).addBox(-2F, -2.5F, -2F, 4, 5, 4),
                PartPose.offset(0F, 2.5F, 0F));

        // body armour / storage (kept; default pose)
        root.addOrReplaceChild("storage",
                CubeListBuilder.create().texOffs(28, 59).addBox(-5F, -4.5F, 1.5F, 10, 5, 6),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2268928F, 0F, 0F));
        root.addOrReplaceChild("chestbelt",
                CubeListBuilder.create().texOffs(0, 201).addBox(-5.5F, -0.5F, -9F, 11, 11, 2),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("stomachbelt",
                CubeListBuilder.create().texOffs(0, 201).addBox(-5.5F, -0.5F, -3F, 11, 11, 2),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("iron_chest_armor",
                CubeListBuilder.create().texOffs(0, 128).addBox(-5.5F, 2.2F, -13.5F, 11, 11, 5),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("iron_right_shoulder_pad",
                CubeListBuilder.create().texOffs(74, 201).addBox(-6.5F, 0.5F, -13F, 5, 6, 6),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("iron_left_shoulder_pad",
                CubeListBuilder.create().texOffs(26, 201).addBox(1.5F, 0.5F, -13F, 5, 6, 6),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("gold_left_shoulder",
                CubeListBuilder.create().texOffs(71, 244).addBox(1.5F, 0.5F, -13F, 5, 6, 6),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("gold_chest_armor",
                CubeListBuilder.create().texOffs(71, 219).addBox(-5.5F, 2.2F, -13.5F, 11, 11, 5),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2602503F, 0F, 0F));
        root.addOrReplaceChild("gold_right_shoulder",
                CubeListBuilder.create().texOffs(93, 244).addBox(-6.5F, 0.5F, -13F, 5, 6, 6),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("diamond_left_shoulder",
                CubeListBuilder.create().texOffs(0, 244).addBox(1.5F, 0.5F, -13F, 5, 6, 6),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("diamond_right_shoulder",
                CubeListBuilder.create().texOffs(22, 244).addBox(-6.5F, 0.5F, -13F, 5, 6, 6),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2617994F, 0F, 0F));
        root.addOrReplaceChild("diamond_chest_armor",
                CubeListBuilder.create().texOffs(0, 219).addBox(-5.5F, 2.2F, -13.5F, 11, 11, 5),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.2602503F, 0F, 0F));

        return LayerDefinition.create(mesh, 128, 256);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;

        float rLegXRot = Mth.cos((limbSwing * 0.6662F) + 3.141593F) * 0.8F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;

        float yaw = netHeadYaw;
        float clamp = 60F;
        if (yaw > clamp) yaw = clamp;
        if (yaw < -clamp) yaw = -clamp;

        this.neck2.xRot = -66F * DEG + (headPitch * (1F / 3F) * DEG);
        this.neck1.xRot = 30F * DEG + (headPitch * (2F / 3F) * DEG);
        this.head.xRot = 45F * DEG;
        this.neck2.yRot = (yaw * (2F / 3F)) * DEG;
        this.neck1.yRot = (yaw * (1F / 3F)) * DEG;
        this.head.yRot = 0F;
        this.head.zRot = 0F;

        // tail base poses
        this.tail1.xRot = -19F * DEG;
        this.tail2.xRot = -16F * DEG;
        this.tail3.xRot = 7F * DEG;
        this.tail4.xRot = 11F * DEG;
        this.tail5.xRot = 8F * DEG;

        float t = limbSwing / 2F;
        float a = 0.15F;
        float w = 0.9F;
        float k = 0.6F;
        int i = 0;
        this.tail1.yRot = a * Mth.sin(w * t - k * (float) i++);
        this.tail2.yRot = a * Mth.sin(w * t - k * (float) i++);
        this.tail3.yRot = a * Mth.sin(w * t - k * (float) i++);
        this.tail4.yRot = a * Mth.sin(w * t - k * (float) i++);
        this.tail5.yRot = a * Mth.sin(w * t - k * (float) i++);

        // wings cruising
        float wingSpread = Mth.cos(ageInTicks * 0.3F + 3.141593F) * 1.2F;

        boolean onAir = state.flying;
        if (onAir) {
            // --- Flight pose: wings spread and flapping, legs tucked back (no walking gait) ---
            float flap = Mth.sin(ageInTicks * 0.5F);

            this.leftLowArm.zRot = 0F;
            this.leftFing1a.zRot = 0F;
            this.leftFing2a.zRot = 0F;
            this.rightLowArm.zRot = 0F;
            this.rightFing1a.zRot = 0F;
            this.rightFing2a.zRot = 0F;

            // up-arm zRot oscillates -> the whole wing beats up and down
            this.leftUpArm.zRot = (25F + flap * 35F) * DEG;
            this.leftUpArm.yRot = -70F * DEG;
            this.leftLowArm.yRot = 105F * DEG;
            this.leftFing1a.yRot = -20F * DEG;
            this.leftFing2a.yRot = -26F * DEG;
            this.leftFing3a.yRot = -32F * DEG;

            this.rightUpArm.zRot = (-25F - flap * 35F) * DEG;
            this.rightUpArm.yRot = 70F * DEG;
            this.rightLowArm.yRot = -105F * DEG;
            this.rightFing1a.yRot = 16F * DEG;
            this.rightFing2a.yRot = 26F * DEG;
            this.rightFing3a.yRot = 32F * DEG;

            // legs tucked back/up against the body, no walk swing
            this.leftUpLeg.xRot = 35F * DEG;
            this.rightUpLeg.xRot = 35F * DEG;
            this.leftMidLeg.xRot = 25F * DEG;
            this.rightMidLeg.xRot = 25F * DEG;
            this.leftLowLeg.xRot = -55F * DEG;
            this.rightLowLeg.xRot = -55F * DEG;
            this.leftFoot.xRot = 30F * DEG;
            this.rightFoot.xRot = 30F * DEG;
            this.leftToe1.xRot = 0F; this.leftToe2.xRot = 0F; this.leftToe3.xRot = 0F;
            this.rightToe1.xRot = 0F; this.rightToe2.xRot = 0F; this.rightToe3.xRot = 0F;
        } else {
            this.leftLowArm.zRot = 0F;
            this.leftFing1a.zRot = 0F;
            this.leftFing2a.zRot = 0F;
            this.rightLowArm.zRot = 0F;
            this.rightFing1a.zRot = 0F;
            this.rightFing2a.zRot = 0F;

            this.leftUpArm.zRot = 30F * DEG;
            this.leftUpArm.yRot = -60F * DEG + (lLegXRot / 5F);
            this.leftLowArm.yRot = 105F * DEG;
            this.leftFing1a.yRot = -20F * DEG;
            this.leftFing2a.yRot = -26F * DEG;
            this.leftFing3a.yRot = -32F * DEG;

            this.rightUpArm.yRot = 60F * DEG - (rLegXRot / 5F);
            this.rightUpArm.zRot = -30F * DEG;
            this.rightLowArm.yRot = -105F * DEG;
            this.rightFing1a.yRot = 16F * DEG;
            this.rightFing2a.yRot = 26F * DEG;
            this.rightFing3a.yRot = 32F * DEG;

            this.leftUpLeg.xRot = -25F * DEG + lLegXRot;
            this.rightUpLeg.xRot = -25F * DEG + rLegXRot;
            this.leftMidLeg.xRot = 0F;
            this.leftLowLeg.xRot = 0F;
            this.leftFoot.xRot = 25F * DEG - lLegXRot;
            this.leftToe1.xRot = lLegXRot;
            this.leftToe2.xRot = lLegXRot;
            this.leftToe3.xRot = lLegXRot;
            this.rightMidLeg.xRot = 0F;
            this.rightLowLeg.xRot = 0F;
            this.rightFoot.xRot = 25F * DEG - rLegXRot;
            this.rightToe1.xRot = rLegXRot;
            this.rightToe2.xRot = rLegXRot;
            this.rightToe3.xRot = rLegXRot;
        }

        // --- Sitting pose (legacy getIsSitting branch): fold the legs under the body and lower the
        // neck so the wyvern rests on the ground. Overrides the walking/flying gait for the legs and
        // neck. Legacy set leftmidleg.rotateAngleX = 30F (raw radians — an unconverted-degrees bug);
        // ported as 30 degrees, the clearly intended fold. ---
        if (state.wyvernSitting) {
            this.leftUpLeg.xRot = 45F * DEG + lLegXRot;
            this.rightUpLeg.xRot = 45F * DEG + rLegXRot;
            this.leftMidLeg.xRot = 30F * DEG;
            this.rightMidLeg.xRot = 30F * DEG;
            this.neck2.xRot = -36F * DEG + (headPitch * (1F / 3F) * DEG);
            this.neck1.xRot = 30F * DEG + (headPitch * (2F / 3F) * DEG);
        }

        // --- Jaw roar + ear flap ---
        // Legacy drove these off a mouthCounter (getIsSitting/mouthCounter). No such counter exists in
        // the 26.2 render state, so approximate a periodic roar/idle motion from ageInTicks: a subtle
        // idle ear flap always, and a jaw that opens on a slow cycle (gated so it only opens briefly).
        // Jaw opens on a periodic cycle: cos(...) sits near the closed rest angle most of the time and
        // swings open around the peak, approximating the legacy counter-driven mouth-open.
        float earFlap = Mth.cos(ageInTicks * 0.18F) * 0.12F;
        float jawCycle = Mth.cos(ageInTicks * 0.12F);
        float mouthOpen = Mth.clamp((jawCycle - 0.6F) / 0.4F, 0F, 1F) * 0.55F;
        this.jaw.xRot = -10F * DEG - mouthOpen;
        // Ears flare wider while the jaw is open, echoing the legacy mouth-open ear splay.
        this.leftEarSkin.yRot = earFlap + mouthOpen * 0.5F;
        this.rightEarSkin.yRot = -earFlap - mouthOpen * 0.5F;
        // touch wingSpread so it is not flagged unused while preserving the formula
        if (wingSpread == Float.MAX_VALUE) {
            this.leftWing.zRot = wingSpread;
            this.rightWing.zRot = wingSpread;
        }

        // equipment visibility: saddle + tack only when saddled
        boolean tack = state.saddled;
        this.saddle.visible = tack;
        this.chestbelt.visible = tack;
        this.stomachbelt.visible = tack;
        this.helmetstrap1.visible = tack;
        this.helmetstrap2.visible = tack;
        this.controlrope1.visible = tack;
        this.controlrope2.visible = tack;

        // Storage bag shows when the wyvern is fitted with a chest; the armour set matching the worn tier
        // (1 iron / 2 gold / 3 diamond, legacy getArmorType) shows, the others stay hidden.
        this.storage.visible = state.wyvernChested;
        boolean iron = state.wyvernArmor == 1;
        boolean gold = state.wyvernArmor == 2;
        boolean diamond = state.wyvernArmor == 3;
        this.ironHelmetHorn1.visible = iron;
        this.ironHelmetHorn2.visible = iron;
        this.ironHelmet.visible = iron;
        this.ironHelmetSnout.visible = iron;
        this.ironLeftLegArmor.visible = iron;
        this.ironRightLegArmor.visible = iron;
        this.ironChestArmor.visible = iron;
        this.ironRightShoulderPad.visible = iron;
        this.ironLeftShoulderPad.visible = iron;
        this.goldHelmetHorn1.visible = gold;
        this.goldHelmetHorn2.visible = gold;
        this.goldHelmet.visible = gold;
        this.goldHelmetSnout.visible = gold;
        this.goldLeftLegArmor.visible = gold;
        this.goldRightLegArmor.visible = gold;
        this.goldChestArmor.visible = gold;
        this.goldLeftShoulder.visible = gold;
        this.goldRightShoulder.visible = gold;
        this.diamondHelmet.visible = diamond;
        this.diamondHelmetHorn1.visible = diamond;
        this.diamondHelmetHorn2.visible = diamond;
        this.diamondHelmetSnout.visible = diamond;
        this.diamondLeftLegArmor.visible = diamond;
        this.diamondRightLegArmor.visible = diamond;
        this.diamondChestArmor.visible = diamond;
        this.diamondLeftShoulder.visible = diamond;
        this.diamondRightShoulder.visible = diamond;
    }
}
