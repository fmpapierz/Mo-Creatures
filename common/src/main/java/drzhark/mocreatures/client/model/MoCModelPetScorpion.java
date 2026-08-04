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
 * Pet scorpion model, converted faithfully from the legacy {@code MoCModelScorpion}
 * ({@code ModelBase}). Geometry and texture offsets are preserved exactly; the leg gait
 * animation is ported into {@link #setupAnim}. Static tail/sting/arm poses use the legacy
 * "resting" (non-poisoning) rotations baked into the part definitions.
 */
public class MoCModelPetScorpion extends EntityModel<MoCEntityRenderState> {

    private static final float DEG = 57.29578F;

    private final ModelPart head;
    private final ModelPart mouthL;
    private final ModelPart mouthR;
    private final ModelPart body;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart tail4;
    private final ModelPart tail5;
    private final ModelPart sting1;
    private final ModelPart sting2;
    private final ModelPart lArm1;
    private final ModelPart lArm2;
    private final ModelPart lArm3;
    private final ModelPart lArm4;
    private final ModelPart rArm1;
    private final ModelPart rArm2;
    private final ModelPart rArm3;
    private final ModelPart rArm4;
    private final ModelPart leg1A;
    private final ModelPart leg1B;
    private final ModelPart leg1C;
    private final ModelPart leg2A;
    private final ModelPart leg2B;
    private final ModelPart leg2C;
    private final ModelPart leg3A;
    private final ModelPart leg3B;
    private final ModelPart leg3C;
    private final ModelPart leg4A;
    private final ModelPart leg4B;
    private final ModelPart leg4C;
    private final ModelPart leg5A;
    private final ModelPart leg5B;
    private final ModelPart leg5C;
    private final ModelPart leg6A;
    private final ModelPart leg6B;
    private final ModelPart leg6C;
    private final ModelPart leg7A;
    private final ModelPart leg7B;
    private final ModelPart leg7C;
    private final ModelPart leg8A;
    private final ModelPart leg8B;
    private final ModelPart leg8C;

    public MoCModelPetScorpion(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.mouthL = root.getChild("mouth_l");
        this.mouthR = root.getChild("mouth_r");
        this.body = root.getChild("body");
        this.tail1 = root.getChild("tail1");
        this.tail2 = root.getChild("tail2");
        this.tail3 = root.getChild("tail3");
        this.tail4 = root.getChild("tail4");
        this.tail5 = root.getChild("tail5");
        this.sting1 = root.getChild("sting1");
        this.sting2 = root.getChild("sting2");
        this.lArm1 = root.getChild("l_arm1");
        this.lArm2 = root.getChild("l_arm2");
        this.lArm3 = root.getChild("l_arm3");
        this.lArm4 = root.getChild("l_arm4");
        this.rArm1 = root.getChild("r_arm1");
        this.rArm2 = root.getChild("r_arm2");
        this.rArm3 = root.getChild("r_arm3");
        this.rArm4 = root.getChild("r_arm4");
        this.leg1A = root.getChild("leg1a");
        this.leg1B = root.getChild("leg1b");
        this.leg1C = root.getChild("leg1c");
        this.leg2A = root.getChild("leg2a");
        this.leg2B = root.getChild("leg2b");
        this.leg2C = root.getChild("leg2c");
        this.leg3A = root.getChild("leg3a");
        this.leg3B = root.getChild("leg3b");
        this.leg3C = root.getChild("leg3c");
        this.leg4A = root.getChild("leg4a");
        this.leg4B = root.getChild("leg4b");
        this.leg4C = root.getChild("leg4c");
        this.leg5A = root.getChild("leg5a");
        this.leg5B = root.getChild("leg5b");
        this.leg5C = root.getChild("leg5c");
        this.leg6A = root.getChild("leg6a");
        this.leg6B = root.getChild("leg6b");
        this.leg6C = root.getChild("leg6c");
        this.leg7A = root.getChild("leg7a");
        this.leg7B = root.getChild("leg7b");
        this.leg7C = root.getChild("leg7c");
        this.leg8A = root.getChild("leg8a");
        this.leg8B = root.getChild("leg8b");
        this.leg8C = root.getChild("leg8c");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 0F, 0F, 10, 5, 13),
                PartPose.offset(0F, 14F, -9F));
        root.addOrReplaceChild("mouth_l",
                CubeListBuilder.create().texOffs(18, 58).addBox(-3F, -2F, -1F, 4, 4, 2),
                PartPose.offsetAndRotation(3F, 17F, -9F, 0F, -0.3839724F, 0F));
        root.addOrReplaceChild("mouth_r",
                CubeListBuilder.create().texOffs(30, 58).addBox(-1F, -2F, -1F, 4, 4, 2),
                PartPose.offsetAndRotation(-3F, 17F, -9F, 0F, 0.3839724F, 0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 18).addBox(-4F, -2F, 0F, 8, 4, 10),
                PartPose.offsetAndRotation(0F, 17F, 3F, 0.0872665F, 0F, 0F));
        root.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(0, 32).addBox(-3F, -2F, 0F, 6, 4, 6),
                PartPose.offsetAndRotation(0F, 16F, 12F, 0.6108652F, 0F, 0F));
        root.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(0, 42).addBox(-2F, -2F, 0F, 4, 4, 6),
                PartPose.offsetAndRotation(0F, 13F, 16.5F, 1.134464F, 0F, 0F));
        root.addOrReplaceChild("tail3",
                CubeListBuilder.create().texOffs(0, 52).addBox(-1.5F, -1.5F, 0F, 3, 3, 6),
                PartPose.offsetAndRotation(0F, 8F, 18.5F, 1.692143F, 0F, 0F));
        root.addOrReplaceChild("tail4",
                CubeListBuilder.create().texOffs(24, 32).addBox(-1.5F, -1.5F, 0F, 3, 3, 6),
                PartPose.offsetAndRotation(0F, 3F, 18F, 2.510073F, 0F, 0F));
        root.addOrReplaceChild("tail5",
                CubeListBuilder.create().texOffs(24, 41).addBox(-1.5F, -1.5F, 0F, 3, 3, 6),
                PartPose.offsetAndRotation(0F, -0.2F, 14F, 3.067752F, 0F, 0F));
        root.addOrReplaceChild("sting1",
                CubeListBuilder.create().texOffs(30, 50).addBox(-1.5F, 0F, -1.5F, 3, 5, 3),
                PartPose.offsetAndRotation(0F, -1F, 7F, 0.4089647F, 0F, 0F));
        root.addOrReplaceChild("sting2",
                CubeListBuilder.create().texOffs(26, 50).addBox(-0.5F, 0F, 0.5F, 1, 4, 1),
                PartPose.offsetAndRotation(0F, 2.6F, 8.8F, -0.2230717F, 0F, 0F));

        root.addOrReplaceChild("l_arm1",
                CubeListBuilder.create().texOffs(26, 18).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 18F, -8F, -0.3490659F, 0F, 0.8726646F));
        root.addOrReplaceChild("l_arm2",
                CubeListBuilder.create().texOffs(42, 55).addBox(-1.5F, -1.5F, -6F, 3, 3, 6),
                PartPose.offsetAndRotation(10F, 14F, -6F, 0.1745329F, -0.3490659F, -0.2617994F));
        root.addOrReplaceChild("l_arm3",
                CubeListBuilder.create().texOffs(42, 39).addBox(-0.5F, -0.5F, -7F, 2, 1, 7),
                PartPose.offsetAndRotation(12F, 15F, -11F, 0.2617994F, 0.1570796F, -0.1570796F));
        root.addOrReplaceChild("l_arm4",
                CubeListBuilder.create().texOffs(42, 31).addBox(-1.5F, -0.5F, -6F, 1, 1, 7),
                PartPose.offsetAndRotation(11F, 15F, -11F, 0.2617994F, 0F, -0.1570796F));

        root.addOrReplaceChild("r_arm1",
                CubeListBuilder.create().texOffs(0, 18).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 18F, -8F, -0.3490659F, 0F, -0.8726646F));
        root.addOrReplaceChild("r_arm2",
                CubeListBuilder.create().texOffs(42, 55).addBox(-1.5F, -1.5F, -6F, 3, 3, 6),
                PartPose.offsetAndRotation(-10F, 14F, -6F, 0.1745329F, 0.3490659F, 0.2617994F));
        root.addOrReplaceChild("r_arm3",
                CubeListBuilder.create().texOffs(42, 47).addBox(-1.5F, -0.5F, -7F, 2, 1, 7),
                PartPose.offsetAndRotation(-12F, 15F, -11F, 0.2617994F, -0.1570796F, 0.1570796F));
        root.addOrReplaceChild("r_arm4",
                CubeListBuilder.create().texOffs(42, 31).addBox(0.5F, -0.5F, -6F, 1, 1, 7),
                PartPose.offsetAndRotation(-11F, 15F, -11F, 0.2617994F, 0F, 0.1570796F));

        root.addOrReplaceChild("leg1a",
                CubeListBuilder.create().texOffs(38, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 18F, -5F, -10F / DEG, 0F, 75F / DEG));
        root.addOrReplaceChild("leg1b",
                CubeListBuilder.create().texOffs(50, 0).addBox(2F, -8F, -1F, 5, 2, 2),
                PartPose.offsetAndRotation(5F, 18F, -5F, -10F / DEG, 0F, 60F / DEG));
        root.addOrReplaceChild("leg1c",
                CubeListBuilder.create().texOffs(52, 16).addBox(4.5F, -9F, -0.7F, 5, 1, 1),
                PartPose.offsetAndRotation(5F, 18F, -5F, -10F / DEG, 0F, 75F / DEG));
        root.addOrReplaceChild("leg2a",
                CubeListBuilder.create().texOffs(38, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 18F, -2F, -30F / DEG, 0F, 70F / DEG));
        root.addOrReplaceChild("leg2b",
                CubeListBuilder.create().texOffs(50, 4).addBox(1F, -8F, -1F, 5, 2, 2),
                PartPose.offsetAndRotation(5F, 18F, -2F, -30F / DEG, 0F, 60F / DEG));
        root.addOrReplaceChild("leg2c",
                CubeListBuilder.create().texOffs(50, 18).addBox(4F, -8.5F, -1F, 6, 1, 1),
                PartPose.offsetAndRotation(5F, 18F, -2F, -30F / DEG, 0F, 70F / DEG));
        root.addOrReplaceChild("leg3a",
                CubeListBuilder.create().texOffs(38, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 17.5F, 1F, -45F / DEG, 0F, 70F / DEG));
        root.addOrReplaceChild("leg3b",
                CubeListBuilder.create().texOffs(48, 8).addBox(1F, -8F, -1F, 6, 2, 2),
                PartPose.offsetAndRotation(5F, 17.5F, 1F, -45F / DEG, 0F, 60F / DEG));
        root.addOrReplaceChild("leg3c",
                CubeListBuilder.create().texOffs(50, 20).addBox(4.5F, -8.2F, -1.3F, 6, 1, 1),
                PartPose.offsetAndRotation(5F, 17.5F, 1F, -45F / DEG, 0F, 70F / DEG));
        root.addOrReplaceChild("leg4a",
                CubeListBuilder.create().texOffs(38, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 17F, 4F, -60F / DEG, 0F, 70F / DEG));
        root.addOrReplaceChild("leg4b",
                CubeListBuilder.create().texOffs(46, 12).addBox(0.5F, -8.5F, -1F, 7, 2, 2),
                PartPose.offsetAndRotation(5F, 17F, 4F, -60F / DEG, 0F, 60F / DEG));
        root.addOrReplaceChild("leg4c",
                CubeListBuilder.create().texOffs(48, 22).addBox(3.5F, -8.5F, -1.5F, 7, 1, 1),
                PartPose.offsetAndRotation(5F, 17F, 4F, -60F / DEG, 0F, 70F / DEG));

        root.addOrReplaceChild("leg5a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 18F, -5F, -10F / DEG, 0F, -75F / DEG));
        root.addOrReplaceChild("leg5b",
                CubeListBuilder.create().texOffs(50, 0).addBox(-7F, -8F, -1F, 5, 2, 2),
                PartPose.offsetAndRotation(-5F, 18F, -5F, -10F / DEG, 0F, -60F / DEG));
        root.addOrReplaceChild("leg5c",
                CubeListBuilder.create().texOffs(52, 16).addBox(-9.5F, -9F, -0.7F, 5, 1, 1),
                PartPose.offsetAndRotation(-5F, 18F, -5F, -10F / DEG, 0F, -75F / DEG));
        root.addOrReplaceChild("leg6a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 18F, -2F, -30F / DEG, 0F, -70F / DEG));
        root.addOrReplaceChild("leg6b",
                CubeListBuilder.create().texOffs(50, 4).addBox(-6F, -8F, -1F, 5, 2, 2),
                PartPose.offsetAndRotation(-5F, 18F, -2F, -30F / DEG, 0F, -60F / DEG));
        root.addOrReplaceChild("leg6c",
                CubeListBuilder.create().texOffs(50, 18).addBox(-10F, -8.5F, -1F, 6, 1, 1),
                PartPose.offsetAndRotation(-5F, 18F, -2F, -30F / DEG, 0F, -60F / DEG));
        root.addOrReplaceChild("leg7a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 17.5F, 1F, -45F / DEG, 0F, -70F / DEG));
        root.addOrReplaceChild("leg7b",
                CubeListBuilder.create().texOffs(48, 8).addBox(-7F, -8.5F, -1F, 6, 2, 2),
                PartPose.offsetAndRotation(-5F, 17.5F, 1F, -45F / DEG, 0F, -60F / DEG));
        root.addOrReplaceChild("leg7c",
                CubeListBuilder.create().texOffs(50, 20).addBox(-10.5F, -8.7F, -1.3F, 6, 1, 1),
                PartPose.offsetAndRotation(-5F, 17.5F, 1F, -45F / DEG, 0F, -70F / DEG));
        root.addOrReplaceChild("leg8a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 17F, 4F, -60F / DEG, 0F, -70F / DEG));
        root.addOrReplaceChild("leg8b",
                CubeListBuilder.create().texOffs(46, 12).addBox(-7.5F, -8.5F, -1F, 7, 2, 2),
                PartPose.offsetAndRotation(-5F, 17F, 4F, -60F / DEG, 0F, -60F / DEG));
        root.addOrReplaceChild("leg8c",
                CubeListBuilder.create().texOffs(48, 22).addBox(-10.5F, -8.5F, -1.5F, 7, 1, 1),
                PartPose.offsetAndRotation(-5F, 17F, 4F, -60F / DEG, 0F, -70F / DEG));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float f = state.walkAnimationPos;
        float f1 = state.walkAnimationSpeed;
        float headYaw = state.yRot / DEG;

        // head + mouths track the look direction
        this.head.yRot = headYaw;
        this.mouthR.yRot = (22F / DEG);
        this.mouthL.yRot = (-22F / DEG);

        // leg gait — ported faithfully from the legacy setRotationAngles
        float f9 = -(Mth.cos(f * 0.6662F * 2.0F + 0.0F) * 0.4F) * f1;
        float f10 = -(Mth.cos(f * 0.6662F * 2.0F + 3.141593F) * 0.4F) * f1;
        float f11 = -(Mth.cos(f * 0.6662F * 2.0F + 1.570796F) * 0.4F) * f1;
        float f12 = -(Mth.cos(f * 0.6662F * 2.0F + 4.712389F) * 0.4F) * f1;
        float f13 = Math.abs(Mth.sin(f * 0.6662F + 0.0F) * 0.4F) * f1;
        float f14 = Math.abs(Mth.sin(f * 0.6662F + 3.141593F) * 0.4F) * f1;
        float f15 = Math.abs(Mth.sin(f * 0.6662F + 1.570796F) * 0.4F) * f1;
        float f16 = Math.abs(Mth.sin(f * 0.6662F + 4.712389F) * 0.4F) * f1;

        this.leg1A.xRot = -10F / DEG + f9;
        this.leg1B.xRot = this.leg1A.xRot;
        this.leg1C.xRot = this.leg1A.xRot;
        this.leg1A.zRot = 75F / DEG + f13;
        this.leg1B.zRot = 60F / DEG + f13;
        this.leg1C.zRot = 75F / DEG + f13;

        this.leg2A.xRot = -30F / DEG + f10;
        this.leg2B.xRot = this.leg2A.xRot;
        this.leg2C.xRot = this.leg2A.xRot;
        this.leg2A.zRot = 70F / DEG + f14;
        this.leg2B.zRot = 60F / DEG + f14;
        this.leg2C.zRot = 70F / DEG + f14;

        this.leg3A.xRot = -45F / DEG + f11;
        this.leg3B.xRot = this.leg3A.xRot;
        this.leg3C.xRot = this.leg3A.xRot;
        this.leg3A.zRot = 70F / DEG + f15;
        this.leg3B.zRot = 60F / DEG + f15;
        this.leg3C.zRot = 70F / DEG + f15;

        this.leg4A.xRot = -60F / DEG + f12;
        this.leg4B.xRot = this.leg4A.xRot;
        this.leg4C.xRot = this.leg4A.xRot;
        this.leg4A.zRot = 70F / DEG + f16;
        this.leg4B.zRot = 60F / DEG + f16;
        this.leg4C.zRot = 70F / DEG + f16;

        this.leg5A.xRot = -10F / DEG - f9;
        this.leg5B.xRot = this.leg5A.xRot;
        this.leg5C.xRot = this.leg5A.xRot;
        this.leg5A.zRot = -75F / DEG - f13;
        this.leg5B.zRot = -60F / DEG - f13;
        this.leg5C.zRot = -75F / DEG - f13;

        this.leg6A.xRot = -30F / DEG - f10;
        this.leg6B.xRot = this.leg6A.xRot;
        this.leg6C.xRot = this.leg6A.xRot;
        this.leg6A.zRot = -70F / DEG - f14;
        this.leg6B.zRot = -60F / DEG - f14;
        this.leg6C.zRot = -70F / DEG - f14;

        this.leg7A.xRot = -45F / DEG - f11;
        this.leg7B.xRot = this.leg7A.xRot;
        this.leg7C.xRot = this.leg7A.xRot;
        this.leg7A.zRot = -70F / DEG - f15;
        this.leg7B.zRot = -60F / DEG - f15;
        this.leg7C.zRot = -70F / DEG - f15;

        this.leg8A.xRot = -60F / DEG - f12;
        this.leg8B.xRot = this.leg8A.xRot;
        this.leg8C.xRot = this.leg8A.xRot;
        this.leg8A.zRot = -70F / DEG - f16;
        this.leg8B.zRot = -60F / DEG - f16;
        this.leg8C.zRot = -70F / DEG - f16;
    }
}
