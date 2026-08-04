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
 * Geometry and texture offsets are preserved; the floating-arm animation is simplified to the
 * fixed raised-arm pose with a gentle idle sway, since the legacy gait used a biped-internal field.
 */
public class MoCModelFlameWraith extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart headwear;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public MoCModelFlameWraith(ModelPart root) {
        // Opaque (default cutout) render type: a translucent flame wraith sorted incorrectly against terrain
        // (it showed through only where sky/clouds were behind it). The "on fire" look now comes from the
        // vanilla fire overlay + flame particles the entity spawns, not from a see-through material.
        super(root);
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
        float headPitch = state.xRot * DEG_TO_RAD;
        float headYaw = state.yRot * DEG_TO_RAD;

        this.head.xRot = headPitch;
        this.head.yRot = headYaw;
        this.headwear.xRot = headPitch;
        this.headwear.yRot = headYaw;

        float age = state.ageInTicks;

        // legacy raised-arm pose with idle sway
        this.rightArm.yRot = -0.1F;
        this.leftArm.yRot = 0.1F;
        this.rightArm.xRot = -1.570796F;
        this.leftArm.xRot = -1.570796F;
        this.rightArm.zRot = (Mth.cos(age * 0.09F) * 0.05F) + 0.05F;
        this.leftArm.zRot = -((Mth.cos(age * 0.09F) * 0.05F) + 0.05F);
        this.rightArm.xRot += Mth.sin(age * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(age * 0.067F) * 0.05F;
    }
}
