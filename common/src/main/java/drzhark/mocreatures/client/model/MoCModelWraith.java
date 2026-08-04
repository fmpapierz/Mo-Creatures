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
 * Wraith model, converted faithfully from the legacy {@code MoCModelWraith} ({@code ModelBiped}).
 * Geometry and texture offsets are preserved. The raised-arm base pose plus the ghostly float drift
 * are blended with the legacy attack swing-progress component (from {@code state.attackSwing}), so the
 * wraith spreads and thrusts its arms forward as it lunges at prey.
 */
public class MoCModelWraith extends EntityModel<MoCEntityRenderState> {

    private static final float HALF_PI = 1.570796F;

    private final ModelPart head;
    private final ModelPart headwear;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public MoCModelWraith(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucent);
        this.head = root.getChild("head");
        this.headwear = root.getChild("headwear");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 40).addBox(-4.0F, -8.0F, -4.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("headwear",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(36, 0).addBox(-6.0F, 0.0F, -2.0F, 10.0F, 20.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(16, 16).addBox(-5.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().mirror().texOffs(16, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float f2 = state.ageInTicks;

        // Attack swing progress (0..1): drives the lunge — the arms spread out and thrust forward mid-swing.
        float swing = state.attackSwing;
        float f6 = Mth.sin(swing * (float) Math.PI);
        float f7 = Mth.sin((1.0F - ((1.0F - swing) * (1.0F - swing))) * (float) Math.PI);

        // legacy base arm pose: arms raised forward, spread apart as the wraith lunges
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightArm.yRot = -(0.1F - f6 * 0.6F);
        this.leftArm.yRot = 0.1F - f6 * 0.6F;
        this.rightArm.xRot = -HALF_PI - (f6 * 1.2F - f7 * 0.4F);
        this.leftArm.xRot = -HALF_PI - (f6 * 1.2F - f7 * 0.4F);

        // float-driven ghostly drift (preserved from legacy)
        this.rightArm.zRot += (Mth.cos(f2 * 0.09F) * 0.05F) + 0.05F;
        this.leftArm.zRot -= (Mth.cos(f2 * 0.09F) * 0.05F) + 0.05F;
        this.rightArm.xRot += Mth.sin(f2 * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(f2 * 0.067F) * 0.05F;
    }
}
