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
 * Wild wolf model, converted faithfully from the legacy {@code MoCModelWolf} ({@code ModelBase}).
 * Geometry, texture offsets and the walking gait are preserved; only the scaffolding is modern.
 */
public class MoCModelWWolf extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = 1.0F / 57.29578F;

    private final ModelPart head;
    private final ModelPart mouthB;
    private final ModelPart nose2;
    private final ModelPart neck;
    private final ModelPart neck2;
    private final ModelPart lSide;
    private final ModelPart rSide;
    private final ModelPart nose;
    private final ModelPart mouth;
    private final ModelPart rEar;
    private final ModelPart lEar;
    private final ModelPart chest;
    private final ModelPart body;
    private final ModelPart tailA;
    private final ModelPart tailB;
    private final ModelPart tailC;
    private final ModelPart tailD;
    private final ModelPart leg1A;
    private final ModelPart leg1B;
    private final ModelPart leg1C;
    private final ModelPart leg2A;
    private final ModelPart leg2B;
    private final ModelPart leg2C;
    private final ModelPart leg3A;
    private final ModelPart leg3B;
    private final ModelPart leg3C;
    private final ModelPart leg3D;
    private final ModelPart leg4A;
    private final ModelPart leg4B;
    private final ModelPart leg4C;
    private final ModelPart leg4D;

    public MoCModelWWolf(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.mouthB = root.getChild("mouthB");
        this.nose2 = root.getChild("nose2");
        this.neck = root.getChild("neck");
        this.neck2 = root.getChild("neck2");
        this.lSide = root.getChild("lSide");
        this.rSide = root.getChild("rSide");
        this.nose = root.getChild("nose");
        this.mouth = root.getChild("mouth");
        this.rEar = root.getChild("rEar");
        this.lEar = root.getChild("lEar");
        this.chest = root.getChild("chest");
        this.body = root.getChild("body");
        this.tailA = root.getChild("tailA");
        this.tailB = root.getChild("tailB");
        this.tailC = root.getChild("tailC");
        this.tailD = root.getChild("tailD");
        this.leg1A = root.getChild("leg1A");
        this.leg1B = root.getChild("leg1B");
        this.leg1C = root.getChild("leg1C");
        this.leg2A = root.getChild("leg2A");
        this.leg2B = root.getChild("leg2B");
        this.leg2C = root.getChild("leg2C");
        this.leg3A = root.getChild("leg3A");
        this.leg3B = root.getChild("leg3B");
        this.leg3C = root.getChild("leg3C");
        this.leg3D = root.getChild("leg3D");
        this.leg4A = root.getChild("leg4A");
        this.leg4B = root.getChild("leg4B");
        this.leg4C = root.getChild("leg4C");
        this.leg4D = root.getChild("leg4D");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.0F, -6.0F, 8.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, 7.0F, -10.0F));
        root.addOrReplaceChild("mouthB",
                CubeListBuilder.create().texOffs(16, 33).addBox(-2.0F, 4.0F, -7.0F, 4.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 7.0F, -10.0F));
        root.addOrReplaceChild("nose2",
                CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, 2.0F, -12.0F, 4.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 7.0F, -10.0F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(28, 0).addBox(-3.5F, -3.0F, -7.0F, 7.0F, 8.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, -6.0F, -0.4537856F, 0.0F, 0.0F));
        root.addOrReplaceChild("neck2",
                CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -2.0F, -5.0F, 3.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 14.0F, -10.0F, -0.4537856F, 0.0F, 0.0F));
        root.addOrReplaceChild("lSide",
                CubeListBuilder.create().texOffs(28, 33).addBox(3.0F, -0.5F, -2.0F, 2.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 7.0F, -10.0F, -0.2094395F, 0.418879F, -0.0872665F));
        root.addOrReplaceChild("rSide",
                CubeListBuilder.create().texOffs(28, 45).addBox(-5.0F, -0.5F, -2.0F, 2.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 7.0F, -10.0F, -0.2094395F, -0.418879F, 0.0872665F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(44, 33).addBox(-1.5F, -1.8F, -12.4F, 3.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 7.0F, -10.0F, 0.2792527F, 0.0F, 0.0F));
        root.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(1, 34).addBox(-2.0F, 4.0F, -11.5F, 4.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 7.0F, -10.0F));
        root.addOrReplaceChild("rEar",
                CubeListBuilder.create().texOffs(22, 0).addBox(-3.5F, -7.0F, -1.5F, 3.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 7.0F, -10.0F, 0.0F, 0.0F, -0.1745329F));
        root.addOrReplaceChild("lEar",
                CubeListBuilder.create().texOffs(13, 14).addBox(0.5F, -7.0F, -1.5F, 3.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 7.0F, -10.0F, 0.0F, 0.0F, 0.1745329F));
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(20, 15).addBox(-4.0F, -11.0F, -12.0F, 8.0F, 8.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, 1.570796F, 0.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, -8.0F, -9.0F, 6.0F, 16.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 6.5F, 2.0F, 1.570796F, 0.0F, 0.0F));
        root.addOrReplaceChild("tailA",
                CubeListBuilder.create().texOffs(52, 42).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 8.5F, 9.0F, 1.064651F, 0.0F, 0.0F));
        root.addOrReplaceChild("tailB",
                CubeListBuilder.create().texOffs(48, 49).addBox(-2.0F, 3.0F, -1.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 8.5F, 9.0F, 0.7504916F, 0.0F, 0.0F));
        root.addOrReplaceChild("tailC",
                CubeListBuilder.create().texOffs(48, 59).addBox(-2.0F, 7.8F, -4.1F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 8.5F, 9.0F, 1.099557F, 0.0F, 0.0F));
        root.addOrReplaceChild("tailD",
                CubeListBuilder.create().texOffs(52, 69).addBox(-1.5F, 9.8F, -3.6F, 3.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 8.5F, 9.0F, 1.099557F, 0.0F, 0.0F));

        root.addOrReplaceChild("leg1A",
                CubeListBuilder.create().texOffs(28, 57).addBox(0.01F, -4.0F, -2.5F, 2.0F, 8.0F, 4.0F),
                PartPose.offsetAndRotation(4.0F, 12.5F, -5.5F, 0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg1B",
                CubeListBuilder.create().texOffs(28, 69).addBox(0.0F, 3.2F, 0.5F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, 12.5F, -5.5F, -0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg1C",
                CubeListBuilder.create().texOffs(28, 79).addBox(-0.5066667F, 9.5F, -2.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(4.0F, 12.5F, -5.5F));

        root.addOrReplaceChild("leg2A",
                CubeListBuilder.create().texOffs(28, 84).addBox(-2.01F, -4.0F, -2.5F, 2.0F, 8.0F, 4.0F),
                PartPose.offsetAndRotation(-4.0F, 12.5F, -5.5F, 0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg2B",
                CubeListBuilder.create().texOffs(28, 96).addBox(-2.0F, 3.2F, 0.5F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, 12.5F, -5.5F, -0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg2C",
                CubeListBuilder.create().texOffs(28, 106).addBox(-2.506667F, 9.5F, -2.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(-4.0F, 12.5F, -5.5F));

        root.addOrReplaceChild("leg3A",
                CubeListBuilder.create().texOffs(0, 64).addBox(0.0F, -3.8F, -3.5F, 2.0F, 7.0F, 5.0F),
                PartPose.offsetAndRotation(3.0F, 12.5F, 7.0F, -0.3665191F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg3B",
                CubeListBuilder.create().texOffs(0, 76).addBox(-0.1F, 1.9F, -1.8F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(3.0F, 12.5F, 7.0F, -0.7330383F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg3C",
                CubeListBuilder.create().texOffs(0, 83).addBox(0.0F, 3.2F, 0.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, 12.5F, 7.0F, -0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg3D",
                CubeListBuilder.create().texOffs(0, 93).addBox(-0.5066667F, 9.5F, -3.0F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(3.0F, 12.5F, 7.0F));

        root.addOrReplaceChild("leg4A",
                CubeListBuilder.create().texOffs(14, 64).addBox(-2.0F, -3.8F, -3.5F, 2.0F, 7.0F, 5.0F),
                PartPose.offsetAndRotation(-3.0F, 12.5F, 7.0F, -0.3665191F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg4B",
                CubeListBuilder.create().texOffs(14, 76).addBox(-1.9F, 1.9F, -1.8F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-3.0F, 12.5F, 7.0F, -0.7330383F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg4C",
                CubeListBuilder.create().texOffs(14, 83).addBox(-2.0F, 3.2F, 0.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, 12.5F, 7.0F, -0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg4D",
                CubeListBuilder.create().texOffs(14, 93).addBox(-2.506667F, 9.5F, -3.0F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(-3.0F, 12.5F, 7.0F));

        return LayerDefinition.create(mesh, 64, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;

        this.head.xRot = headPitch * DEG_TO_RAD;
        this.head.yRot = netHeadYaw * DEG_TO_RAD;
        float lLegX = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
        float rLegX = Mth.cos((limbSwing * 0.6662F) + 3.141593F) * 1.4F * limbAmount;

        this.mouth.xRot = this.head.xRot;
        this.mouth.yRot = this.head.yRot;
        this.mouthB.xRot = this.head.xRot;
        this.mouthB.yRot = this.head.yRot;
        this.nose.xRot = (16 * DEG_TO_RAD) + this.head.xRot;
        this.nose.yRot = this.head.yRot;
        this.nose2.xRot = this.head.xRot;
        this.nose2.yRot = this.head.yRot;

        this.lSide.xRot = (-12 * DEG_TO_RAD) + this.head.xRot;
        this.lSide.yRot = (24 * DEG_TO_RAD) + this.head.yRot;
        this.rSide.xRot = (-12 * DEG_TO_RAD) + this.head.xRot;
        this.rSide.yRot = (-24 * DEG_TO_RAD) + this.head.yRot;

        this.rEar.xRot = this.head.xRot;
        this.rEar.yRot = this.head.yRot;
        this.lEar.xRot = this.head.xRot;
        this.lEar.yRot = this.head.yRot;

        this.leg1A.xRot = (15 * DEG_TO_RAD) + lLegX;
        this.leg1B.xRot = (-10 * DEG_TO_RAD) + lLegX;
        this.leg1C.xRot = lLegX;

        this.leg2A.xRot = (15 * DEG_TO_RAD) + rLegX;
        this.leg2B.xRot = (-10 * DEG_TO_RAD) + rLegX;
        this.leg2C.xRot = rLegX;

        this.leg3A.xRot = (-21 * DEG_TO_RAD) + rLegX;
        this.leg3B.xRot = (-42 * DEG_TO_RAD) + rLegX;
        this.leg3C.xRot = (-10 * DEG_TO_RAD) + rLegX;
        this.leg3D.xRot = rLegX;

        this.leg4A.xRot = (-21 * DEG_TO_RAD) + lLegX;
        this.leg4B.xRot = (-42 * DEG_TO_RAD) + lLegX;
        this.leg4C.xRot = (-10 * DEG_TO_RAD) + lLegX;
        this.leg4D.xRot = lLegX;

        float tailMov = -1.3089F + (limbAmount * 1.5F);
        this.tailA.yRot = 0.0F;
        this.tailB.yRot = this.tailA.yRot;
        this.tailC.yRot = this.tailA.yRot;
        this.tailD.yRot = this.tailA.yRot;

        this.tailA.xRot = (61 / 57.29F) - tailMov;
        this.tailB.xRot = (43 / 57.29F) - tailMov;
        this.tailC.xRot = (63 / 57.29F) - tailMov;
        this.tailD.xRot = (63 / 57.29F) - tailMov;
    }
}
