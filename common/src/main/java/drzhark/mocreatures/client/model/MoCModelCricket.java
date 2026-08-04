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
 * Cricket model, converted faithfully from the legacy {@code MoCModelCricket} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; only the scaffolding is modern.
 */
public class MoCModelCricket extends EntityModel<MoCEntityRenderState> {

    private final ModelPart head;
    private final ModelPart antenna;
    private final ModelPart antennaB;
    private final ModelPart thorax;
    private final ModelPart abdomen;
    private final ModelPart tailA;
    private final ModelPart tailB;
    private final ModelPart frontLegs;
    private final ModelPart midLegs;
    private final ModelPart thighLeft;
    private final ModelPart thighRight;
    private final ModelPart legLeft;
    private final ModelPart legRight;
    private final ModelPart foldedWings;

    public MoCModelCricket(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.antenna = root.getChild("antenna");
        this.antennaB = root.getChild("antenna_b");
        this.thorax = root.getChild("thorax");
        this.abdomen = root.getChild("abdomen");
        this.tailA = root.getChild("tail_a");
        this.tailB = root.getChild("tail_b");
        this.frontLegs = root.getChild("front_legs");
        this.midLegs = root.getChild("mid_legs");
        this.thighLeft = root.getChild("thigh_left");
        this.thighRight = root.getChild("thigh_right");
        this.legLeft = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
        this.foldedWings = root.getChild("folded_wings");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 4).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 22.5F, -2.0F, -2.171231F, 0.0F, 0.0F));
        root.addOrReplaceChild("antenna",
                CubeListBuilder.create().texOffs(0, 11).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.5F, -3.0F, -2.736346F, 0.0F, 0.0F));
        root.addOrReplaceChild("antenna_b",
                CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 20.7F, -3.8F, 2.88506F, 0.0F, 0.0F));
        root.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 21.0F, -1.0F));
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 1.427659F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_a",
                CubeListBuilder.create().texOffs(4, 9).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 2.8F, 1.308687F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_b",
                CubeListBuilder.create().texOffs(4, 7).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 23.0F, 2.8F, 1.665602F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 7).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 23.0F, -1.8F, -0.8328009F, 0.0F, 0.0F));
        root.addOrReplaceChild("mid_legs",
                CubeListBuilder.create().texOffs(0, 13).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 23.0F, -1.2F, 1.070744F, 0.0F, 0.0F));
        root.addOrReplaceChild("thigh_left",
                CubeListBuilder.create().texOffs(8, 5).addBox(0.0F, -3.0F, 0.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.5F, 23.0F, 0.0F, -0.4886922F, 0.2617994F, 0.0F));
        root.addOrReplaceChild("thigh_right",
                CubeListBuilder.create().texOffs(12, 5).addBox(-1.0F, -3.0F, 0.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-0.5F, 23.0F, 0.0F, -0.4886922F, -0.2617994F, 0.0F));
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(0, 15).addBox(0.0F, 0.0F, -1.0F, 0.0F, 3.0F, 2.0F),
                PartPose.offset(2.0F, 21.0F, 2.5F));
        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(4, 15).addBox(0.0F, 0.0F, -1.0F, 0.0F, 3.0F, 2.0F),
                PartPose.offset(-2.0F, 21.0F, 2.5F));
        root.addOrReplaceChild("folded_wings",
                CubeListBuilder.create().texOffs(0, 26).addBox(0.0F, 0.0F, -1.0F, 6.0F, 0.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 20.9F, -2.0F, 0.0F, -1.570796F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        if (state.flying) {
            // Airborne (legacy isFlying): the wings buzz open and the leg drive quickens with the front legs
            // tucked up out of the way.
            float flap = Mth.cos(state.ageInTicks * 2.0F) * 0.7F;
            this.foldedWings.zRot = flap;
            float legMov = Mth.cos((limbSwing * 1.5F) + 3.141593F) * 2.0F * limbAmount * 1.5F;
            float legMovB = Mth.cos(limbSwing * 1.5F) * 2.0F * limbAmount * 1.5F;
            this.antennaB.xRot = 2.88506F - legMov;
            this.frontLegs.xRot = -0.8328009F + 1.4F + legMov;
            this.midLegs.xRot = 1.070744F + legMovB;
        } else {
            // grounded gait (legacy non-flying branch)
            this.foldedWings.zRot = 0.0F;
            float legMov = Mth.cos((limbSwing * 1.5F) + 3.141593F) * 2.0F * limbAmount;
            float legMovB = Mth.cos(limbSwing * 1.5F) * 2.0F * limbAmount;

            this.antennaB.xRot = 2.88506F - legMov;
            this.frontLegs.xRot = -0.8328009F + legMov;
            this.midLegs.xRot = 1.070744F + legMovB;
        }
    }
}
