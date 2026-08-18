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
 * Werewolf model, converted faithfully from the legacy {@code MoCModelWere} ({@code ModelBase}).
 * Geometry, texture offsets and the walking/arm gait are preserved (non-hunched pose); only the
 * scaffolding is modern.
 */
public class MoCModelWerewolf extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart snout;
    private final ModelPart teethU;
    private final ModelPart teethL;
    private final ModelPart mouth;
    private final ModelPart lEar;
    private final ModelPart rEar;
    private final ModelPart neck;
    private final ModelPart neck2;
    private final ModelPart sideburnL;
    private final ModelPart sideburnR;
    private final ModelPart chest;
    private final ModelPart abdomen;
    private final ModelPart tailA;
    private final ModelPart tailB;
    private final ModelPart tailC;
    private final ModelPart tailD;
    private final ModelPart rLegA;
    private final ModelPart rLegB;
    private final ModelPart rLegC;
    private final ModelPart rFoot;
    private final ModelPart lLegA;
    private final ModelPart lLegB;
    private final ModelPart lLegC;
    private final ModelPart lFoot;
    private final ModelPart rArmA;
    private final ModelPart rArmB;
    private final ModelPart rArmC;
    private final ModelPart rHand;
    private final ModelPart lArmA;
    private final ModelPart lArmB;
    private final ModelPart lArmC;
    private final ModelPart lHand;
    private final ModelPart rFinger1;
    private final ModelPart rFinger2;
    private final ModelPart rFinger3;
    private final ModelPart rFinger4;
    private final ModelPart rFinger5;
    private final ModelPart lFinger1;
    private final ModelPart lFinger2;
    private final ModelPart lFinger3;
    private final ModelPart lFinger4;
    private final ModelPart lFinger5;

    public MoCModelWerewolf(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.nose = root.getChild("nose");
        this.snout = root.getChild("snout");
        this.teethU = root.getChild("teeth_u");
        this.teethL = root.getChild("teeth_l");
        this.mouth = root.getChild("mouth");
        this.lEar = root.getChild("ear_left");
        this.rEar = root.getChild("ear_right");
        this.neck = root.getChild("neck");
        this.neck2 = root.getChild("neck2");
        this.sideburnL = root.getChild("sideburn_left");
        this.sideburnR = root.getChild("sideburn_right");
        this.chest = root.getChild("chest");
        this.abdomen = root.getChild("abdomen");
        this.tailA = root.getChild("tail_a");
        this.tailB = root.getChild("tail_b");
        this.tailC = root.getChild("tail_c");
        this.tailD = root.getChild("tail_d");
        this.rLegA = root.getChild("leg_right_a");
        this.rLegB = root.getChild("leg_right_b");
        this.rLegC = root.getChild("leg_right_c");
        this.rFoot = root.getChild("foot_right");
        this.lLegA = root.getChild("leg_left_a");
        this.lLegB = root.getChild("leg_left_b");
        this.lLegC = root.getChild("leg_left_c");
        this.lFoot = root.getChild("foot_left");
        this.rArmA = root.getChild("arm_right_a");
        this.rArmB = root.getChild("arm_right_b");
        this.rArmC = root.getChild("arm_right_c");
        this.rHand = root.getChild("hand_right");
        this.lArmA = root.getChild("arm_left_a");
        this.lArmB = root.getChild("arm_left_b");
        this.lArmC = root.getChild("arm_left_c");
        this.lHand = root.getChild("hand_left");
        this.rFinger1 = root.getChild("finger_right_1");
        this.rFinger2 = root.getChild("finger_right_2");
        this.rFinger3 = root.getChild("finger_right_3");
        this.rFinger4 = root.getChild("finger_right_4");
        this.rFinger5 = root.getChild("finger_right_5");
        this.lFinger1 = root.getChild("finger_left_1");
        this.lFinger2 = root.getChild("finger_left_2");
        this.lFinger3 = root.getChild("finger_left_3");
        this.lFinger4 = root.getChild("finger_left_4");
        this.lFinger5 = root.getChild("finger_left_5");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.0F, -6.0F, 8.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, -8.0F, -6.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(44, 33).addBox(-1.5F, -1.7F, -12.3F, 3.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -6.0F, 0.2792527F, 0.0F, 0.0F));
        root.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, 2.0F, -12.0F, 4.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, -8.0F, -6.0F));
        root.addOrReplaceChild("teeth_u",
                CubeListBuilder.create().texOffs(46, 18).addBox(-2.0F, 4.01F, -12.0F, 4.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, -8.0F, -6.0F));
        root.addOrReplaceChild("teeth_l",
                CubeListBuilder.create().texOffs(20, 109).addBox(-1.5F, -12.5F, 2.01F, 3.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -6.0F, 2.530727F, 0.0F, 0.0F));
        root.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(42, 69).addBox(-1.5F, -12.5F, 0.0F, 3.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -6.0F, 2.530727F, 0.0F, 0.0F));
        root.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(13, 14).addBox(0.5F, -7.5F, -1.0F, 3.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -6.0F, 0.0F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(22, 0).addBox(-3.5F, -7.5F, -1.0F, 3.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -6.0F, 0.0F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(28, 0).addBox(-3.5F, -3.0F, -7.0F, 7.0F, 8.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -5.0F, -2.0F, -0.6025001F, 0.0F, 0.0F));
        root.addOrReplaceChild("neck2",
                CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -2.0F, -5.0F, 3.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -6.0F, -0.4537856F, 0.0F, 0.0F));
        root.addOrReplaceChild("sideburn_left",
                CubeListBuilder.create().texOffs(28, 33).addBox(3.0F, 0.0F, -2.0F, 2.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -6.0F, -0.2094395F, 0.418879F, -0.0872665F));
        root.addOrReplaceChild("sideburn_right",
                CubeListBuilder.create().texOffs(28, 45).addBox(-5.0F, 0.0F, -2.0F, 2.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -6.0F, -0.2094395F, -0.418879F, 0.0872665F));
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(20, 15).addBox(-4.0F, 0.0F, -7.0F, 8.0F, 8.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, -2.5F, 0.641331F, 0.0F, 0.0F));
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, -8.0F, -8.0F, 6.0F, 14.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 4.5F, 5.0F, 0.2695449F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_a",
                CubeListBuilder.create().texOffs(52, 42).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 9.5F, 6.0F, 1.064651F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_c",
                CubeListBuilder.create().texOffs(48, 59).addBox(-2.0F, 6.8F, -4.6F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 9.5F, 6.0F, 1.099557F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_b",
                CubeListBuilder.create().texOffs(48, 49).addBox(-2.0F, 2.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 9.5F, 6.0F, 0.7504916F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_d",
                CubeListBuilder.create().texOffs(52, 69).addBox(-1.5F, 9.8F, -4.1F, 3.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 9.5F, 6.0F, 1.099557F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_right_a",
                CubeListBuilder.create().texOffs(12, 64).addBox(-2.5F, -1.5F, -3.5F, 3.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(-3.0F, 9.5F, 3.0F, -0.8126625F, 0.0F, 0.0F));
        root.addOrReplaceChild("foot_right",
                CubeListBuilder.create().texOffs(14, 93).addBox(-2.506667F, 12.5F, -5.0F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(-3.0F, 9.5F, 3.0F));
        root.addOrReplaceChild("leg_right_b",
                CubeListBuilder.create().texOffs(14, 76).addBox(-1.9F, 4.2F, 0.5F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-3.0F, 9.5F, 3.0F, -0.8445741F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_right_c",
                CubeListBuilder.create().texOffs(14, 83).addBox(-2.0F, 6.2F, 0.5F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, 9.5F, 3.0F, -0.2860688F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_left_b",
                CubeListBuilder.create().texOffs(0, 76).addBox(-0.1F, 4.2F, 0.5F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(3.0F, 9.5F, 3.0F, -0.8445741F, 0.0F, 0.0F));
        root.addOrReplaceChild("foot_left",
                CubeListBuilder.create().texOffs(0, 93).addBox(-0.5066667F, 12.5F, -5.0F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(3.0F, 9.5F, 3.0F));
        root.addOrReplaceChild("leg_left_c",
                CubeListBuilder.create().texOffs(0, 83).addBox(0.0F, 6.2F, 0.5F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, 9.5F, 3.0F, -0.2860688F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_left_a",
                CubeListBuilder.create().texOffs(0, 64).addBox(-0.5F, -1.5F, -3.5F, 3.0F, 8.0F, 5.0F),
                PartPose.offsetAndRotation(3.0F, 9.5F, 3.0F, -0.8126625F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_right_b",
                CubeListBuilder.create().texOffs(48, 77).addBox(-3.5F, 1.0F, -1.5F, 4.0F, 8.0F, 4.0F),
                PartPose.offsetAndRotation(-4.0F, -4.0F, -2.0F, 0.2617994F, 0.0F, 0.3490659F));
        root.addOrReplaceChild("arm_right_c",
                CubeListBuilder.create().texOffs(48, 112).addBox(-6.0F, 5.0F, 3.0F, 4.0F, 7.0F, 4.0F),
                PartPose.offsetAndRotation(-4.0F, -4.0F, -2.0F, -0.3490659F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_left_b",
                CubeListBuilder.create().texOffs(48, 89).addBox(-0.5F, 1.0F, -1.5F, 4.0F, 8.0F, 4.0F),
                PartPose.offsetAndRotation(4.0F, -4.0F, -2.0F, 0.2617994F, 0.0F, -0.3490659F));
        root.addOrReplaceChild("hand_right",
                CubeListBuilder.create().texOffs(32, 118).addBox(-6.0F, 12.5F, -1.5F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(-4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("arm_right_a",
                CubeListBuilder.create().texOffs(0, 108).addBox(-5.0F, -3.0F, -2.0F, 5.0F, 5.0F, 5.0F),
                PartPose.offsetAndRotation(-4.0F, -4.0F, -2.0F, 0.6320364F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_left_a",
                CubeListBuilder.create().texOffs(0, 98).addBox(0.0F, -3.0F, -2.0F, 5.0F, 5.0F, 5.0F),
                PartPose.offsetAndRotation(4.0F, -4.0F, -2.0F, 0.6320364F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_left_c",
                CubeListBuilder.create().texOffs(48, 101).addBox(2.0F, 5.0F, 3.0F, 4.0F, 7.0F, 4.0F),
                PartPose.offsetAndRotation(4.0F, -4.0F, -2.0F, -0.3490659F, 0.0F, 0.0F));
        root.addOrReplaceChild("hand_left",
                CubeListBuilder.create().texOffs(32, 111).addBox(2.0F, 12.5F, -1.5F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(4.0F, -4.0F, -2.0F));
        // legacy RFinger1 is declared twice; only the second declaration survives
        root.addOrReplaceChild("finger_right_1",
                CubeListBuilder.create().texOffs(8, 120).addBox(-3.0F, 15.5F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("finger_right_2",
                CubeListBuilder.create().texOffs(12, 124).addBox(-3.5F, 15.5F, -1.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("finger_right_3",
                CubeListBuilder.create().texOffs(12, 119).addBox(-4.8F, 15.5F, -1.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(-4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("finger_right_4",
                CubeListBuilder.create().texOffs(16, 119).addBox(-6.0F, 15.5F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(-4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("finger_right_5",
                CubeListBuilder.create().texOffs(16, 124).addBox(-6.0F, 15.5F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("finger_left_1",
                CubeListBuilder.create().texOffs(8, 124).addBox(2.0F, 15.5F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("finger_left_2",
                CubeListBuilder.create().texOffs(0, 124).addBox(2.5F, 15.5F, -1.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("finger_left_3",
                CubeListBuilder.create().texOffs(0, 119).addBox(3.8F, 15.5F, -1.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("finger_left_4",
                CubeListBuilder.create().texOffs(4, 119).addBox(5.0F, 15.5F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(4.0F, -4.0F, -2.0F));
        root.addOrReplaceChild("finger_left_5",
                CubeListBuilder.create().texOffs(4, 124).addBox(5.0F, 15.5F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(4.0F, -4.0F, -2.0F));

        return LayerDefinition.create(mesh, 64, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float headYaw = state.yRot * DEG_TO_RAD;
        float headPitch = state.xRot * DEG_TO_RAD;

        float rLegXRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 0.8F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;

        // head + facial parts track the look direction (non-hunched pose)
        this.head.yRot = headYaw;
        this.head.xRot = headPitch;

        this.nose.yRot = this.head.yRot;
        this.snout.yRot = this.head.yRot;
        this.teethU.yRot = this.head.yRot;
        this.lEar.yRot = this.head.yRot;
        this.rEar.yRot = this.head.yRot;
        this.teethL.yRot = this.head.yRot;
        this.mouth.yRot = this.head.yRot;

        this.teethL.xRot = this.head.xRot + 2.530727F;
        this.mouth.xRot = this.head.xRot + 2.530727F;

        this.sideburnL.xRot = -0.2094395F + this.head.xRot;
        this.sideburnL.yRot = 0.418879F + this.head.yRot;
        this.sideburnR.xRot = -0.2094395F + this.head.xRot;
        this.sideburnR.yRot = -0.418879F + this.head.yRot;

        this.nose.xRot = 0.2792527F + this.head.xRot;
        this.snout.xRot = this.head.xRot;
        this.teethU.xRot = this.head.xRot;

        this.lEar.xRot = this.head.xRot;
        this.rEar.xRot = this.head.xRot;

        this.rLegA.xRot = -0.8126625F + rLegXRot;
        this.rLegB.xRot = -0.8445741F + rLegXRot;
        this.rLegC.xRot = -0.2860688F + rLegXRot;
        this.rFoot.xRot = rLegXRot;

        this.lLegA.xRot = -0.8126625F + lLegXRot;
        this.lLegB.xRot = -0.8445741F + lLegXRot;
        this.lLegC.xRot = -0.2860688F + lLegXRot;
        this.lFoot.xRot = lLegXRot;

        this.rArmA.zRot = -(Mth.cos(ageInTicks * 0.09F) * 0.05F) + 0.05F;
        this.lArmA.zRot = (Mth.cos(ageInTicks * 0.09F) * 0.05F) - 0.05F;
        this.rArmA.xRot = lLegXRot;
        this.lArmA.xRot = rLegXRot;

        this.rArmB.zRot = 0.3490659F + this.rArmA.zRot;
        this.lArmB.zRot = -0.3490659F + this.lArmA.zRot;
        this.rArmB.xRot = 0.2617994F + this.rArmA.xRot;
        this.lArmB.xRot = 0.2617994F + this.lArmA.xRot;

        this.rArmC.zRot = this.rArmA.zRot;
        this.lArmC.zRot = this.lArmA.zRot;
        this.rArmC.xRot = -0.3490659F + this.rArmA.xRot;
        this.lArmC.xRot = -0.3490659F + this.lArmA.xRot;

        this.rHand.zRot = this.rArmA.zRot;
        this.lHand.zRot = this.lArmA.zRot;
        this.rHand.xRot = this.rArmA.xRot;
        this.lHand.xRot = this.lArmA.xRot;

        this.rFinger1.xRot = this.rArmA.xRot;
        this.rFinger2.xRot = this.rArmA.xRot;
        this.rFinger3.xRot = this.rArmA.xRot;
        this.rFinger4.xRot = this.rArmA.xRot;
        this.rFinger5.xRot = this.rArmA.xRot;

        this.lFinger1.xRot = this.lArmA.xRot;
        this.lFinger2.xRot = this.lArmA.xRot;
        this.lFinger3.xRot = this.lArmA.xRot;
        this.lFinger4.xRot = this.lArmA.xRot;
        this.lFinger5.xRot = this.lArmA.xRot;

        this.rFinger1.zRot = this.rArmA.zRot;
        this.rFinger2.zRot = this.rArmA.zRot;
        this.rFinger3.zRot = this.rArmA.zRot;
        this.rFinger4.zRot = this.rArmA.zRot;
        this.rFinger5.zRot = this.rArmA.zRot;

        this.lFinger1.zRot = this.lArmA.zRot;
        this.lFinger2.zRot = this.lArmA.zRot;
        this.lFinger3.zRot = this.lArmA.zRot;
        this.lFinger4.zRot = this.lArmA.zRot;
        this.lFinger5.zRot = this.lArmA.zRot;

        if (state.werewolfHunched) {
            // Aggressive hunched-charge pose (legacy hunched beast): the torso pitches forward over its
            // prey, the neck cranes low, the head thrusts down, and the arms reach toward the ground as
            // the beast stalks. Overrides the upright standing pose.
            this.chest.xRot = 1.0472F;   // torso leans forward ~60°
            this.abdomen.xRot = 0.5F;    // lower body folds up under the lean
            this.neck.xRot = -0.1F;      // crane the neck forward and down
            this.neck2.xRot = 0.05F;

            this.head.xRot = headPitch + 0.45F; // head driven down and forward
            this.nose.xRot = 0.2792527F + this.head.xRot;
            this.snout.xRot = this.head.xRot;
            this.teethU.xRot = this.head.xRot;
            this.lEar.xRot = this.head.xRot;
            this.rEar.xRot = this.head.xRot;
            this.teethL.xRot = this.head.xRot + 2.530727F;
            this.mouth.xRot = this.head.xRot + 2.530727F;
            this.sideburnL.xRot = -0.2094395F + this.head.xRot;
            this.sideburnR.xRot = -0.2094395F + this.head.xRot;

            // Arms swing down to the ground for the four-point stalk, still gaiting with the run.
            float armReach = 1.15F + (rLegXRot * 0.5F);
            this.rArmA.xRot = armReach;
            this.lArmA.xRot = armReach;
            this.rArmA.zRot = 0.05F;
            this.lArmA.zRot = -0.05F;
            this.rArmB.xRot = 0.2617994F + this.rArmA.xRot;
            this.lArmB.xRot = 0.2617994F + this.lArmA.xRot;
            this.rArmB.zRot = 0.3490659F + this.rArmA.zRot;
            this.lArmB.zRot = -0.3490659F + this.lArmA.zRot;
            this.rArmC.xRot = -0.3490659F + this.rArmA.xRot;
            this.lArmC.xRot = -0.3490659F + this.lArmA.xRot;
            this.rArmC.zRot = this.rArmA.zRot;
            this.lArmC.zRot = this.lArmA.zRot;
            this.rHand.xRot = this.rArmA.xRot;
            this.lHand.xRot = this.lArmA.xRot;
            this.rHand.zRot = this.rArmA.zRot;
            this.lHand.zRot = this.lArmA.zRot;

            this.rFinger1.xRot = this.rArmA.xRot;
            this.rFinger2.xRot = this.rArmA.xRot;
            this.rFinger3.xRot = this.rArmA.xRot;
            this.rFinger4.xRot = this.rArmA.xRot;
            this.rFinger5.xRot = this.rArmA.xRot;
            this.lFinger1.xRot = this.lArmA.xRot;
            this.lFinger2.xRot = this.lArmA.xRot;
            this.lFinger3.xRot = this.lArmA.xRot;
            this.lFinger4.xRot = this.lArmA.xRot;
            this.lFinger5.xRot = this.lArmA.xRot;
        }
    }
}
