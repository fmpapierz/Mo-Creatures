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
 * Ray model, converted faithfully from the legacy {@code MoCModelRay} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; the flapping-wing swim gait is ported to setupAnim.
 */
public class MoCModelRay extends EntityModel<MoCEntityRenderState> {

    private final ModelPart tail;
    private final ModelPart body;
    private final ModelPart right;
    private final ModelPart left;
    private final ModelPart bodyU;
    private final ModelPart bodyTail;
    private final ModelPart rWingA;
    private final ModelPart rWingB;
    private final ModelPart rWingC;
    private final ModelPart rWingD;
    private final ModelPart rWingE;
    private final ModelPart rWingF;
    private final ModelPart rWingG;
    private final ModelPart lWingA;
    private final ModelPart lWingB;
    private final ModelPart lWingC;
    private final ModelPart lWingD;
    private final ModelPart lWingE;
    private final ModelPart lWingF;
    private final ModelPart lWingG;
    private final ModelPart lEye;
    private final ModelPart rEye;

    public MoCModelRay(ModelPart root) {
        super(root);
        this.tail = root.getChild("tail");
        this.body = root.getChild("body");
        this.right = root.getChild("right");
        this.left = root.getChild("left");
        this.bodyU = root.getChild("body_u");
        this.bodyTail = root.getChild("body_tail");
        this.rWingA = root.getChild("r_wing_a");
        this.rWingB = root.getChild("r_wing_b");
        this.rWingC = root.getChild("r_wing_c");
        this.rWingD = root.getChild("r_wing_d");
        this.rWingE = root.getChild("r_wing_e");
        this.rWingF = root.getChild("r_wing_f");
        this.rWingG = root.getChild("r_wing_g");
        this.lWingA = root.getChild("l_wing_a");
        this.lWingB = root.getChild("l_wing_b");
        this.lWingC = root.getChild("l_wing_c");
        this.lWingD = root.getChild("l_wing_d");
        this.lWingE = root.getChild("l_wing_e");
        this.lWingF = root.getChild("l_wing_f");
        this.lWingG = root.getChild("l_wing_g");
        this.lEye = root.getChild("l_eye");
        this.rEye = root.getChild("r_eye");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(26, 0).addBox(-4.0F, -1.0F, 0.0F, 8.0F, 2.0F, 11.0F),
                PartPose.offset(0.0F, 22.0F, -5.0F));
        root.addOrReplaceChild("right",
                CubeListBuilder.create().texOffs(10, 26).addBox(-0.5F, -1.0F, -4.0F, 1.0F, 2.0F, 4.0F),
                PartPose.offset(-3.0F, 22.0F, -4.8F));
        root.addOrReplaceChild("left",
                CubeListBuilder.create().texOffs(0, 26).addBox(-0.5F, -1.0F, -4.0F, 1.0F, 2.0F, 4.0F),
                PartPose.offset(3.0F, 22.0F, -4.8F));
        root.addOrReplaceChild("body_u",
                CubeListBuilder.create().texOffs(0, 11).addBox(-3.0F, -1.0F, 0.0F, 6.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 21.0F, -4.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(30, 15).addBox(-0.5F, -0.5F, 1.0F, 1.0F, 1.0F, 16.0F),
                PartPose.offset(0.0F, 22.0F, 8.0F));
        root.addOrReplaceChild("body_tail",
                CubeListBuilder.create().texOffs(0, 20).addBox(-1.8F, -0.5F, -3.2F, 5.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 7.0F, 0.0F, 1.0F, 0.0F));

        root.addOrReplaceChild("r_wing_a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -0.5F, -5.0F, 3.0F, 1.0F, 10.0F),
                PartPose.offset(-4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("r_wing_b",
                CubeListBuilder.create().texOffs(2, 2).addBox(-6.0F, -0.5F, -4.0F, 3.0F, 1.0F, 8.0F),
                PartPose.offset(-4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("r_wing_c",
                CubeListBuilder.create().texOffs(5, 4).addBox(-8.0F, -0.5F, -3.0F, 2.0F, 1.0F, 6.0F),
                PartPose.offset(-4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("r_wing_d",
                CubeListBuilder.create().texOffs(6, 5).addBox(-10.0F, -0.5F, -2.5F, 2.0F, 1.0F, 5.0F),
                PartPose.offset(-4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("r_wing_e",
                CubeListBuilder.create().texOffs(7, 6).addBox(-12.0F, -0.5F, -2.0F, 2.0F, 1.0F, 4.0F),
                PartPose.offset(-4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("r_wing_f",
                CubeListBuilder.create().texOffs(8, 7).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(-4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("r_wing_g",
                CubeListBuilder.create().texOffs(9, 8).addBox(-16.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(-4.0F, 22.0F, 1.0F));

        root.addOrReplaceChild("l_wing_a",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, -0.5F, -5.0F, 3.0F, 1.0F, 10.0F),
                PartPose.offset(4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("l_wing_b",
                CubeListBuilder.create().texOffs(2, 2).mirror().addBox(3.0F, -0.5F, -4.0F, 3.0F, 1.0F, 8.0F),
                PartPose.offset(4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("l_wing_c",
                CubeListBuilder.create().texOffs(5, 4).mirror().addBox(6.0F, -0.5F, -3.0F, 2.0F, 1.0F, 6.0F),
                PartPose.offset(4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("l_wing_d",
                CubeListBuilder.create().texOffs(6, 5).mirror().addBox(8.0F, -0.5F, -2.5F, 2.0F, 1.0F, 5.0F),
                PartPose.offset(4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("l_wing_e",
                CubeListBuilder.create().texOffs(7, 6).mirror().addBox(10.0F, -0.5F, -2.0F, 2.0F, 1.0F, 4.0F),
                PartPose.offset(4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("l_wing_f",
                CubeListBuilder.create().texOffs(8, 7).mirror().addBox(12.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(4.0F, 22.0F, 1.0F));
        root.addOrReplaceChild("l_wing_g",
                CubeListBuilder.create().texOffs(9, 8).mirror().addBox(14.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(4.0F, 22.0F, 1.0F));

        root.addOrReplaceChild("l_eye",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.0F, 1.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 21.0F, -4.0F));
        root.addOrReplaceChild("r_eye",
                CubeListBuilder.create().texOffs(0, 3).addBox(2.0F, -2.0F, 1.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 21.0F, -4.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        // Legacy part-visibility gating on ray type: MANTA (type 1) shows the side fins
        // and the full wing chain (segments c-g) and hides the eyes; STINGRAY (type 2)
        // shows the eyes and hides the side fins and wing segments c-g (only a/b show).
        boolean manta = state.typeMoC == 1;
        this.rEye.visible = !manta;
        this.lEye.visible = !manta;
        this.right.visible = manta;
        this.left.visible = manta;
        this.rWingC.visible = manta;
        this.rWingD.visible = manta;
        this.rWingE.visible = manta;
        this.rWingF.visible = manta;
        this.rWingG.visible = manta;
        this.lWingC.visible = manta;
        this.lWingD.visible = manta;
        this.lWingE.visible = manta;
        this.lWingF.visible = manta;
        this.lWingG.visible = manta;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        float rotF = Mth.cos(limbSwing * 0.6662F) * 1.5F * limbAmount;
        float f6 = 20.0F;
        this.tail.yRot = rotF;
        this.rWingA.zRot = rotF;
        this.lWingA.zRot = -rotF;
        rotF += (rotF / f6);
        this.rWingB.zRot = rotF;
        this.lWingB.zRot = -rotF;
        rotF += (rotF / f6);

        this.rWingC.zRot = rotF;
        this.lWingC.zRot = -rotF;
        rotF += (rotF / f6);

        this.rWingD.zRot = rotF;
        this.lWingD.zRot = -rotF;
        rotF += (rotF / f6);

        this.rWingE.zRot = rotF;
        this.lWingE.zRot = -rotF;
        rotF += (rotF / f6);

        this.rWingF.zRot = rotF;
        this.lWingF.zRot = -rotF;
        rotF += (rotF / f6);

        this.rWingG.zRot = rotF;
        this.lWingG.zRot = -rotF;

        this.tail.xRot = 0.0F;
    }
}
