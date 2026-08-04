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
 * Crocodile model, converted faithfully from the legacy {@code MoCModelCrocodile} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; the leg gait, tail sway and head tracking are kept,
 * while the bite/swim/resting state animations (which needed the live entity) are simplified.
 */
public class MoCModelCrocodile extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart ljaw;
    private final ModelPart tailA;
    private final ModelPart tailB;
    private final ModelPart tailC;
    private final ModelPart ujaw;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leg1;
    private final ModelPart leg3;
    private final ModelPart leg2;
    private final ModelPart leg4;
    private final ModelPart tailD;
    private final ModelPart leg1A;
    private final ModelPart leg2A;
    private final ModelPart leg3A;
    private final ModelPart leg4A;
    private final ModelPart ujaw2;
    private final ModelPart ljaw2;
    private final ModelPart teethA;
    private final ModelPart teethB;
    private final ModelPart teethC;
    private final ModelPart teethD;
    private final ModelPart teethF;
    private final ModelPart spike0;
    private final ModelPart spike1;
    private final ModelPart spike2;
    private final ModelPart spike3;
    private final ModelPart spike4;
    private final ModelPart spike5;
    private final ModelPart spike6;
    private final ModelPart spike7;
    private final ModelPart spike8;
    private final ModelPart spike9;
    private final ModelPart spike10;
    private final ModelPart spike11;
    private final ModelPart spikeEye;
    private final ModelPart spikeEye1;
    private final ModelPart teethA1;
    private final ModelPart teethB1;
    private final ModelPart teethC1;
    private final ModelPart teethD1;

    public MoCModelCrocodile(ModelPart root) {
        super(root);
        this.ljaw = root.getChild("ljaw");
        this.tailA = root.getChild("tail_a");
        this.tailB = root.getChild("tail_b");
        this.tailC = root.getChild("tail_c");
        this.ujaw = root.getChild("ujaw");
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leg1 = root.getChild("leg1");
        this.leg3 = root.getChild("leg3");
        this.leg2 = root.getChild("leg2");
        this.leg4 = root.getChild("leg4");
        this.tailD = root.getChild("tail_d");
        this.leg1A = root.getChild("leg1a");
        this.leg2A = root.getChild("leg2a");
        this.leg3A = root.getChild("leg3a");
        this.leg4A = root.getChild("leg4a");
        this.ujaw2 = root.getChild("ujaw2");
        this.ljaw2 = root.getChild("ljaw2");
        this.teethA = root.getChild("teeth_a");
        this.teethB = root.getChild("teeth_b");
        this.teethC = root.getChild("teeth_c");
        this.teethD = root.getChild("teeth_d");
        this.teethF = root.getChild("teeth_f");
        this.spike0 = root.getChild("spike0");
        this.spike1 = root.getChild("spike1");
        this.spike2 = root.getChild("spike2");
        this.spike3 = root.getChild("spike3");
        this.spike4 = root.getChild("spike4");
        this.spike5 = root.getChild("spike5");
        this.spike6 = root.getChild("spike6");
        this.spike7 = root.getChild("spike7");
        this.spike8 = root.getChild("spike8");
        this.spike9 = root.getChild("spike9");
        this.spike10 = root.getChild("spike10");
        this.spike11 = root.getChild("spike11");
        this.spikeEye = root.getChild("spike_eye");
        this.spikeEye1 = root.getChild("spike_eye1");
        this.teethA1 = root.getChild("teeth_a1");
        this.teethB1 = root.getChild("teeth_b1");
        this.teethC1 = root.getChild("teeth_c1");
        this.teethD1 = root.getChild("teeth_d1");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("ljaw",
                CubeListBuilder.create().texOffs(42, 0).addBox(-2.5F, 1.0F, -12.0F, 5.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("tail_a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -0.5F, 0.0F, 8.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("tail_b",
                CubeListBuilder.create().texOffs(2, 0).addBox(-3.0F, 0.0F, 8.0F, 6.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("tail_c",
                CubeListBuilder.create().texOffs(6, 2).addBox(-2.0F, 0.5F, 16.0F, 4.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("tail_d",
                CubeListBuilder.create().texOffs(7, 2).addBox(-1.5F, 1.0F, 22.0F, 3.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("ujaw",
                CubeListBuilder.create().texOffs(44, 8).addBox(-2.0F, -1.0F, -12.0F, 4.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -2.0F, -6.0F, 6.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(4, 7).addBox(0.0F, 0.0F, 0.0F, 10.0F, 5.0F, 20.0F),
                PartPose.offset(-5.0F, 16.0F, -8.0F));
        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(49, 21).addBox(1.0F, 2.0F, -3.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(5.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(48, 20).addBox(1.0F, 2.0F, -3.0F, 3.0F, 2.0F, 5.0F),
                PartPose.offset(5.0F, 19.0F, 9.0F));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(49, 21).addBox(-4.0F, 2.0F, -3.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(-5.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(48, 20).addBox(-4.0F, 2.0F, -3.0F, 3.0F, 2.0F, 5.0F),
                PartPose.offset(-5.0F, 19.0F, 9.0F));
        root.addOrReplaceChild("leg1a",
                CubeListBuilder.create().texOffs(7, 9).addBox(0.0F, -1.0F, -2.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(5.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("leg2a",
                CubeListBuilder.create().texOffs(7, 9).addBox(-3.0F, -1.0F, -2.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(-5.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("leg3a",
                CubeListBuilder.create().texOffs(6, 8).addBox(0.0F, -1.0F, -2.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(5.0F, 19.0F, 9.0F));
        root.addOrReplaceChild("leg4a",
                CubeListBuilder.create().texOffs(6, 8).addBox(-3.0F, -1.0F, -2.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(-5.0F, 19.0F, 9.0F));
        root.addOrReplaceChild("ujaw2",
                CubeListBuilder.create().texOffs(37, 0).addBox(-1.5F, -1.0F, -16.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("ljaw2",
                CubeListBuilder.create().texOffs(24, 1).addBox(-2.0F, 1.0F, -16.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("teeth_a",
                CubeListBuilder.create().texOffs(8, 11).addBox(1.6F, 0.0F, -16.0F, 0.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("teeth_b",
                CubeListBuilder.create().texOffs(8, 11).addBox(-1.6F, 0.0F, -16.0F, 0.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("teeth_c",
                CubeListBuilder.create().texOffs(6, 9).addBox(2.1F, 0.0F, -12.0F, 0.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("teeth_d",
                CubeListBuilder.create().texOffs(6, 9).addBox(-2.1F, 0.0F, -12.0F, 0.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("teeth_f",
                CubeListBuilder.create().texOffs(19, 21).addBox(-1.0F, 0.0F, -16.1F, 2.0F, 1.0F, 0.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("spike0",
                CubeListBuilder.create().texOffs(44, 16).addBox(-1.0F, -1.0F, 23.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike1",
                CubeListBuilder.create().texOffs(44, 16).addBox(1.0F, -1.0F, 23.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike2",
                CubeListBuilder.create().texOffs(44, 16).addBox(-1.5F, -1.5F, 17.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike3",
                CubeListBuilder.create().texOffs(44, 16).addBox(1.5F, -1.5F, 17.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike4",
                CubeListBuilder.create().texOffs(44, 16).addBox(-2.0F, -2.0F, 12.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike5",
                CubeListBuilder.create().texOffs(44, 16).addBox(2.0F, -2.0F, 12.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike6",
                CubeListBuilder.create().texOffs(44, 16).addBox(-2.5F, -2.0F, 8.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike7",
                CubeListBuilder.create().texOffs(44, 16).addBox(2.5F, -2.0F, 8.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike8",
                CubeListBuilder.create().texOffs(44, 16).addBox(-3.0F, -2.5F, 4.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike9",
                CubeListBuilder.create().texOffs(44, 16).addBox(3.0F, -2.5F, 4.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike10",
                CubeListBuilder.create().texOffs(44, 16).addBox(3.5F, -2.5F, 0.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike11",
                CubeListBuilder.create().texOffs(44, 16).addBox(-3.5F, -2.5F, 0.0F, 0.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 12.0F));
        root.addOrReplaceChild("spike_back0",
                CubeListBuilder.create().texOffs(44, 10).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 14.0F, 3.0F));
        root.addOrReplaceChild("spike_back1",
                CubeListBuilder.create().texOffs(44, 10).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 14.0F, -6.0F));
        root.addOrReplaceChild("spike_back2",
                CubeListBuilder.create().texOffs(44, 10).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 8.0F),
                PartPose.offset(4.0F, 14.0F, -8.0F));
        root.addOrReplaceChild("spike_back3",
                CubeListBuilder.create().texOffs(44, 10).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 8.0F),
                PartPose.offset(-4.0F, 14.0F, -8.0F));
        root.addOrReplaceChild("spike_back4",
                CubeListBuilder.create().texOffs(44, 10).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 8.0F),
                PartPose.offset(-4.0F, 14.0F, 1.0F));
        root.addOrReplaceChild("spike_back5",
                CubeListBuilder.create().texOffs(44, 10).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 8.0F),
                PartPose.offset(4.0F, 14.0F, 1.0F));
        root.addOrReplaceChild("spike_eye",
                CubeListBuilder.create().texOffs(44, 14).addBox(-3.0F, -3.0F, -6.0F, 0.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("spike_eye1",
                CubeListBuilder.create().texOffs(44, 14).addBox(3.0F, -3.0F, -6.0F, 0.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("teeth_a1",
                CubeListBuilder.create().texOffs(52, 12).addBox(1.4F, 1.0F, -16.4F, 0.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("teeth_b1",
                CubeListBuilder.create().texOffs(52, 12).addBox(-1.4F, 1.0F, -16.4F, 0.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("teeth_c1",
                CubeListBuilder.create().texOffs(50, 10).addBox(1.9F, 1.0F, -12.5F, 0.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        root.addOrReplaceChild("teeth_d1",
                CubeListBuilder.create().texOffs(50, 10).addBox(-1.9F, 1.0F, -12.5F, 0.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float headYaw = state.yRot * DEG_TO_RAD;
        float headPitch = state.xRot * DEG_TO_RAD;

        this.head.xRot = headPitch;
        this.head.yRot = headYaw;
        this.spikeEye.xRot = this.head.xRot;
        this.spikeEye.yRot = this.head.yRot;
        this.spikeEye1.xRot = this.head.xRot;
        this.spikeEye1.yRot = this.head.yRot;

        this.ljaw.yRot = this.head.yRot;
        this.ljaw2.yRot = this.head.yRot;
        this.ujaw.yRot = this.head.yRot;
        this.ujaw2.yRot = this.head.yRot;

        if (state.crocInWater) {
            // Swimming: the crocodile tucks its legs back alongside the body and sculls its tail; the legs
            // trail streamlined instead of striding (legacy in-water pose).
            this.leg1.xRot = -1.4F;
            this.leg2.xRot = -1.4F;
            this.leg3.xRot = -1.4F;
            this.leg4.xRot = -1.4F;
            this.leg1A.xRot = this.leg1.xRot;
            this.leg2A.xRot = this.leg2.xRot;
            this.leg3A.xRot = this.leg3.xRot;
            this.leg4A.xRot = this.leg4.xRot;
            this.leg1.zRot = 0.0F;
            this.leg1A.zRot = 0.0F;
            this.leg2.zRot = 0.0F;
            this.leg2A.zRot = 0.0F;
            this.leg3.zRot = 0.0F;
            this.leg3A.zRot = 0.0F;
            this.leg4.zRot = 0.0F;
            this.leg4A.zRot = 0.0F;
        } else {
            // walking leg gait
            this.leg1.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
            this.leg2.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;
            this.leg3.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;
            this.leg4.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;

            this.leg1A.xRot = this.leg1.xRot;
            this.leg2A.xRot = this.leg2.xRot;
            this.leg3A.xRot = this.leg3.xRot;
            this.leg4A.xRot = this.leg4.xRot;

            float latrot = Mth.cos(limbSwing / 1.919107651F) * 0.261799387799149F * limbAmount * 5;
            this.leg1.zRot = latrot;
            this.leg1A.zRot = latrot;
            this.leg4.zRot = -latrot;
            this.leg4A.zRot = -latrot;
            this.leg3.zRot = latrot;
            this.leg3A.zRot = latrot;
            this.leg2.zRot = -latrot;
            this.leg2A.zRot = -latrot;
        }

        // tail sway — a slow, powerful scull while swimming (driven by the age timer so it sways even when
        // stationary in water), otherwise a gentle walk-driven sway.
        float tailSway = state.crocInWater
                ? Mth.cos(state.ageInTicks * 0.2F) * 0.5F
                : Mth.cos(limbSwing * 0.6662F) * 0.7F * limbAmount;
        this.tailA.yRot = tailSway;
        this.tailB.yRot = tailSway;
        this.tailC.yRot = tailSway;
        this.tailD.yRot = tailSway;
        this.spike0.yRot = tailSway;
        this.spike1.yRot = tailSway;
        this.spike2.yRot = tailSway;
        this.spike3.yRot = tailSway;
        this.spike4.yRot = tailSway;
        this.spike5.yRot = tailSway;
        this.spike6.yRot = tailSway;
        this.spike7.yRot = tailSway;
        this.spike8.yRot = tailSway;
        this.spike9.yRot = tailSway;
        this.spike10.yRot = tailSway;
        this.spike11.yRot = tailSway;

        // jaws / teeth follow the head; when the croc has seized prey the maw gapes wide (upper jaw lifts,
        // lower jaw drops) for the death-roll bite.
        float biteOpen = state.crocBiting ? 0.45F : 0.0F;
        this.ujaw.xRot = this.head.xRot - biteOpen;
        this.ujaw2.xRot = this.ujaw.xRot;
        this.ljaw.xRot = this.head.xRot + (biteOpen * 0.55F);
        this.ljaw2.xRot = this.ljaw.xRot;
        this.teethA.xRot = this.ljaw.xRot;
        this.teethB.xRot = this.ljaw.xRot;
        this.teethC.xRot = this.ljaw.xRot;
        this.teethD.xRot = this.ljaw.xRot;
        this.teethF.xRot = this.ljaw.xRot;
        this.teethA.yRot = this.ljaw.yRot;
        this.teethB.yRot = this.ljaw.yRot;
        this.teethC.yRot = this.ljaw.yRot;
        this.teethD.yRot = this.ljaw.yRot;
        this.teethF.yRot = this.ljaw.yRot;
        this.teethA1.xRot = this.ujaw.xRot;
        this.teethB1.xRot = this.ujaw.xRot;
        this.teethC1.xRot = this.ujaw.xRot;
        this.teethD1.xRot = this.ujaw.xRot;
        this.teethA1.yRot = this.ujaw.yRot;
        this.teethB1.yRot = this.ujaw.yRot;
        this.teethC1.yRot = this.ujaw.yRot;
        this.teethD1.yRot = this.ujaw.yRot;
    }
}
