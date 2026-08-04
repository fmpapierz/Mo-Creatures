package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Horse model, converted faithfully from the legacy {@code MoCModelNewHorse} ({@code ModelBase}).
 * Every cube, texture offset and rotation point is preserved. The legacy model carried a great deal
 * of state-driven animation (flying, standing, eating, saddles, butterfly/pegasus wings); the
 * modern render state only exposes basic walk and look data, so {@link #setupAnim} reproduces the
 * leg gait and head tracking and leaves the special-case poses at their static rest values.
 *
 * <p><b>Equipment.</b> In MoCreatures the horse's protective armour is a full-texture swap (the
 * armoured look lives entirely in the {@code horsearmor*} texture variants), not extra geometry &mdash;
 * the legacy model contains <em>no</em> armour cubes, so there is nothing here to gate on
 * {@code state.horseArmor}; the texture swap handles that on its own. The only model-level equipment
 * is the saddle group (saddle pad, girth straps, stirrups, bit and head-stall), which the legacy
 * {@code render()} drew only when the horse was saddled. {@link #setupAnim} reproduces that by
 * toggling the saddle parts' {@code visible} from {@code state.saddled}.
 */
public class MoCModelHorse extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart uMouth;
    private final ModelPart lMouth;
    private final ModelPart uMouth2;
    private final ModelPart lMouth2;
    private final ModelPart unicorn;
    private final ModelPart ear1;
    private final ModelPart ear2;
    private final ModelPart muleEarL;
    private final ModelPart muleEarR;
    private final ModelPart neck;
    private final ModelPart headSaddle;
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

    private final ModelPart bag1;
    private final ModelPart bag2;

    private final ModelPart saddle;
    private final ModelPart saddleB;
    private final ModelPart saddleC;
    private final ModelPart saddleL;
    private final ModelPart saddleL2;
    private final ModelPart saddleR;
    private final ModelPart saddleR2;
    private final ModelPart saddleMouthL;
    private final ModelPart saddleMouthR;
    private final ModelPart saddleMouthLine;
    private final ModelPart saddleMouthLineR;

    private final ModelPart midWing;
    private final ModelPart innerWing;
    private final ModelPart outerWing;
    private final ModelPart innerWingR;
    private final ModelPart midWingR;
    private final ModelPart outerWingR;

    private final ModelPart butterflyL;
    private final ModelPart butterflyR;

    public MoCModelHorse(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.uMouth = root.getChild("u_mouth");
        this.lMouth = root.getChild("l_mouth");
        this.uMouth2 = root.getChild("u_mouth2");
        this.lMouth2 = root.getChild("l_mouth2");
        this.unicorn = root.getChild("unicorn");
        this.ear1 = root.getChild("ear1");
        this.ear2 = root.getChild("ear2");
        this.muleEarL = root.getChild("mule_ear_l");
        this.muleEarR = root.getChild("mule_ear_r");
        this.neck = root.getChild("neck");
        this.headSaddle = root.getChild("head_saddle");
        this.mane = root.getChild("mane");

        this.body = root.getChild("body");
        this.tailA = root.getChild("tail_a");
        this.tailB = root.getChild("tail_b");
        this.tailC = root.getChild("tail_c");

        this.leg1A = root.getChild("leg1a");
        this.leg1B = root.getChild("leg1b");
        this.leg1C = root.getChild("leg1c");
        this.leg2A = root.getChild("leg2a");
        this.leg2B = root.getChild("leg2b");
        this.leg2C = root.getChild("leg2c");
        this.leg3A = root.getChild("leg3a");
        this.leg3B = root.getChild("leg3b");
        this.leg3C = root.getChild("leg3c");
        this.leg4A = root.getChild("leg4a");
        this.leg4B = root.getChild("leg4b");
        this.leg4C = root.getChild("leg4c");

        this.bag1 = root.getChild("bag1");
        this.bag2 = root.getChild("bag2");

        this.saddle = root.getChild("saddle");
        this.saddleB = root.getChild("saddle_b");
        this.saddleC = root.getChild("saddle_c");
        this.saddleL = root.getChild("saddle_l");
        this.saddleL2 = root.getChild("saddle_l2");
        this.saddleR = root.getChild("saddle_r");
        this.saddleR2 = root.getChild("saddle_r2");
        this.saddleMouthL = root.getChild("saddle_mouth_l");
        this.saddleMouthR = root.getChild("saddle_mouth_r");
        this.saddleMouthLine = root.getChild("saddle_mouth_line");
        this.saddleMouthLineR = root.getChild("saddle_mouth_line_r");

        this.midWing = root.getChild("mid_wing");
        this.innerWing = root.getChild("inner_wing");
        this.outerWing = root.getChild("outer_wing");
        this.innerWingR = root.getChild("inner_wing_r");
        this.midWingR = root.getChild("mid_wing_r");
        this.outerWingR = root.getChild("outer_wing_r");

        this.butterflyL = root.getChild("butterfly_l");
        this.butterflyR = root.getChild("butterfly_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 34).addBox(-5.0F, -8.0F, -19.0F, 10.0F, 10.0F, 24.0F),
                PartPose.offset(0.0F, 11.0F, 9.0F));

        root.addOrReplaceChild("tail_a",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 14.0F, -1.134464F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_b",
                CubeListBuilder.create().texOffs(38, 7).addBox(-1.5F, -2.0F, 3.0F, 3.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 14.0F, -1.134464F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_c",
                CubeListBuilder.create().texOffs(24, 3).addBox(-1.5F, -4.5F, 9.0F, 3.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 14.0F, -1.40215F, 0.0F, 0.0F));

        root.addOrReplaceChild("leg1a",
                CubeListBuilder.create().texOffs(78, 29).addBox(-2.5F, -2.0F, -2.5F, 4.0F, 9.0F, 5.0F),
                PartPose.offset(4.0F, 9.0F, 11.0F));
        root.addOrReplaceChild("leg1b",
                CubeListBuilder.create().texOffs(78, 43).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(4.0F, 16.0F, 11.0F));
        root.addOrReplaceChild("leg1c",
                CubeListBuilder.create().texOffs(78, 51).addBox(-2.5F, 5.1F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(4.0F, 16.0F, 11.0F));

        root.addOrReplaceChild("leg2a",
                CubeListBuilder.create().texOffs(96, 29).addBox(-1.5F, -2.0F, -2.5F, 4.0F, 9.0F, 5.0F),
                PartPose.offset(-4.0F, 9.0F, 11.0F));
        root.addOrReplaceChild("leg2b",
                CubeListBuilder.create().texOffs(96, 43).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(-4.0F, 16.0F, 11.0F));
        root.addOrReplaceChild("leg2c",
                CubeListBuilder.create().texOffs(96, 51).addBox(-1.5F, 5.1F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(-4.0F, 16.0F, 11.0F));

        root.addOrReplaceChild("leg3a",
                CubeListBuilder.create().texOffs(44, 29).addBox(-1.9F, -1.0F, -2.1F, 3.0F, 8.0F, 4.0F),
                PartPose.offset(4.0F, 9.0F, -8.0F));
        root.addOrReplaceChild("leg3b",
                CubeListBuilder.create().texOffs(44, 41).addBox(-1.9F, 0.0F, -1.6F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(4.0F, 16.0F, -8.0F));
        root.addOrReplaceChild("leg3c",
                CubeListBuilder.create().texOffs(44, 51).addBox(-2.4F, 5.1F, -2.1F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(4.0F, 16.0F, -8.0F));

        root.addOrReplaceChild("leg4a",
                CubeListBuilder.create().texOffs(60, 29).addBox(-1.1F, -1.0F, -2.1F, 3.0F, 8.0F, 4.0F),
                PartPose.offset(-4.0F, 9.0F, -8.0F));
        root.addOrReplaceChild("leg4b",
                CubeListBuilder.create().texOffs(60, 41).addBox(-1.1F, 0.0F, -1.6F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(-4.0F, 16.0F, -8.0F));
        root.addOrReplaceChild("leg4c",
                CubeListBuilder.create().texOffs(60, 51).addBox(-1.6F, 5.1F, -2.1F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(-4.0F, 16.0F, -8.0F));

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -10.0F, -1.5F, 5.0F, 5.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("u_mouth",
                CubeListBuilder.create().texOffs(24, 18).addBox(-2.0F, -10.0F, -7.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("l_mouth",
                CubeListBuilder.create().texOffs(24, 27).addBox(-2.0F, -7.0F, -6.5F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("u_mouth2",
                CubeListBuilder.create().texOffs(24, 18).addBox(-2.0F, -10.0F, -8.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("l_mouth2",
                CubeListBuilder.create().texOffs(24, 27).addBox(-2.0F, -7.0F, -5.5F, 4.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.7853982F, 0.0F, 0.0F));

        root.addOrReplaceChild("unicorn",
                CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -18.0F, 2.0F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("ear1",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.45F, -12.0F, 4.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("ear2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.45F, -12.0F, 4.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("mule_ear_l",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, -16.0F, 4.0F, 2.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.2617994F));
        root.addOrReplaceChild("mule_ear_r",
                CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, -16.0F, 4.0F, 2.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, -0.2617994F));

        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2.05F, -9.8F, -2.0F, 4.0F, 14.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("bag1",
                CubeListBuilder.create().texOffs(0, 34).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(-7.5F, 3.0F, 10.0F, 0.0F, 1.570796F, 0.0F));
        root.addOrReplaceChild("bag2",
                CubeListBuilder.create().texOffs(0, 47).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(4.5F, 3.0F, 10.0F, 0.0F, 1.570796F, 0.0F));

        root.addOrReplaceChild("saddle",
                CubeListBuilder.create().texOffs(80, 0).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 2.0F, 2.0F));
        root.addOrReplaceChild("saddle_b",
                CubeListBuilder.create().texOffs(106, 9).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 2.0F, 2.0F));
        root.addOrReplaceChild("saddle_c",
                CubeListBuilder.create().texOffs(80, 9).addBox(-4.0F, -1.0F, 3.0F, 8.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 2.0F, 2.0F));

        root.addOrReplaceChild("saddle_l2",
                CubeListBuilder.create().texOffs(74, 0).addBox(-0.5F, 6.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(5.0F, 3.0F, 2.0F));
        root.addOrReplaceChild("saddle_l",
                CubeListBuilder.create().texOffs(70, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offset(5.0F, 3.0F, 2.0F));
        root.addOrReplaceChild("saddle_r2",
                CubeListBuilder.create().texOffs(74, 4).addBox(-0.5F, 6.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(-5.0F, 3.0F, 2.0F));
        root.addOrReplaceChild("saddle_r",
                CubeListBuilder.create().texOffs(80, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offset(-5.0F, 3.0F, 2.0F));

        root.addOrReplaceChild("saddle_mouth_l",
                CubeListBuilder.create().texOffs(74, 13).addBox(1.5F, -8.0F, -4.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("saddle_mouth_r",
                CubeListBuilder.create().texOffs(74, 13).addBox(-2.5F, -8.0F, -4.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("saddle_mouth_line",
                CubeListBuilder.create().texOffs(44, 10).addBox(2.6F, -6.0F, -6.0F, 0.0F, 3.0F, 16.0F),
                PartPose.offset(0.0F, 4.0F, -10.0F));
        root.addOrReplaceChild("saddle_mouth_line_r",
                CubeListBuilder.create().texOffs(44, 5).addBox(-2.6F, -6.0F, -6.0F, 0.0F, 3.0F, 16.0F),
                PartPose.offset(0.0F, 4.0F, -10.0F));

        root.addOrReplaceChild("mane",
                CubeListBuilder.create().texOffs(58, 0).addBox(-1.0F, -11.5F, 5.0F, 2.0F, 16.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("head_saddle",
                CubeListBuilder.create().texOffs(80, 12).addBox(-2.5F, -10.1F, -7.0F, 5.0F, 5.0F, 12.0F, new CubeDeformation(0.2F)),
                PartPose.offsetAndRotation(0.0F, 4.0F, -10.0F, 0.5235988F, 0.0F, 0.0F));

        root.addOrReplaceChild("mid_wing",
                CubeListBuilder.create().texOffs(82, 68).addBox(1.0F, 0.1F, 1.0F, 12.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(5.0F, 3.0F, -6.0F, 0.0F, 0.0872665F, 0.0F));
        root.addOrReplaceChild("inner_wing",
                CubeListBuilder.create().texOffs(0, 96).addBox(0.0F, 0.0F, 0.0F, 7.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(5.0F, 3.0F, -6.0F, 0.0F, -0.3490659F, 0.0F));
        root.addOrReplaceChild("outer_wing",
                CubeListBuilder.create().texOffs(0, 68).addBox(0.0F, 0.0F, 0.0F, 22.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(17.0F, 3.0F, -6.0F, 0.0F, -0.3228859F, 0.0F));
        root.addOrReplaceChild("inner_wing_r",
                CubeListBuilder.create().texOffs(0, 110).addBox(-7.0F, 0.0F, 0.0F, 7.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(-5.0F, 3.0F, -6.0F, 0.0F, 0.3490659F, 0.0F));
        root.addOrReplaceChild("mid_wing_r",
                CubeListBuilder.create().texOffs(82, 82).addBox(-13.0F, 0.1F, 1.0F, 12.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(-5.0F, 3.0F, -6.0F, 0.0F, -0.0872665F, 0.0F));
        root.addOrReplaceChild("outer_wing_r",
                CubeListBuilder.create().texOffs(0, 82).addBox(-22.0F, 0.0F, 0.0F, 22.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(-17.0F, 3.0F, -6.0F, 0.0F, 0.3228859F, 0.0F));

        root.addOrReplaceChild("butterfly_l",
                CubeListBuilder.create().texOffs(0, 98).addBox(-1.0F, 0.0F, -14.0F, 26.0F, 0.0F, 30.0F),
                PartPose.offsetAndRotation(4.5F, 3.0F, -2.0F, 0.0F, 0.0F, -0.78539F));
        root.addOrReplaceChild("butterfly_r",
                CubeListBuilder.create().texOffs(0, 68).addBox(-25.0F, 0.0F, -14.0F, 26.0F, 0.0F, 30.0F),
                PartPose.offsetAndRotation(-4.5F, 3.0F, -2.0F, 0.0F, 0.0F, 0.78539F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float headYaw = state.yRot;
        float headPitch = state.xRot;

        if (headYaw > 20.0F) {
            headYaw = 20.0F;
        }
        if (headYaw < -20.0F) {
            headYaw = -20.0F;
        }

        // legs: alternating cosine gait (faithful to legacy RLegXRot / LLegXRot)
        float rLegXRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.8F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbAmount;

        this.leg1A.xRot = lLegXRot;
        this.leg1B.xRot = lLegXRot;
        this.leg1C.xRot = lLegXRot;
        this.leg2A.xRot = rLegXRot;
        this.leg2B.xRot = rLegXRot;
        this.leg2C.xRot = rLegXRot;
        this.leg3A.xRot = rLegXRot;
        this.leg3B.xRot = rLegXRot;
        this.leg3C.xRot = rLegXRot;
        this.leg4A.xRot = lLegXRot;
        this.leg4B.xRot = lLegXRot;
        this.leg4C.xRot = lLegXRot;

        // Rearing: the front legs (leg3/leg4, at the front of the body) tuck up and forward as the horse
        // stands on its hind legs; the whole body is pitched back by the renderer (see MoCMobRenderer).
        if (state.horseRearing) {
            this.leg3A.xRot = -1.2F;
            this.leg3B.xRot = -1.4F;
            this.leg3C.xRot = -1.4F;
            this.leg4A.xRot = -1.2F;
            this.leg4B.xRot = -1.4F;
            this.leg4C.xRot = -1.4F;
        }

        // head + attached parts track look direction (legacy base rotation = 30 degrees pitch); a grazing
        // horse drives its nose to the ground, a rearing horse throws its head up (legacy eating/standing).
        float headXRot;
        float headYRot = headYaw * DEG_TO_RAD;
        if (state.horseEating) {
            headXRot = 2.0F;   // nose lowered to graze
            headYRot = 0.0F;
        } else if (state.horseRearing) {
            headXRot = -0.35F; // head flung up as it rears
        } else {
            headXRot = 0.5235988F + (headPitch * DEG_TO_RAD);
            // Idle shuffle: a standing horse gently bobs its head (legacy shuffling head-bob).
            if (limbAmount < 0.05F) {
                headXRot += Mth.cos(state.ageInTicks * 0.09F) * 0.05F;
            }
        }

        this.head.xRot = headXRot;
        this.head.yRot = headYRot;
        this.ear1.xRot = headXRot;
        this.ear1.yRot = headYRot;
        this.ear2.xRot = headXRot;
        this.ear2.yRot = headYRot;
        this.muleEarL.xRot = headXRot;
        this.muleEarL.yRot = headYRot;
        this.muleEarR.xRot = headXRot;
        this.muleEarR.yRot = headYRot;
        this.neck.xRot = headXRot;
        this.neck.yRot = headYRot;
        this.mane.xRot = headXRot;
        this.mane.yRot = headYRot;
        this.unicorn.xRot = headXRot;
        this.unicorn.yRot = headYRot;
        this.uMouth.xRot = headXRot;
        this.uMouth.yRot = headYRot;
        this.lMouth.xRot = headXRot;
        this.lMouth.yRot = headYRot;
        this.uMouth2.xRot = headXRot - 0.0872664F;
        this.uMouth2.yRot = headYRot;
        this.lMouth2.xRot = headXRot + 0.261799F;
        this.lMouth2.yRot = headYRot;

        // The saddle group (pad, girth straps, stirrups, bit, head-stall and reins) only renders when
        // the horse is wearing a saddle. Armour, by contrast, is a full-texture swap with no geometry,
        // so there is nothing to toggle for state.horseArmor here.
        // The saddlebags (bag1 / bag2) show once the horse is fitted with a chest (bagger-horse storage,
        // legacy getIsChested) — MoCEntityHorse.hasChest() drives state.hasChest.
        this.bag1.visible = state.hasChest;
        this.bag2.visible = state.hasChest;

        boolean saddled = state.saddled;
        this.saddle.visible = saddled;
        this.saddleB.visible = saddled;
        this.saddleC.visible = saddled;
        this.saddleL.visible = saddled;
        this.saddleL2.visible = saddled;
        this.saddleR.visible = saddled;
        this.saddleR2.visible = saddled;
        this.saddleMouthL.visible = saddled;
        this.saddleMouthR.visible = saddled;
        this.saddleMouthLine.visible = saddled;
        this.saddleMouthLineR.visible = saddled;
        this.headSaddle.visible = saddled;

        // Wings render only on flyer types (pegasus / fairy / bat / ghost / undead-pegasus) and the unicorn
        // horn only on unicorned types (unicorn / fairy / undead-unicorn / unicorn-skeleton) — computed from
        // the synched type so a regular horse never shows garbled wing/horn geometry from its texture.
        int type = state.typeMoC;
        boolean flyer = type == 39 || type == 40 || (type >= 45 && type < 60) || type == 32
                || type == 21 || type == 25 || type == 28;
        boolean unicorned = type == 36 || (type >= 45 && type < 60) || type == 27 || type == 24;
        this.midWing.visible = flyer;
        this.innerWing.visible = flyer;
        this.outerWing.visible = flyer;
        this.innerWingR.visible = flyer;
        this.midWingR.visible = flyer;
        this.outerWingR.visible = flyer;
        this.unicorn.visible = unicorned;

        // Wing flap. The wing segments (inner / mid / outer for each side) are all direct children of
        // root — they are flat, not nested — and their rest PartPose carries only a yRot sweep (the fan
        // shape) with zero zRot. The wings extend sideways along the X axis, so the axis that raises and
        // lowers a wing is zRot: a negative zRot lifts the left wing's outboard tip (toward +X) upward,
        // and the mirrored positive zRot lifts the right. We leave yRot alone (preserving the baked fan)
        // and drive zRot only.
        //
        // When airborne the wings spread (a base zRot offset) and beat with a cosine cycle; the inner,
        // mid and outer segments get progressively larger, phase-shifted amplitudes so the whole wing
        // articulates rather than swinging rigidly. When grounded we restore the folded rest pose by
        // clearing zRot back to the layer's baked value of 0.
        if (state.flying) {
            float flap = Mth.cos(state.ageInTicks * 0.5F) * 0.5F;
            float spread = 20.0F * DEG_TO_RAD;

            // Left wing (extends toward +X): negative zRot lifts it up.
            this.innerWing.zRot = -(spread + flap);
            this.midWing.zRot = -(spread + flap * 1.35F);
            this.outerWing.zRot = -(spread + Mth.cos(state.ageInTicks * 0.5F + 0.6F) * 0.5F * 1.7F);

            // Right wing (extends toward -X): mirror the sign.
            this.innerWingR.zRot = spread + flap;
            this.midWingR.zRot = spread + flap * 1.35F;
            this.outerWingR.zRot = spread + Mth.cos(state.ageInTicks * 0.5F + 0.6F) * 0.5F * 1.7F;
        } else {
            // Folded rest pose (baked layer zRot is 0 on every wing segment).
            this.innerWing.zRot = 0.0F;
            this.midWing.zRot = 0.0F;
            this.outerWing.zRot = 0.0F;
            this.innerWingR.zRot = 0.0F;
            this.midWingR.zRot = 0.0F;
            this.outerWingR.zRot = 0.0F;
        }
    }
}
