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
 * Bunny model, converted faithfully from the legacy {@code MoCModelBunny} ({@code ModelBase}).
 * Geometry, texture offsets and the hopping leg gait are preserved; only the scaffolding is modern.
 */
public class MoCModelBunny extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float HALF_PI = (float) (Math.PI / 2.0);

    private final ModelPart head;
    private final ModelPart earLeft;
    private final ModelPart earRight;
    private final ModelPart cheekLeft;
    private final ModelPart cheekRight;
    private final ModelPart frontLegRight;
    private final ModelPart frontLegLeft;
    private final ModelPart backLegRight;
    private final ModelPart backLegLeft;

    public MoCModelBunny(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.earLeft = root.getChild("ear_left");
        this.earRight = root.getChild("ear_right");
        this.cheekLeft = root.getChild("cheek_left");
        this.cheekRight = root.getChild("cheek_right");
        this.frontLegRight = root.getChild("front_leg_right");
        this.frontLegLeft = root.getChild("front_leg_left");
        this.backLegRight = root.getChild("back_leg_right");
        this.backLegLeft = root.getChild("back_leg_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // legacy used a y-offset of byte0 = 16 added to every setRotationPoint y
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 15.0F, -4.0F));
        root.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(14, 0).addBox(-2.0F, -5.0F, -3.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 15.0F, -4.0F));
        root.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(14, 0).addBox(1.0F, -5.0F, -3.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 15.0F, -4.0F));
        root.addOrReplaceChild("cheek_left",
                CubeListBuilder.create().texOffs(20, 0).addBox(-4.0F, 0.0F, -3.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 15.0F, -4.0F));
        root.addOrReplaceChild("cheek_right",
                CubeListBuilder.create().texOffs(20, 0).addBox(2.0F, 0.0F, -3.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 15.0F, -4.0F));
        // body and tail carry a fixed pi/2 pitch (legacy set this every frame)
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 10).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, HALF_PI, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 24).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, HALF_PI, 0.0F, 0.0F));
        root.addOrReplaceChild("front_leg_right",
                CubeListBuilder.create().texOffs(24, 16).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(3.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("front_leg_left",
                CubeListBuilder.create().texOffs(24, 16).addBox(0.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-3.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("back_leg_right",
                CubeListBuilder.create().texOffs(16, 24).addBox(-2.0F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(3.0F, 19.0F, 4.0F));
        root.addOrReplaceChild("back_leg_left",
                CubeListBuilder.create().texOffs(16, 24).addBox(0.0F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(-3.0F, 19.0F, 4.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float headPitch = state.xRot * DEG_TO_RAD;
        float headYaw = state.yRot * DEG_TO_RAD;

        // head + ears + cheeks track the look direction (legacy negates the pitch)
        this.head.xRot = -headPitch;
        this.head.yRot = headYaw;
        this.earLeft.xRot = -headPitch;
        this.earLeft.yRot = headYaw;
        this.earRight.xRot = -headPitch;
        this.earRight.yRot = headYaw;
        this.cheekLeft.xRot = -headPitch;
        this.cheekLeft.yRot = headYaw;
        this.cheekRight.xRot = -headPitch;
        this.cheekRight.yRot = headYaw;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        // hopping gait: both front legs swing together, both back legs swing together (opposite phase)
        this.frontLegRight.xRot = Mth.cos(limbSwing * 0.6662F) * 1.0F * limbAmount;
        this.frontLegLeft.xRot = Mth.cos(limbSwing * 0.6662F) * 1.0F * limbAmount;
        this.backLegRight.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.2F * limbAmount;
        this.backLegLeft.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.2F * limbAmount;
    }
}
