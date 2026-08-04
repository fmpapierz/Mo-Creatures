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
 * Komodo dragon model, converted faithfully from the legacy {@code MoCModelKomodo} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; the walking leg gait and tail/head animation are ported.
 */
public class MoCModelKomodo extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = 1.0F / 57.29578F;

    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart nose;
    private final ModelPart mouth;
    private final ModelPart tongue;
    private final ModelPart tail;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart tail4;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontLeft1;
    private final ModelPart legFrontLeft2;
    private final ModelPart legBackLeft;
    private final ModelPart legBackLeft1;
    private final ModelPart legBackLeft2;
    private final ModelPart legFrontRight;
    private final ModelPart legFrontRight1;
    private final ModelPart legFrontRight2;
    private final ModelPart legBackRight;
    private final ModelPart legBackRight1;
    private final ModelPart legBackRight2;

    public MoCModelKomodo(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.neck = this.head.getChild("neck");
        this.nose = this.neck.getChild("nose");
        this.mouth = this.neck.getChild("mouth");
        this.tongue = this.mouth.getChild("tongue");
        this.tail = root.getChild("tail");
        this.tail1 = this.tail.getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");
        this.tail4 = this.tail3.getChild("tail4");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legFrontLeft1 = this.legFrontLeft.getChild("leg_front_left1");
        this.legFrontLeft2 = this.legFrontLeft1.getChild("leg_front_left2");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legBackLeft1 = this.legBackLeft.getChild("leg_back_left1");
        this.legBackLeft2 = this.legBackLeft1.getChild("leg_back_left2");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legFrontRight1 = this.legFrontRight.getChild("leg_front_right1");
        this.legFrontRight2 = this.legFrontRight1.getChild("leg_front_right2");
        this.legBackRight = root.getChild("leg_back_right");
        this.legBackRight1 = this.legBackRight.getChild("leg_back_right1");
        this.legBackRight2 = this.legBackRight1.getChild("leg_back_right2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 13.0F, -8.0F));
        PartDefinition neck = head.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(22, 34).addBox(-2.0F, 0.0F, -6.0F, 4.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        neck.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(24, 45).addBox(-1.5F, -1.0F, -6.5F, 3.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 1.0F, -5.0F));
        PartDefinition mouth = neck.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, -0.3F, -5.0F, 2.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 3.0F, -5.8F));
        mouth.addOrReplaceChild("tongue",
                CubeListBuilder.create().texOffs(48, 44).addBox(-1.5F, 0.0F, -5.0F, 3.0F, 0.0F, 5.0F),
                PartPose.offset(0.0F, -0.4F, -4.7F));

        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(36, 2).addBox(-3.0F, 0.0F, -8.0F, 6.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(36, 49).addBox(-3.0F, 0.0F, -1.0F, 6.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        PartDefinition tail = root.addOrReplaceChild("tail",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 13.0F, 7.0F));
        PartDefinition tail1 = tail.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(0, 21).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 0.1F, 7.7F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3",
                CubeListBuilder.create().texOffs(0, 46).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, 0.1F, 7.3F));
        tail3.addOrReplaceChild("tail4",
                CubeListBuilder.create().texOffs(24, 21).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 0.1F, 7.0F));

        PartDefinition legFrontLeft = root.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create(),
                PartPose.offset(2.0F, 17.0F, -7.0F));
        PartDefinition legFrontLeft1 = legFrontLeft.addOrReplaceChild("leg_front_left1",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -1.0F, -1.5F, 4.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition legFrontLeft2 = legFrontLeft1.addOrReplaceChild("leg_front_left2",
                CubeListBuilder.create().texOffs(22, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(3.0F, 0.5F, 0.0F));
        legFrontLeft2.addOrReplaceChild("leg_front_left3",
                CubeListBuilder.create().texOffs(16, 58).addBox(-1.5F, 0.0F, -3.5F, 3.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, -10.0F * DEG_TO_RAD, 0.0F));

        PartDefinition legBackLeft = root.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create(),
                PartPose.offset(2.0F, 17.0F, 6.0F));
        PartDefinition legBackLeft1 = legBackLeft.addOrReplaceChild("leg_back_left1",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -1.0F, -1.5F, 4.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition legBackLeft2 = legBackLeft1.addOrReplaceChild("leg_back_left2",
                CubeListBuilder.create().texOffs(22, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(3.0F, 0.5F, 0.0F));
        legBackLeft2.addOrReplaceChild("leg_back_left3",
                CubeListBuilder.create().texOffs(16, 58).addBox(-1.5F, 0.0F, -3.5F, 3.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, -10.0F * DEG_TO_RAD, 0.0F));

        PartDefinition legFrontRight = root.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create(),
                PartPose.offset(-2.0F, 17.0F, -7.0F));
        PartDefinition legFrontRight1 = legFrontRight.addOrReplaceChild("leg_front_right1",
                CubeListBuilder.create().texOffs(0, 6).addBox(-4.0F, -1.0F, -1.5F, 4.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition legFrontRight2 = legFrontRight1.addOrReplaceChild("leg_front_right2",
                CubeListBuilder.create().texOffs(22, 7).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(-3.0F, 0.5F, 0.0F));
        legFrontRight2.addOrReplaceChild("leg_front_right3",
                CubeListBuilder.create().texOffs(0, 58).addBox(-1.5F, 0.0F, -3.5F, 3.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 10.0F * DEG_TO_RAD, 0.0F));

        PartDefinition legBackRight = root.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create(),
                PartPose.offset(-2.0F, 17.0F, 6.0F));
        PartDefinition legBackRight1 = legBackRight.addOrReplaceChild("leg_back_right1",
                CubeListBuilder.create().texOffs(0, 6).addBox(-4.0F, -1.0F, -1.5F, 4.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition legBackRight2 = legBackRight1.addOrReplaceChild("leg_back_right2",
                CubeListBuilder.create().texOffs(22, 7).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(-3.0F, 0.5F, 0.0F));
        legBackRight2.addOrReplaceChild("leg_back_right3",
                CubeListBuilder.create().texOffs(0, 58).addBox(-1.5F, 0.0F, -3.5F, 3.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 10.0F * DEG_TO_RAD, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float netHeadYaw = state.yRot;
        float headPitch = state.xRot;

        float tailXRot = Mth.cos(limbSwing * 0.4F) * 0.2F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6F) * 0.9F * limbAmount;
        float rLegXRot = Mth.cos((limbSwing * 0.6F) + 3.141593F) * 0.9F * limbAmount;

        if (netHeadYaw > 60F) {
            netHeadYaw = 60F;
        }
        if (netHeadYaw < -60F) {
            netHeadYaw = -60F;
        }

        // Standing (walking) gait
        this.tail1.xRot = (-15F * DEG_TO_RAD) - tailXRot;
        this.legFrontLeft1.zRot = 30F * DEG_TO_RAD;
        this.legFrontLeft2.zRot = -30F * DEG_TO_RAD;
        this.legFrontLeft1.yRot = lLegXRot;
        this.legFrontLeft2.xRot = -lLegXRot;

        this.legBackLeft1.zRot = 30F * DEG_TO_RAD;
        this.legBackLeft2.zRot = -30F * DEG_TO_RAD;
        this.legBackLeft1.yRot = rLegXRot;
        this.legBackLeft2.xRot = -rLegXRot;

        this.legFrontRight1.zRot = -30F * DEG_TO_RAD;
        this.legFrontRight2.zRot = 30F * DEG_TO_RAD;
        this.legFrontRight1.yRot = -rLegXRot;
        this.legFrontRight2.xRot = -rLegXRot;

        this.legBackRight1.zRot = -30F * DEG_TO_RAD;
        this.legBackRight2.zRot = 30F * DEG_TO_RAD;
        this.legBackRight1.yRot = -lLegXRot;
        this.legBackRight2.xRot = -lLegXRot;

        this.tongue.z = 0.3F;

        this.neck.xRot = 11F * DEG_TO_RAD + (headPitch * 0.33F * DEG_TO_RAD);
        this.nose.xRot = 10.6F * DEG_TO_RAD + (headPitch * 0.66F * DEG_TO_RAD);
        this.mouth.xRot = (-3F * DEG_TO_RAD) + (headPitch * 0.66F * DEG_TO_RAD);
        this.tongue.xRot = 0F;

        this.neck.yRot = (netHeadYaw * 0.33F * DEG_TO_RAD);
        this.nose.yRot = (netHeadYaw * 0.66F * DEG_TO_RAD);
        this.mouth.yRot = (netHeadYaw * 0.66F * DEG_TO_RAD);

        this.tail2.xRot = (-17F * DEG_TO_RAD) + tailXRot;
        this.tail3.xRot = (13F * DEG_TO_RAD) + tailXRot;
        this.tail4.xRot = (11F * DEG_TO_RAD) + tailXRot;

        float t = limbSwing / 2F;
        float a = 0.35F;
        float w = 0.6F;
        float k = 0.6F;

        int i = 0;
        this.tail1.yRot = a * Mth.sin(w * t - k * (float) i++);
        this.tail2.yRot = a * Mth.sin(w * t - k * (float) i++);
        this.tail3.yRot = a * Mth.sin(w * t - k * (float) i++);
        this.tail4.yRot = a * Mth.sin(w * t - k * (float) i++);
    }
}
