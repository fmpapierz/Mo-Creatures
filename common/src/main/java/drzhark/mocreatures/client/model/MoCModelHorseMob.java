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
 * Horse-mob model, converted faithfully from the legacy {@code MoCModelNewHorse} geometry
 * ({@code ModelBase}). All cube boxes, texture offsets and the static head/leg poses are preserved;
 * the walk gait + head look-tracking are kept, and the bat-horse (type 32) shows its beating membrane
 * wings (legacy ButterflyL/R). The saddle/standing/rearing rig remains simplified away (these are hostile
 * mobs, not mounts).
 */
public class MoCModelHorseMob extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart uMouth;
    private final ModelPart lMouth;
    private final ModelPart unicorn;
    private final ModelPart ear1;
    private final ModelPart ear2;
    private final ModelPart neck;
    private final ModelPart mane;
    private final ModelPart body;
    private final ModelPart tailA;
    private final ModelPart tailB;
    private final ModelPart tailC;
    private final ModelPart leg1A;
    private final ModelPart leg1B;
    private final ModelPart leg1C;
    private final ModelPart leg2A;
    private final ModelPart leg2B;
    private final ModelPart leg2C;
    private final ModelPart leg3A;
    private final ModelPart leg3B;
    private final ModelPart leg3C;
    private final ModelPart leg4A;
    private final ModelPart leg4B;
    private final ModelPart leg4C;
    /** Bat-horse (type 32) membrane wings (legacy ButterflyL/R). */
    private final ModelPart butterflyL;
    private final ModelPart butterflyR;

    public MoCModelHorseMob(ModelPart root) {
        super(root);
        this.butterflyL = root.getChild("butterfly_l");
        this.butterflyR = root.getChild("butterfly_r");
        this.head = root.getChild("head");
        this.uMouth = root.getChild("u_mouth");
        this.lMouth = root.getChild("l_mouth");
        this.unicorn = root.getChild("unicorn");
        this.ear1 = root.getChild("ear1");
        this.ear2 = root.getChild("ear2");
        this.neck = root.getChild("neck");
        this.mane = root.getChild("mane");
        this.body = root.getChild("body");
        this.tailA = root.getChild("tail_a");
        this.tailB = root.getChild("tail_b");
        this.tailC = root.getChild("tail_c");
        this.leg1A = root.getChild("leg1_a");
        this.leg1B = root.getChild("leg1_b");
        this.leg1C = root.getChild("leg1_c");
        this.leg2A = root.getChild("leg2_a");
        this.leg2B = root.getChild("leg2_b");
        this.leg2C = root.getChild("leg2_c");
        this.leg3A = root.getChild("leg3_a");
        this.leg3B = root.getChild("leg3_b");
        this.leg3C = root.getChild("leg3_c");
        this.leg4A = root.getChild("leg4_a");
        this.leg4B = root.getChild("leg4_b");
        this.leg4C = root.getChild("leg4_c");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        float headRot = 0.5235988F;

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 34).addBox(-5F, -8F, -19F, 10, 10, 24),
                PartPose.offset(0F, 11F, 9F));

        root.addOrReplaceChild("tail_a",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -1F, 0F, 2, 2, 3),
                PartPose.offsetAndRotation(0F, 3F, 14F, -1.134464F, 0F, 0F));
        root.addOrReplaceChild("tail_b",
                CubeListBuilder.create().texOffs(38, 7).addBox(-1.5F, -2F, 3F, 3, 4, 7),
                PartPose.offsetAndRotation(0F, 3F, 14F, -1.134464F, 0F, 0F));
        root.addOrReplaceChild("tail_c",
                CubeListBuilder.create().texOffs(24, 3).addBox(-1.5F, -4.5F, 9F, 3, 4, 7),
                PartPose.offsetAndRotation(0F, 3F, 14F, -1.40215F, 0F, 0F));

        root.addOrReplaceChild("leg1_a",
                CubeListBuilder.create().texOffs(78, 29).addBox(-2.5F, -2F, -2.5F, 4, 9, 5),
                PartPose.offset(4F, 9F, 11F));
        root.addOrReplaceChild("leg1_b",
                CubeListBuilder.create().texOffs(78, 43).addBox(-2F, 0F, -1.5F, 3, 5, 3),
                PartPose.offset(4F, 16F, 11F));
        root.addOrReplaceChild("leg1_c",
                CubeListBuilder.create().texOffs(78, 51).addBox(-2.5F, 5.1F, -2F, 4, 3, 4),
                PartPose.offset(4F, 16F, 11F));

        root.addOrReplaceChild("leg2_a",
                CubeListBuilder.create().texOffs(96, 29).addBox(-1.5F, -2F, -2.5F, 4, 9, 5),
                PartPose.offset(-4F, 9F, 11F));
        root.addOrReplaceChild("leg2_b",
                CubeListBuilder.create().texOffs(96, 43).addBox(-1F, 0F, -1.5F, 3, 5, 3),
                PartPose.offset(-4F, 16F, 11F));
        root.addOrReplaceChild("leg2_c",
                CubeListBuilder.create().texOffs(96, 51).addBox(-1.5F, 5.1F, -2F, 4, 3, 4),
                PartPose.offset(-4F, 16F, 11F));

        root.addOrReplaceChild("leg3_a",
                CubeListBuilder.create().texOffs(44, 29).addBox(-1.9F, -1F, -2.1F, 3, 8, 4),
                PartPose.offset(4F, 9F, -8F));
        root.addOrReplaceChild("leg3_b",
                CubeListBuilder.create().texOffs(44, 41).addBox(-1.9F, 0F, -1.6F, 3, 5, 3),
                PartPose.offset(4F, 16F, -8F));
        root.addOrReplaceChild("leg3_c",
                CubeListBuilder.create().texOffs(44, 51).addBox(-2.4F, 5.1F, -2.1F, 4, 3, 4),
                PartPose.offset(4F, 16F, -8F));

        root.addOrReplaceChild("leg4_a",
                CubeListBuilder.create().texOffs(60, 29).addBox(-1.1F, -1F, -2.1F, 3, 8, 4),
                PartPose.offset(-4F, 9F, -8F));
        root.addOrReplaceChild("leg4_b",
                CubeListBuilder.create().texOffs(60, 41).addBox(-1.1F, 0F, -1.6F, 3, 5, 3),
                PartPose.offset(-4F, 16F, -8F));
        root.addOrReplaceChild("leg4_c",
                CubeListBuilder.create().texOffs(60, 51).addBox(-1.6F, 5.1F, -2.1F, 4, 3, 4),
                PartPose.offset(-4F, 16F, -8F));

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -10F, -1.5F, 5, 5, 7),
                PartPose.offsetAndRotation(0F, 4F, -10F, headRot, 0F, 0F));

        root.addOrReplaceChild("u_mouth",
                CubeListBuilder.create().texOffs(24, 18).addBox(-2F, -10F, -7F, 4, 3, 6),
                PartPose.offsetAndRotation(0F, 4F, -10F, headRot, 0F, 0F));
        root.addOrReplaceChild("l_mouth",
                CubeListBuilder.create().texOffs(24, 27).addBox(-2F, -7F, -6.5F, 4, 2, 5),
                PartPose.offsetAndRotation(0F, 4F, -10F, headRot, 0F, 0F));

        root.addOrReplaceChild("unicorn",
                CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -18F, 2F, 1, 8, 1),
                PartPose.offsetAndRotation(0F, 4F, -10F, headRot, 0F, 0F));

        root.addOrReplaceChild("ear1",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.45F, -12F, 4F, 2, 3, 1),
                PartPose.offsetAndRotation(0F, 4F, -10F, headRot, 0F, 0F));
        root.addOrReplaceChild("ear2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.45F, -12F, 4F, 2, 3, 1),
                PartPose.offsetAndRotation(0F, 4F, -10F, headRot, 0F, 0F));

        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2.05F, -9.8F, -2F, 4, 14, 8),
                PartPose.offsetAndRotation(0F, 4F, -10F, headRot, 0F, 0F));

        root.addOrReplaceChild("mane",
                CubeListBuilder.create().texOffs(58, 0).addBox(-1F, -11.5F, 5F, 2, 16, 4),
                PartPose.offsetAndRotation(0F, 4F, -10F, headRot, 0F, 0F));

        // Bat-horse membrane wings (legacy ButterflyL/R inherited from MoCModelNewHorse), shown for type 32.
        root.addOrReplaceChild("butterfly_l",
                CubeListBuilder.create().texOffs(0, 98).addBox(-1.0F, 0.0F, -14.0F, 26, 0, 30),
                PartPose.offsetAndRotation(4.5F, 3.0F, -2.0F, 0.0F, 0.0F, -0.78539F));
        root.addOrReplaceChild("butterfly_r",
                CubeListBuilder.create().texOffs(0, 68).addBox(-25.0F, 0.0F, -14.0F, 26, 0, 30),
                PartPose.offsetAndRotation(-4.5F, 3.0F, -2.0F, 0.0F, 0.0F, 0.78539F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float headYaw = state.yRot * DEG_TO_RAD;
        float headPitch = state.xRot * DEG_TO_RAD;

        // Legacy head-look tracking (clamped to +/-20 degrees of yaw in the original rig).
        float clampedYaw = Mth.clamp(headYaw, -20F * DEG_TO_RAD, 20F * DEG_TO_RAD);
        float baseHeadX = 0.5235988F + headPitch;

        this.head.xRot = baseHeadX;
        this.head.yRot = clampedYaw;
        this.uMouth.xRot = baseHeadX;
        this.uMouth.yRot = clampedYaw;
        this.lMouth.xRot = baseHeadX;
        this.lMouth.yRot = clampedYaw;
        this.unicorn.xRot = baseHeadX;
        this.unicorn.yRot = clampedYaw;
        this.ear1.xRot = baseHeadX;
        this.ear1.yRot = clampedYaw;
        this.ear2.xRot = baseHeadX;
        this.ear2.yRot = clampedYaw;
        this.neck.xRot = baseHeadX;
        this.neck.yRot = clampedYaw;
        this.mane.xRot = baseHeadX;
        this.mane.yRot = clampedYaw;

        // Quadruped walk gait, mirroring the legacy left/right leg phase split.
        float rLeg = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.8F * limbAmount;
        float lLeg = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;

        this.leg1A.xRot = lLeg;
        this.leg1B.xRot = lLeg;
        this.leg1C.xRot = lLeg;
        this.leg2A.xRot = rLeg;
        this.leg2B.xRot = rLeg;
        this.leg2C.xRot = rLeg;
        this.leg3A.xRot = rLeg;
        this.leg3B.xRot = rLeg;
        this.leg3C.xRot = rLeg;
        this.leg4A.xRot = lLeg;
        this.leg4B.xRot = lLeg;
        this.leg4C.xRot = lLeg;

        // Bat horse (type 32) spreads its membrane wings and beats them slowly (legacy flyer flapwings);
        // the other hostile horse variants (undead 23, skeleton 26) have none.
        boolean batHorse = state.typeMoC == 32;
        this.butterflyL.visible = batHorse;
        this.butterflyR.visible = batHorse;
        if (batHorse) {
            float flap = Mth.cos(state.ageInTicks * 0.5F) * 0.35F;
            this.butterflyL.zRot = -0.78539F - flap;
            this.butterflyR.zRot = 0.78539F + flap;
        }
    }
}
