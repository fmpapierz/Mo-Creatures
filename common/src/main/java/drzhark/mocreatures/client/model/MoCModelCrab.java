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
 * Crab model, converted faithfully from the legacy {@code MoCModelCrab} ({@code ModelBase}).
 * Geometry, texture offsets and the scuttling leg gait are preserved; only the scaffolding is modern.
 */
public class MoCModelCrab extends EntityModel<MoCEntityRenderState> {

    private static final float RADIAN_F = 57.29578F;

    private final ModelPart rightArmA;
    private final ModelPart rightArmB;
    private final ModelPart leftArmA;
    private final ModelPart leftArmB;
    private final ModelPart rightLeg1A;
    private final ModelPart rightLeg1B;
    private final ModelPart rightLeg2A;
    private final ModelPart rightLeg2B;
    private final ModelPart rightLeg3A;
    private final ModelPart rightLeg3B;
    private final ModelPart rightLeg4A;
    private final ModelPart leftLeg1A;
    private final ModelPart leftLeg1B;
    private final ModelPart leftLeg2A;
    private final ModelPart leftLeg2B;
    private final ModelPart leftLeg3A;
    private final ModelPart leftLeg3B;
    private final ModelPart leftLeg4A;

    public MoCModelCrab(ModelPart root) {
        super(root);
        this.rightArmA = root.getChild("right_arm_a");
        this.rightArmB = this.rightArmA.getChild("right_arm_b");
        this.leftArmA = root.getChild("left_arm_a");
        this.leftArmB = this.leftArmA.getChild("left_arm_b");
        this.rightLeg1A = root.getChild("right_leg1_a");
        this.rightLeg1B = this.rightLeg1A.getChild("right_leg1_b");
        this.rightLeg2A = root.getChild("right_leg2_a");
        this.rightLeg2B = this.rightLeg2A.getChild("right_leg2_b");
        this.rightLeg3A = root.getChild("right_leg3_a");
        this.rightLeg3B = this.rightLeg3A.getChild("right_leg3_b");
        this.rightLeg4A = root.getChild("right_leg4_a");
        this.leftLeg1A = root.getChild("left_leg1_a");
        this.leftLeg1B = this.leftLeg1A.getChild("left_leg1_b");
        this.leftLeg2A = root.getChild("left_leg2_a");
        this.leftLeg2B = this.leftLeg2A.getChild("left_leg2_b");
        this.leftLeg3A = root.getChild("left_leg3_a");
        this.leftLeg3B = this.leftLeg3A.getChild("left_leg3_b");
        this.leftLeg4A = root.getChild("left_leg4_a");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -4.0F, 10.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("shell_right",
                CubeListBuilder.create().texOffs(0, 23).addBox(4.6F, -2.0F, -4.0F, 3.0F, 3.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.418879F));
        root.addOrReplaceChild("shell_left",
                CubeListBuilder.create().texOffs(0, 12).addBox(-7.6F, -2.0F, -4.0F, 3.0F, 3.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, -0.418879F));
        root.addOrReplaceChild("shell_back",
                CubeListBuilder.create().texOffs(10, 42).addBox(-5.0F, -1.6F, 3.6F, 10.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, -0.418879F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_eye",
                CubeListBuilder.create().texOffs(0, 4).addBox(1.0F, -2.0F, -4.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("left_eye_base",
                CubeListBuilder.create().texOffs(0, 16).addBox(1.0F, 1.0F, -5.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.2094395F));
        root.addOrReplaceChild("right_eye_base",
                CubeListBuilder.create().texOffs(0, 12).addBox(-3.0F, 1.0F, -5.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, -0.2094395F));
        root.addOrReplaceChild("right_eye",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, -0.1745329F));

        PartDefinition rightArmA = root.addOrReplaceChild("right_arm_a",
                CubeListBuilder.create().texOffs(0, 34).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, 19.0F, -4.0F, 0.0F, -0.5235988F, 0.0F));
        PartDefinition rightArmB = rightArmA.addOrReplaceChild("right_arm_b",
                CubeListBuilder.create().texOffs(22, 12).addBox(-4.0F, -1.5F, -1.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, -2.094395F, 0.0F));
        rightArmB.addOrReplaceChild("right_arm_c",
                CubeListBuilder.create().texOffs(22, 17).addBox(-3.0F, -1.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(-4.0F, 0.0F, 0.0F));
        rightArmB.addOrReplaceChild("right_arm_d",
                CubeListBuilder.create().texOffs(16, 12).addBox(-2.0F, 0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(-4.0F, 0.0F, 0.0F));

        PartDefinition leftArmA = root.addOrReplaceChild("left_arm_a",
                CubeListBuilder.create().texOffs(0, 38).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, 19.0F, -4.0F, 0.0F, 0.5235988F, 0.0F));
        PartDefinition leftArmB = leftArmA.addOrReplaceChild("left_arm_b",
                CubeListBuilder.create().texOffs(22, 20).addBox(0.0F, -1.5F, -1.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 2.094395F, 0.0F));
        leftArmB.addOrReplaceChild("left_arm_c",
                CubeListBuilder.create().texOffs(22, 25).addBox(0.0F, -1.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(4.0F, 0.0F, 0.0F));
        leftArmB.addOrReplaceChild("left_arm_d",
                CubeListBuilder.create().texOffs(16, 23).addBox(0.0F, 0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(4.0F, 0.0F, 0.0F));

        PartDefinition rightLeg1A = root.addOrReplaceChild("right_leg1_a",
                CubeListBuilder.create().texOffs(0, 42).addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-5.0F, 19.5F, -2.5F, 0.0F, -0.1745329F, -0.418879F));
        rightLeg1A.addOrReplaceChild("right_leg1_b",
                CubeListBuilder.create().texOffs(0, 48).addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));

        PartDefinition rightLeg2A = root.addOrReplaceChild("right_leg2_a",
                CubeListBuilder.create().texOffs(0, 44).addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-5.0F, 19.5F, 0.0F, 0.0F, 0.0872665F, -0.418879F));
        rightLeg2A.addOrReplaceChild("right_leg2_b",
                CubeListBuilder.create().texOffs(0, 50).addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));

        PartDefinition rightLeg3A = root.addOrReplaceChild("right_leg3_a",
                CubeListBuilder.create().texOffs(0, 46).addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-5.0F, 19.5F, 2.5F, 0.0F, 0.6981317F, -0.418879F));
        rightLeg3A.addOrReplaceChild("right_leg3_b",
                CubeListBuilder.create().texOffs(0, 52).addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));

        PartDefinition rightLeg4A = root.addOrReplaceChild("right_leg4_a",
                CubeListBuilder.create().texOffs(12, 34).addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, 19.5F, 3.5F, 0.0F, 0.6108652F, -0.418879F));
        PartDefinition rightLeg4B = rightLeg4A.addOrReplaceChild("right_leg4_b",
                CubeListBuilder.create().texOffs(12, 36).addBox(-3.0F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 1.308997F, -0.418879F));
        rightLeg4B.addOrReplaceChild("right_leg4_c",
                CubeListBuilder.create().texOffs(12, 39).mirror().addBox(-3.0F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, 0.0F, 0.0F, 0.0F, 0.8726646F, -0.418879F));

        PartDefinition leftLeg1A = root.addOrReplaceChild("left_leg1_a",
                CubeListBuilder.create().texOffs(0, 54).addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(5.0F, 19.5F, -2.5F, 0.0F, 0.1745329F, 0.418879F));
        leftLeg1A.addOrReplaceChild("left_leg1_b",
                CubeListBuilder.create().texOffs(0, 56).addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));

        PartDefinition leftLeg2A = root.addOrReplaceChild("left_leg2_a",
                CubeListBuilder.create().texOffs(0, 62).addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(5.0F, 19.5F, 0.0F, 0.0F, -0.0872665F, 0.418879F));
        leftLeg2A.addOrReplaceChild("left_leg2_b",
                CubeListBuilder.create().texOffs(10, 62).addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));

        PartDefinition leftLeg3A = root.addOrReplaceChild("left_leg3_a",
                CubeListBuilder.create().texOffs(0, 58).addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(5.0F, 19.5F, 2.5F, 0.0F, -0.6981317F, 0.418879F));
        leftLeg3A.addOrReplaceChild("left_leg3_b",
                CubeListBuilder.create().texOffs(0, 60).addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));

        PartDefinition leftLeg4A = root.addOrReplaceChild("left_leg4_a",
                CubeListBuilder.create().texOffs(22, 34).addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 19.5F, 3.5F, 0.0F, -0.6108652F, 0.418879F));
        PartDefinition leftLeg4B = leftLeg4A.addOrReplaceChild("left_leg4_b",
                CubeListBuilder.create().texOffs(22, 36).addBox(0.0F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, -1.308997F, 0.418879F));
        leftLeg4B.addOrReplaceChild("left_leg4_c",
                CubeListBuilder.create().texOffs(22, 39).addBox(0.0F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, 0.0F, 0.0F, 0.0F, -0.8726646F, 0.418879F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float f = state.walkAnimationPos;
        float f1 = state.walkAnimationSpeed;
        float f2 = state.ageInTicks;

        if (f1 < 0.1F) {
            this.rightArmA.yRot = -30.0F / RADIAN_F;
            this.rightArmB.yRot = -120.0F / RADIAN_F;

            // LHand random animation
            float lHand = 0.0F;
            float f2a = f2 % 100.0F;
            if (f2a > 0 && f2a < 10) {
                lHand = (f2a * 2.0F) / RADIAN_F;
            }
            this.leftArmA.yRot = 30.0F / RADIAN_F + lHand;
            this.leftArmB.yRot = 120.0F / RADIAN_F + lHand;

            // RHand random animation
            float rHand = 0.0F;
            float f2b = f2 % 75.0F;
            if (f2b > 30 && f2b < 40) {
                rHand = (f2b - 29) * 2.0F / RADIAN_F;
            }
            this.rightArmA.yRot = -30.0F / RADIAN_F - rHand;
            this.rightArmB.yRot = -120.0F / RADIAN_F - rHand;
        }

        // floats used for the leg animations
        float f9 = -(Mth.cos(f * 5.0F)) * f1 * 2.0F;
        float f10 = -(Mth.cos(f * 5.0F + 3.141593F)) * f1 * 2.0F;
        float f11 = -(Mth.cos(f * 5.0F + 1.570796F)) * f1 * 2.0F;
        float f12 = -(Mth.cos(f * 5.0F + 4.712389F)) * f1 * 2.0F;
        float f13 = Math.abs(Mth.sin(f * 0.6662F + 0.0F) * 0.4F) * f1 * 5.0F;
        float f14 = Math.abs(Mth.sin(f * 0.6662F + 3.141593F) * 0.4F) * f1;
        float f15 = Math.abs(Mth.sin(f * 0.6662F + 1.570796F) * 0.4F) * f1;
        float f16 = Math.abs(Mth.sin(f * 0.6662F + 4.712389F) * 0.4F) * f1;

        this.rightLeg1A.yRot = -0.1745329F + f9;
        this.rightLeg1A.zRot = -0.418879F + f13;
        this.rightLeg1B.zRot = -0.5235988F - f13;

        this.rightLeg2A.yRot = 0.0872665F + f10;
        this.rightLeg2A.zRot = -0.418879F + f14;
        this.rightLeg2B.zRot = -0.5235988F - f14;

        this.rightLeg3A.yRot = 0.6981317F + f11;
        this.rightLeg3A.zRot = -0.418879F + f15;
        this.rightLeg3B.zRot = -0.5235988F - f15;

        this.rightLeg4A.yRot = 0.6108652F + f12;
        this.rightLeg4A.zRot = -0.418879F + f16;

        this.leftLeg1A.yRot = 0.1745329F - f9;
        this.leftLeg1A.zRot = 0.418879F - f13;
        this.leftLeg1B.zRot = 0.5235988F + f13;

        this.leftLeg2A.yRot = -0.0872665F - f10;
        this.leftLeg2A.zRot = 0.418879F - f14;
        this.leftLeg2B.zRot = 0.5235988F + f14;

        this.leftLeg3A.yRot = -0.6981317F - f11;
        this.leftLeg3A.zRot = 0.418879F - f15;
        this.leftLeg3B.zRot = 0.5235988F + f15;

        this.leftLeg4A.yRot = -0.6108652F - f12;
        this.leftLeg4A.zRot = 0.418879F - f16;

        // Defensive claw-raise: a fleeing / pinching crab throws both front claws up (legacy fleeing branch,
        // LeftArmA/RightArmA rotateAngleX = -90deg).
        if (state.crabClawsUp) {
            this.rightArmA.xRot = -90.0F / RADIAN_F;
            this.leftArmA.xRot = -90.0F / RADIAN_F;
        } else {
            this.rightArmA.xRot = 0.0F;
            this.leftArmA.xRot = 0.0F;
        }
    }
}
