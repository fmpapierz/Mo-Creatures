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
 * Fly model, converted faithfully from the legacy {@code MoCModelFly} ({@code ModelBase}).
 * Geometry, texture offsets and the wing-beat / leg motion are preserved.
 */
public class MoCModelFly extends EntityModel<MoCEntityRenderState> {

    private final ModelPart head;
    private final ModelPart thorax;
    private final ModelPart abdomen;
    private final ModelPart tail;
    private final ModelPart frontLegs;
    private final ModelPart rearLegs;
    private final ModelPart midLegs;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart foldedWings;

    public MoCModelFly(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.thorax = root.getChild("thorax");
        this.abdomen = root.getChild("abdomen");
        this.tail = root.getChild("tail");
        this.frontLegs = root.getChild("front_legs");
        this.rearLegs = root.getChild("rear_legs");
        this.midLegs = root.getChild("mid_legs");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.foldedWings = root.getChild("folded_wings");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 21.5F, -2.0F, -2.171231F, 0.0F, 0.0F));
        root.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 20.5F, -1.0F));
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 21.5F, 0.0F, 1.427659F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(10, 2).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.5F, 21.2F, 1.5F, 1.427659F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 7).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.5F, -1.8F, 0.1487144F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_legs",
                CubeListBuilder.create().texOffs(0, 11).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.5F, -0.4F, 1.070744F, 0.0F, 0.0F));
        root.addOrReplaceChild("mid_legs",
                CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.5F, -1.2F, 0.5948578F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(4, 4).addBox(-1.0F, 0.0F, 0.5F, 2.0F, 0.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 20.4F, -1.0F, 0.0F, 1.047198F, 0.0F));
        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(4, 4).addBox(-1.0F, 0.0F, 0.5F, 2.0F, 0.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 20.4F, -1.0F, 0.0F, -1.047198F, 0.0F));
        root.addOrReplaceChild("folded_wings",
                CubeListBuilder.create().texOffs(4, 4).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 20.5F, -2.0F, 0.0872665F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;

        // wing beat (always, legacy)
        float wingRot = Mth.cos(ageInTicks * 3.0F) * 0.7F;
        this.rightWing.zRot = wingRot;
        this.leftWing.zRot = -wingRot;

        float legMov;
        float legMovB;
        if (state.flying) {
            // Airborne: legs dangle in a fixed flight pose scaled by speed (legacy legMov=f1*1.5).
            legMov = limbAmount * 1.5F;
            legMovB = legMov;
        } else {
            // walking leg motion (legacy on-ground gait)
            legMov = Mth.cos((limbSwing * 1.5F) + 3.141593F) * 2.0F * limbAmount;
            legMovB = Mth.cos(limbSwing * 1.5F) * 2.0F * limbAmount;
        }

        this.frontLegs.xRot = 0.1487144F + legMov;
        this.midLegs.xRot = 0.5948578F + legMovB;
        this.rearLegs.xRot = 1.070744F + legMov;
    }
}
