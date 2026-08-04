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
 * HellRat model, converted faithfully from the legacy {@code MoCModelRat} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; only the scaffolding is modern.
 */
public class MoCModelHellRat extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float HALF_PI = 1.570796F;

    private final ModelPart head;
    private final ModelPart earRight;
    private final ModelPart earLeft;
    private final ModelPart whiskerRight;
    private final ModelPart whiskerLeft;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart frontLeft;
    private final ModelPart frontRight;
    private final ModelPart rearLeft;
    private final ModelPart rearRight;
    private final ModelPart bodyF;

    public MoCModelHellRat(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.earRight = root.getChild("ear_right");
        this.earLeft = root.getChild("ear_left");
        this.whiskerRight = root.getChild("whisker_right");
        this.whiskerLeft = root.getChild("whisker_left");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
        this.frontLeft = root.getChild("front_left");
        this.frontRight = root.getChild("front_right");
        this.rearLeft = root.getChild("rear_left");
        this.rearRight = root.getChild("rear_right");
        this.bodyF = root.getChild("body_f");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.0F, -6.0F, 3.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 18.0F, -9.0F));
        root.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(16, 26).addBox(-3.5F, -3.0F, -2.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 18.0F, -9.0F));
        root.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(24, 26).addBox(0.5F, -3.0F, -2.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 18.0F, -9.0F));
        root.addOrReplaceChild("whisker_right",
                CubeListBuilder.create().texOffs(24, 16).addBox(-4.5F, -1.0F, -6.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 18.0F, -9.0F));
        root.addOrReplaceChild("whisker_left",
                CubeListBuilder.create().texOffs(24, 20).addBox(1.5F, -1.0F, -6.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 18.0F, -9.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(24, 0).addBox(-4.0F, -3.0F, -3.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, HALF_PI, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(56, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 19.0F, 5.0F, HALF_PI, 0.0F, 0.0F));
        root.addOrReplaceChild("front_left",
                CubeListBuilder.create().texOffs(0, 18).addBox(-2.0F, 0.0F, -3.0F, 2.0F, 1.0F, 4.0F),
                PartPose.offset(3.0F, 22.0F, -7.0F));
        root.addOrReplaceChild("front_right",
                CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, -3.0F, 2.0F, 1.0F, 4.0F),
                PartPose.offset(-3.0F, 22.0F, -7.0F));
        root.addOrReplaceChild("rear_left",
                CubeListBuilder.create().texOffs(0, 24).addBox(-2.0F, 0.0F, -4.0F, 2.0F, 1.0F, 5.0F),
                PartPose.offset(4.0F, 22.0F, 2.0F));
        root.addOrReplaceChild("rear_right",
                CubeListBuilder.create().texOffs(0, 24).addBox(0.0F, 0.0F, -4.0F, 2.0F, 1.0F, 5.0F),
                PartPose.offset(-4.0F, 22.0F, 2.0F));
        root.addOrReplaceChild("body_f",
                CubeListBuilder.create().texOffs(32, 16).addBox(-3.0F, -3.0F, -7.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 19.0F, -2.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float headPitch = state.xRot * DEG_TO_RAD;
        float headYaw = state.yRot * DEG_TO_RAD;

        this.head.xRot = -headPitch;
        this.head.yRot = headYaw;
        this.earRight.xRot = -headPitch;
        this.earRight.yRot = headYaw;
        this.earLeft.xRot = -headPitch;
        this.earLeft.yRot = headYaw;
        this.whiskerRight.xRot = -headPitch;
        this.whiskerRight.yRot = headYaw;
        this.whiskerLeft.xRot = -headPitch;
        this.whiskerLeft.yRot = headYaw;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        this.frontLeft.xRot = Mth.cos(limbSwing * 0.6662F) * 0.6F * limbAmount;
        this.rearLeft.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 0.8F * limbAmount;
        this.rearRight.xRot = Mth.cos(limbSwing * 0.6662F) * 0.6F * limbAmount;
        this.frontRight.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 0.8F * limbAmount;
        this.tail.yRot = this.frontLeft.xRot * 0.625F;
    }
}
