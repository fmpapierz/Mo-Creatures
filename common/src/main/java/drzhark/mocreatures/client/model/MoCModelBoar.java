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
 * Boar model, converted faithfully from the legacy {@code MoCModelBoar} ({@code ModelBase}).
 * Geometry, texture offsets and the four-legged trotting gait are preserved.
 */
public class MoCModelBoar extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart trout;
    private final ModelPart tusks;
    private final ModelPart jaw;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart headMane;
    private final ModelPart body;
    private final ModelPart bodyMane;
    private final ModelPart tail;
    private final ModelPart upperLegRight;
    private final ModelPart lowerLegRight;
    private final ModelPart upperLegLeft;
    private final ModelPart lowerLegLeft;
    private final ModelPart upperHindLegRight;
    private final ModelPart lowerHindLegRight;
    private final ModelPart upperHindLegLeft;
    private final ModelPart lowerHindLegLeft;

    public MoCModelBoar(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.trout = root.getChild("trout");
        this.tusks = root.getChild("tusks");
        this.jaw = root.getChild("jaw");
        this.leftEar = root.getChild("left_ear");
        this.rightEar = root.getChild("right_ear");
        this.headMane = root.getChild("head_mane");
        this.body = root.getChild("body");
        this.bodyMane = root.getChild("body_mane");
        this.tail = root.getChild("tail");
        this.upperLegRight = root.getChild("upper_leg_right");
        this.lowerLegRight = root.getChild("lower_leg_right");
        this.upperLegLeft = root.getChild("upper_leg_left");
        this.lowerLegLeft = root.getChild("lower_leg_left");
        this.upperHindLegRight = root.getChild("upper_hind_leg_right");
        this.lowerHindLegRight = root.getChild("lower_hind_leg_right");
        this.upperHindLegLeft = root.getChild("upper_hind_leg_left");
        this.lowerHindLegLeft = root.getChild("lower_hind_leg_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 6.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, -5.0F, 0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("trout",
                CubeListBuilder.create().texOffs(0, 11).addBox(-1.5F, 1.5F, -9.5F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, -5.0F, 0.3490659F, 0.0F, 0.0F));
        root.addOrReplaceChild("tusks",
                CubeListBuilder.create().texOffs(0, 24).addBox(-2.0F, 3.0F, -8.0F, 4.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, -5.0F, 0.3490659F, 0.0F, 0.0F));
        root.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 19).addBox(-1.0F, 4.9F, -8.5F, 2.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, -5.0F, 0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(16, 11).addBox(1.0F, -4.0F, -2.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, -5.0F, 0.6981317F, 0.0F, 0.3490659F));
        root.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(16, 17).addBox(-3.0F, -4.0F, -2.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, -5.0F, 0.6981317F, 0.0F, -0.3490659F));
        root.addOrReplaceChild("head_mane",
                CubeListBuilder.create().texOffs(23, 0).addBox(-1.0F, -2.0F, -5.0F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, -5.0F, 0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(24, 0).addBox(-3.5F, 0.0F, 0.0F, 7.0F, 8.0F, 13.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, -5.0F, -0.0872665F, 0.0F, 0.0F));
        root.addOrReplaceChild("body_mane",
                CubeListBuilder.create().texOffs(0, 27).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 11.3F, -4.0F, -0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(60, 38).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 13.0F, 7.5F, 0.0872665F, 0.0F, 0.0F));
        root.addOrReplaceChild("upper_leg_right",
                CubeListBuilder.create().texOffs(32, 21).addBox(-1.0F, -2.0F, -2.0F, 1.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(-3.5F, 16.0F, -2.5F, 0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("lower_leg_right",
                CubeListBuilder.create().texOffs(32, 29).addBox(-0.5F, 2.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(-3.5F, 16.0F, -2.5F));
        root.addOrReplaceChild("upper_leg_left",
                CubeListBuilder.create().texOffs(24, 21).addBox(0.0F, -2.0F, -2.0F, 1.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(3.5F, 16.0F, -2.5F, 0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("lower_leg_left",
                CubeListBuilder.create().texOffs(24, 29).addBox(-1.5F, 2.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(3.5F, 16.0F, -2.5F));
        root.addOrReplaceChild("upper_hind_leg_right",
                CubeListBuilder.create().texOffs(44, 21).addBox(-1.5F, -2.0F, -2.0F, 1.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(-3.0F, 16.0F, 5.5F, -0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("lower_hind_leg_right",
                CubeListBuilder.create().texOffs(46, 30).addBox(-1.0F, 2.0F, 0.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(-3.0F, 16.0F, 5.5F));
        root.addOrReplaceChild("upper_hind_leg_left",
                CubeListBuilder.create().texOffs(54, 21).addBox(0.5F, -2.0F, -2.0F, 1.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(3.0F, 16.0F, 5.5F, -0.2617994F, 0.0F, 0.0F));
        root.addOrReplaceChild("lower_hind_leg_left",
                CubeListBuilder.create().texOffs(56, 30).addBox(-1.0F, 2.0F, 0.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(3.0F, 16.0F, 5.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float xAngle = state.xRot * DEG_TO_RAD;
        float yAngle = state.yRot * DEG_TO_RAD;

        this.head.xRot = 0.2617994F + xAngle;
        this.head.yRot = yAngle;
        this.headMane.xRot = 0.4363323F + xAngle;
        this.headMane.yRot = yAngle;
        this.trout.xRot = 0.3490659F + xAngle;
        this.trout.yRot = yAngle;
        this.jaw.xRot = 0.2617994F + xAngle;
        this.jaw.yRot = yAngle;
        this.tusks.xRot = 0.3490659F + xAngle;
        this.tusks.yRot = yAngle;
        this.leftEar.xRot = 0.6981317F + xAngle;
        this.leftEar.yRot = yAngle;
        this.rightEar.xRot = 0.6981317F + xAngle;
        this.rightEar.yRot = yAngle;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float lLegRotX = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
        float rLegRotX = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;

        this.upperLegLeft.xRot = lLegRotX;
        this.lowerLegLeft.xRot = lLegRotX;
        this.upperHindLegRight.xRot = lLegRotX;
        this.lowerHindLegRight.xRot = lLegRotX;

        this.upperLegRight.xRot = rLegRotX;
        this.lowerLegRight.xRot = rLegRotX;
        this.upperHindLegLeft.xRot = rLegRotX;
        this.lowerHindLegLeft.xRot = rLegRotX;

        this.tail.zRot = lLegRotX * 0.2F;
    }
}
