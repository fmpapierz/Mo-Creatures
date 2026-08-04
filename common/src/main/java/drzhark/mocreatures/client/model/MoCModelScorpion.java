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
 * Scorpion model, converted faithfully from the legacy {@code MoCModelScorpion} ({@code ModelBase}).
 * Geometry, texture offsets and the eight-legged gait are preserved. The poisoning-tail pose is driven
 * by the synched {@code scorpionAttacking} flag: the tail whips forward for the sting strike. The pincer
 * arms have their full four-segment claws (larm/rarm 1-4) with an idle claw-flick jitter and a forward
 * claw-lunge driven by {@code attackSwing}. The legacy on-back baby cluster (baby1..baby5) is rendered
 * with its per-baby wiggle when {@code scorpionHasBabies} is set.
 */
public class MoCModelScorpion extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart head;
    private final ModelPart mouthL;
    private final ModelPart mouthR;
    private final ModelPart larm1;
    private final ModelPart larm2;
    private final ModelPart larm3;
    private final ModelPart larm4;
    private final ModelPart rarm1;
    private final ModelPart rarm2;
    private final ModelPart rarm3;
    private final ModelPart rarm4;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart tail4;
    private final ModelPart tail5;
    private final ModelPart sting1;
    private final ModelPart sting2;
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
    private final ModelPart leg5A;
    private final ModelPart leg5B;
    private final ModelPart leg5C;
    private final ModelPart leg6A;
    private final ModelPart leg6B;
    private final ModelPart leg6C;
    private final ModelPart leg7A;
    private final ModelPart leg7B;
    private final ModelPart leg7C;
    private final ModelPart leg8A;
    private final ModelPart leg8B;
    private final ModelPart leg8C;
    private final ModelPart baby1;
    private final ModelPart baby2;
    private final ModelPart baby3;
    private final ModelPart baby4;
    private final ModelPart baby5;

    public MoCModelScorpion(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.mouthL = root.getChild("mouth_l");
        this.mouthR = root.getChild("mouth_r");
        this.larm1 = root.getChild("larm1");
        this.larm2 = root.getChild("larm2");
        this.larm3 = root.getChild("larm3");
        this.larm4 = root.getChild("larm4");
        this.rarm1 = root.getChild("rarm1");
        this.rarm2 = root.getChild("rarm2");
        this.rarm3 = root.getChild("rarm3");
        this.rarm4 = root.getChild("rarm4");
        this.tail1 = root.getChild("tail1");
        this.tail2 = root.getChild("tail2");
        this.tail3 = root.getChild("tail3");
        this.tail4 = root.getChild("tail4");
        this.tail5 = root.getChild("tail5");
        this.sting1 = root.getChild("sting1");
        this.sting2 = root.getChild("sting2");
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
        this.leg5A = root.getChild("leg5a");
        this.leg5B = root.getChild("leg5b");
        this.leg5C = root.getChild("leg5c");
        this.leg6A = root.getChild("leg6a");
        this.leg6B = root.getChild("leg6b");
        this.leg6C = root.getChild("leg6c");
        this.leg7A = root.getChild("leg7a");
        this.leg7B = root.getChild("leg7b");
        this.leg7C = root.getChild("leg7c");
        this.leg8A = root.getChild("leg8a");
        this.leg8B = root.getChild("leg8b");
        this.leg8C = root.getChild("leg8c");
        this.baby1 = root.getChild("baby1");
        this.baby2 = root.getChild("baby2");
        this.baby3 = root.getChild("baby3");
        this.baby4 = root.getChild("baby4");
        this.baby5 = root.getChild("baby5");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 0F, 0F, 10, 5, 13),
                PartPose.offset(0F, 14F, -9F));
        root.addOrReplaceChild("mouth_l",
                CubeListBuilder.create().texOffs(18, 58).addBox(-3F, -2F, -1F, 4, 4, 2),
                PartPose.offsetAndRotation(3F, 17F, -9F, 0F, -0.3839724F, 0F));
        root.addOrReplaceChild("mouth_r",
                CubeListBuilder.create().texOffs(30, 58).addBox(-1F, -2F, -1F, 4, 4, 2),
                PartPose.offsetAndRotation(-3F, 17F, -9F, 0F, 0.3839724F, 0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 18).addBox(-4F, -2F, 0F, 8, 4, 10),
                PartPose.offsetAndRotation(0F, 17F, 3F, 0.0872665F, 0F, 0F));
        root.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(0, 32).addBox(-3F, -2F, 0F, 6, 4, 6),
                PartPose.offsetAndRotation(0F, 16F, 12F, 0.6108652F, 0F, 0F));
        root.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(0, 42).addBox(-2F, -2F, 0F, 4, 4, 6),
                PartPose.offsetAndRotation(0F, 13F, 16.5F, 1.134464F, 0F, 0F));
        root.addOrReplaceChild("tail3",
                CubeListBuilder.create().texOffs(0, 52).addBox(-1.5F, -1.5F, 0F, 3, 3, 6),
                PartPose.offsetAndRotation(0F, 8F, 18.5F, 1.692143F, 0F, 0F));
        root.addOrReplaceChild("tail4",
                CubeListBuilder.create().texOffs(24, 32).addBox(-1.5F, -1.5F, 0F, 3, 3, 6),
                PartPose.offsetAndRotation(0F, 3F, 18F, 2.510073F, 0F, 0F));
        root.addOrReplaceChild("tail5",
                CubeListBuilder.create().texOffs(24, 41).addBox(-1.5F, -1.5F, 0F, 3, 3, 6),
                PartPose.offsetAndRotation(0F, -0.2F, 14F, 3.067752F, 0F, 0F));
        root.addOrReplaceChild("sting1",
                CubeListBuilder.create().texOffs(30, 50).addBox(-1.5F, 0F, -1.5F, 3, 5, 3),
                PartPose.offsetAndRotation(0F, -1F, 7F, 0.4089647F, 0F, 0F));
        root.addOrReplaceChild("sting2",
                CubeListBuilder.create().texOffs(26, 50).addBox(-0.5F, 0F, 0.5F, 1, 4, 1),
                PartPose.offsetAndRotation(0F, 2.6F, 8.8F, -0.2230717F, 0F, 0F));

        root.addOrReplaceChild("larm1",
                CubeListBuilder.create().texOffs(26, 18).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 18F, -8F, -0.3490659F, 0F, 0.8726646F));
        root.addOrReplaceChild("larm2",
                CubeListBuilder.create().texOffs(42, 55).addBox(-1.5F, -1.5F, -6F, 3, 3, 6),
                PartPose.offsetAndRotation(10F, 14F, -6F, 0.1745329F, -0.3490659F, -0.2617994F));
        root.addOrReplaceChild("larm3",
                CubeListBuilder.create().texOffs(42, 39).addBox(-0.5F, -0.5F, -7F, 2, 1, 7),
                PartPose.offsetAndRotation(12F, 15F, -11F, 0.2617994F, 0.1570796F, -0.1570796F));
        root.addOrReplaceChild("larm4",
                CubeListBuilder.create().texOffs(42, 31).addBox(-1.5F, -0.5F, -6F, 1, 1, 7),
                PartPose.offsetAndRotation(11F, 15F, -11F, 0.2617994F, 0F, -0.1570796F));

        root.addOrReplaceChild("rarm1",
                CubeListBuilder.create().texOffs(0, 18).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 18F, -8F, -0.3490659F, 0F, -0.8726646F));
        root.addOrReplaceChild("rarm2",
                CubeListBuilder.create().texOffs(42, 55).addBox(-1.5F, -1.5F, -6F, 3, 3, 6),
                PartPose.offsetAndRotation(-10F, 14F, -6F, 0.1745329F, 0.3490659F, 0.2617994F));
        root.addOrReplaceChild("rarm3",
                CubeListBuilder.create().texOffs(42, 47).addBox(-1.5F, -0.5F, -7F, 2, 1, 7),
                PartPose.offsetAndRotation(-12F, 15F, -11F, 0.2617994F, -0.1570796F, 0.1570796F));
        root.addOrReplaceChild("rarm4",
                CubeListBuilder.create().texOffs(42, 31).addBox(0.5F, -0.5F, -6F, 1, 1, 7),
                PartPose.offsetAndRotation(-11F, 15F, -11F, 0.2617994F, 0F, 0.1570796F));

        root.addOrReplaceChild("leg1a",
                CubeListBuilder.create().texOffs(38, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 18F, -5F, -10F * DEG_TO_RAD, 0F, 75F * DEG_TO_RAD));
        root.addOrReplaceChild("leg1b",
                CubeListBuilder.create().texOffs(50, 0).addBox(2F, -8F, -1F, 5, 2, 2),
                PartPose.offsetAndRotation(5F, 18F, -5F, -10F * DEG_TO_RAD, 0F, 60F * DEG_TO_RAD));
        root.addOrReplaceChild("leg1c",
                CubeListBuilder.create().texOffs(52, 16).addBox(4.5F, -9F, -0.7F, 5, 1, 1),
                PartPose.offsetAndRotation(5F, 18F, -5F, -10F * DEG_TO_RAD, 0F, 75F * DEG_TO_RAD));

        root.addOrReplaceChild("leg2a",
                CubeListBuilder.create().texOffs(38, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 18F, -2F, -30F * DEG_TO_RAD, 0F, 70F * DEG_TO_RAD));
        root.addOrReplaceChild("leg2b",
                CubeListBuilder.create().texOffs(50, 4).addBox(1F, -8F, -1F, 5, 2, 2),
                PartPose.offsetAndRotation(5F, 18F, -2F, -30F * DEG_TO_RAD, 0F, 60F * DEG_TO_RAD));
        root.addOrReplaceChild("leg2c",
                CubeListBuilder.create().texOffs(50, 18).addBox(4F, -8.5F, -1F, 6, 1, 1),
                PartPose.offsetAndRotation(5F, 18F, -2F, -30F * DEG_TO_RAD, 0F, 70F * DEG_TO_RAD));

        root.addOrReplaceChild("leg3a",
                CubeListBuilder.create().texOffs(38, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 17.5F, 1F, -45F * DEG_TO_RAD, 0F, 70F * DEG_TO_RAD));
        root.addOrReplaceChild("leg3b",
                CubeListBuilder.create().texOffs(48, 8).addBox(1F, -8F, -1F, 6, 2, 2),
                PartPose.offsetAndRotation(5F, 17.5F, 1F, -45F * DEG_TO_RAD, 0F, 60F * DEG_TO_RAD));
        root.addOrReplaceChild("leg3c",
                CubeListBuilder.create().texOffs(50, 20).addBox(4.5F, -8.2F, -1.3F, 6, 1, 1),
                PartPose.offsetAndRotation(5F, 17.5F, 1F, -45F * DEG_TO_RAD, 0F, 70F * DEG_TO_RAD));

        root.addOrReplaceChild("leg4a",
                CubeListBuilder.create().texOffs(38, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(5F, 17F, 4F, -60F * DEG_TO_RAD, 0F, 70F * DEG_TO_RAD));
        root.addOrReplaceChild("leg4b",
                CubeListBuilder.create().texOffs(46, 12).addBox(0.5F, -8.5F, -1F, 7, 2, 2),
                PartPose.offsetAndRotation(5F, 17F, 4F, -60F * DEG_TO_RAD, 0F, 60F * DEG_TO_RAD));
        root.addOrReplaceChild("leg4c",
                CubeListBuilder.create().texOffs(48, 22).addBox(3.5F, -8.5F, -1.5F, 7, 1, 1),
                PartPose.offsetAndRotation(5F, 17F, 4F, -60F * DEG_TO_RAD, 0F, 70F * DEG_TO_RAD));

        root.addOrReplaceChild("leg5a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 18F, -5F, -10F * DEG_TO_RAD, 0F, -75F * DEG_TO_RAD));
        root.addOrReplaceChild("leg5b",
                CubeListBuilder.create().texOffs(50, 0).addBox(-7F, -8F, -1F, 5, 2, 2),
                PartPose.offsetAndRotation(-5F, 18F, -5F, -10F * DEG_TO_RAD, 0F, -60F * DEG_TO_RAD));
        root.addOrReplaceChild("leg5c",
                CubeListBuilder.create().texOffs(52, 16).addBox(-9.5F, -9F, -0.7F, 5, 1, 1),
                PartPose.offsetAndRotation(-5F, 18F, -5F, -10F * DEG_TO_RAD, 0F, -75F * DEG_TO_RAD));

        root.addOrReplaceChild("leg6a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 18F, -2F, -30F * DEG_TO_RAD, 0F, -70F * DEG_TO_RAD));
        root.addOrReplaceChild("leg6b",
                CubeListBuilder.create().texOffs(50, 4).addBox(-6F, -8F, -1F, 5, 2, 2),
                PartPose.offsetAndRotation(-5F, 18F, -2F, -30F * DEG_TO_RAD, 0F, -60F * DEG_TO_RAD));
        root.addOrReplaceChild("leg6c",
                CubeListBuilder.create().texOffs(50, 18).addBox(-10F, -8.5F, -1F, 6, 1, 1),
                PartPose.offsetAndRotation(-5F, 18F, -2F, -30F * DEG_TO_RAD, 0F, -60F * DEG_TO_RAD));

        root.addOrReplaceChild("leg7a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 17.5F, 1F, -45F * DEG_TO_RAD, 0F, -70F * DEG_TO_RAD));
        root.addOrReplaceChild("leg7b",
                CubeListBuilder.create().texOffs(48, 8).addBox(-7F, -8.5F, -1F, 6, 2, 2),
                PartPose.offsetAndRotation(-5F, 17.5F, 1F, -45F * DEG_TO_RAD, 0F, -60F * DEG_TO_RAD));
        root.addOrReplaceChild("leg7c",
                CubeListBuilder.create().texOffs(50, 20).addBox(-10.5F, -8.7F, -1.3F, 6, 1, 1),
                PartPose.offsetAndRotation(-5F, 17.5F, 1F, -45F * DEG_TO_RAD, 0F, -70F * DEG_TO_RAD));

        root.addOrReplaceChild("leg8a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -7F, -1F, 2, 7, 2),
                PartPose.offsetAndRotation(-5F, 17F, 4F, -60F * DEG_TO_RAD, 0F, -70F * DEG_TO_RAD));
        root.addOrReplaceChild("leg8b",
                CubeListBuilder.create().texOffs(46, 12).addBox(-7.5F, -8.5F, -1F, 7, 2, 2),
                PartPose.offsetAndRotation(-5F, 17F, 4F, -60F * DEG_TO_RAD, 0F, -60F * DEG_TO_RAD));
        root.addOrReplaceChild("leg8c",
                CubeListBuilder.create().texOffs(48, 22).addBox(-10.5F, -8.5F, -1.5F, 7, 1, 1),
                PartPose.offsetAndRotation(-5F, 17F, 4F, -60F * DEG_TO_RAD, 0F, -70F * DEG_TO_RAD));

        // On-back baby cluster (rendered only when a female carries young). Legacy MoCModelScorpion baby1..baby5:
        // same cube (texOffs 48,24; box -1.5,0,-2.5,3,2,5) placed at five points on the mother's back with the
        // legacy resting rotations (already radians in the legacy setRotation calls).
        root.addOrReplaceChild("baby1",
                CubeListBuilder.create().texOffs(48, 24).addBox(-1.5F, 0F, -2.5F, 3, 2, 5),
                PartPose.offset(0F, 12F, 0F));
        root.addOrReplaceChild("baby2",
                CubeListBuilder.create().texOffs(48, 24).addBox(-1.5F, 0F, -2.5F, 3, 2, 5),
                PartPose.offsetAndRotation(-5F, 13.4F, -1F, 0.4461433F, 2.490967F, 0.5205006F));
        root.addOrReplaceChild("baby3",
                CubeListBuilder.create().texOffs(48, 24).addBox(-1.5F, 0F, -2.5F, 3, 2, 5),
                PartPose.offsetAndRotation(-2F, 13F, 4F, 0F, 0.8551081F, 0F));
        root.addOrReplaceChild("baby4",
                CubeListBuilder.create().texOffs(48, 24).addBox(-1.5F, 0F, -2.5F, 3, 2, 5),
                PartPose.offsetAndRotation(4F, 13F, 2F, 0F, 2.714039F, -0.3717861F));
        root.addOrReplaceChild("baby5",
                CubeListBuilder.create().texOffs(48, 24).addBox(-1.5F, 0F, -2.5F, 3, 2, 5),
                PartPose.offsetAndRotation(1F, 13F, 8F, 0F, -1.189716F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float headYaw = state.yRot * DEG_TO_RAD;
        float headPitch = state.xRot * DEG_TO_RAD;
        this.head.yRot = headYaw;
        this.head.xRot = headPitch;

        // mouth pincers fixed splay (legacy 22 degrees out)
        this.mouthR.yRot = 22F * DEG_TO_RAD;
        this.mouthL.yRot = -22F * DEG_TO_RAD;

        // --- Pincer arms -------------------------------------------------------------------------------
        // Rest pose: upper arm angled back (legacy -20 deg). During a melee swing the whole pincer lunges
        // forward at the prey. The legacy strike was a 4-phase armCounter (L open, L closed, R open, R
        // closed); we don't have the counter here, so we approximate it with a single symmetric lunge
        // scaled by the swing progress (attackSwing 0..1).
        float claw = state.attackSwing;                 // 0 resting .. 1 fully lunged
        // upper arm rocks forward from -20 deg toward ~ +70 deg over the swing
        float armX = (-20F + 90F * claw) * DEG_TO_RAD;
        this.larm1.xRot = armX;
        this.rarm1.xRot = armX;

        // segment lunge: shove the pincer forearms/claws forward (toward -Z) and tuck them in as they close
        float lungeZ = -6F * claw;                       // pixels forward
        float lungeY = 1.5F * claw;                      // slight rise
        this.larm2.z = -6F + lungeZ;
        this.larm2.y = 14F + lungeY;
        this.larm3.z = -11F + lungeZ;
        this.larm3.y = 15F + lungeY;
        this.larm4.z = -11F + lungeZ;
        this.larm4.y = 15F + lungeY;
        this.rarm2.z = -6F + lungeZ;
        this.rarm2.y = 14F + lungeY;
        this.rarm3.z = -11F + lungeZ;
        this.rarm3.y = 15F + lungeY;
        this.rarm4.z = -11F + lungeZ;
        this.rarm4.y = 15F + lungeY;

        // claw pincers: open (splay apart) at mid-lunge, then snap shut. sin(pi*claw) peaks at claw=0.5.
        float clawSplay = Mth.sin((float) Math.PI * claw) * (40F * DEG_TO_RAD);
        if (claw > 0.001F) {
            this.larm4.yRot = clawSplay;
            this.rarm4.yRot = -clawSplay;
            this.larm3.yRot = 9F * DEG_TO_RAD;
            this.rarm3.yRot = -9F * DEG_TO_RAD;
        } else {
            // idle hand jitter — the legacy random flick of the claws driven by the age timer (f2 % window).
            float age = state.ageInTicks;
            float lHand = 0F;
            float f2a = age % 100F;
            if (f2a > 0F && f2a < 20F) {
                lHand = f2a * DEG_TO_RAD;
            }
            this.larm3.yRot = (9F * DEG_TO_RAD) - lHand;
            this.larm4.yRot = lHand;

            float rHand = 0F;
            float f2b = age % 75F;
            if (f2b > 30F && f2b < 50F) {
                rHand = (f2b - 29F) * DEG_TO_RAD;
            }
            this.rarm3.yRot = (-9F * DEG_TO_RAD) + rHand;
            this.rarm4.yRot = -rHand;
        }

        // --- On-back baby cluster ---------------------------------------------------------------------
        // Only shown when the mother is carrying young. Each baby wiggles on its own seeded rest angle,
        // cos-modulated by the age timer in short windows (legacy fb1..fb5 = 0/142/49/155/-68 deg).
        boolean babies = state.scorpionHasBabies;
        this.baby1.visible = babies;
        this.baby2.visible = babies;
        this.baby3.visible = babies;
        this.baby4.visible = babies;
        this.baby5.visible = babies;
        if (babies) {
            float age = state.ageInTicks;
            float fmov = age % 100F;
            float fb1 = 0F;
            float fb2 = 142F * DEG_TO_RAD;
            float fb3 = 49F * DEG_TO_RAD;
            float fb4 = 155F * DEG_TO_RAD;
            float fb5 = -68F * DEG_TO_RAD;

            if (fmov > 0F && fmov < 20F) {
                fb2 -= (Mth.cos(age * 0.8F) * 0.3F);
                fb3 -= (Mth.cos(age * 0.6F) * 0.2F);
                fb1 += (Mth.cos(age * 0.4F) * 0.4F);
                fb5 += (Mth.cos(age * 0.7F) * 0.5F);
            }
            if (fmov > 30F && fmov < 50F) {
                fb4 -= (Mth.cos(age * 0.8F) * 0.4F);
                fb1 += (Mth.cos(age * 0.7F) * 0.1F);
                fb3 -= (Mth.cos(age * 0.6F) * 0.2F);
            }
            if (fmov > 80F) {
                fb5 += (Mth.cos(age * 0.2F) * 0.4F);
                fb2 -= (Mth.cos(age * 0.6F) * 0.3F);
                fb4 -= (Mth.cos(age * 0.4F) * 0.2F);
            }
            this.baby1.yRot = fb1;
            this.baby2.yRot = fb2;
            this.baby3.yRot = fb3;
            this.baby4.yRot = fb4;
            this.baby5.yRot = fb5;
        }

        // Tail / stinger: normally held in the arched resting curl, but during a sting the whole tail whips
        // up and forward over the body so the stinger jabs down at the prey (legacy poisoning-tail pose).
        float strike = state.scorpionAttacking ? 1.0F : 0.0F;
        this.tail1.xRot = 0.6108652F - (0.35F * strike);
        this.tail2.xRot = 1.134464F - (0.55F * strike);
        this.tail3.xRot = 1.692143F - (0.80F * strike);
        this.tail4.xRot = 2.510073F - (1.05F * strike);
        this.tail5.xRot = 3.067752F - (1.25F * strike);
        this.sting1.xRot = 0.4089647F - (1.35F * strike);
        this.sting2.xRot = -0.2230717F - (0.6F * strike);

        float f = state.walkAnimationPos;
        float f1 = state.walkAnimationSpeed;

        // floats used for the scorpion's eight-legged gait
        float f9 = -(Mth.cos(f * 0.6662F * 2.0F + 0.0F) * 0.4F) * f1;
        float f10 = -(Mth.cos(f * 0.6662F * 2.0F + 3.141593F) * 0.4F) * f1;
        float f11 = -(Mth.cos(f * 0.6662F * 2.0F + 1.570796F) * 0.4F) * f1;
        float f12 = -(Mth.cos(f * 0.6662F * 2.0F + 4.712389F) * 0.4F) * f1;
        float f13 = Math.abs(Mth.sin(f * 0.6662F + 0.0F) * 0.4F) * f1;
        float f14 = Math.abs(Mth.sin(f * 0.6662F + 3.141593F) * 0.4F) * f1;
        float f15 = Math.abs(Mth.sin(f * 0.6662F + 1.570796F) * 0.4F) * f1;
        float f16 = Math.abs(Mth.sin(f * 0.6662F + 4.712389F) * 0.4F) * f1;

        this.leg1A.xRot = -10F * DEG_TO_RAD;
        this.leg1A.zRot = 75F * DEG_TO_RAD;
        this.leg1B.zRot = 60F * DEG_TO_RAD;
        this.leg1C.zRot = 75F * DEG_TO_RAD;
        this.leg1A.xRot += f9;
        this.leg1B.xRot = this.leg1A.xRot;
        this.leg1C.xRot = this.leg1A.xRot;
        this.leg1A.zRot += f13;
        this.leg1B.zRot += f13;
        this.leg1C.zRot += f13;

        this.leg2A.xRot = -30F * DEG_TO_RAD;
        this.leg2A.zRot = 70F * DEG_TO_RAD;
        this.leg2B.zRot = 60F * DEG_TO_RAD;
        this.leg2C.zRot = 70F * DEG_TO_RAD;
        this.leg2A.xRot += f10;
        this.leg2B.xRot = this.leg2A.xRot;
        this.leg2C.xRot = this.leg2A.xRot;
        this.leg2A.zRot += f14;
        this.leg2B.zRot += f14;
        this.leg2C.zRot += f14;

        this.leg3A.xRot = -45F * DEG_TO_RAD;
        this.leg3A.zRot = 70F * DEG_TO_RAD;
        this.leg3B.zRot = 60F * DEG_TO_RAD;
        this.leg3C.zRot = 70F * DEG_TO_RAD;
        this.leg3A.xRot += f11;
        this.leg3B.xRot = this.leg3A.xRot;
        this.leg3C.xRot = this.leg3A.xRot;
        this.leg3A.zRot += f15;
        this.leg3B.zRot += f15;
        this.leg3C.zRot += f15;

        this.leg4A.xRot = -60F * DEG_TO_RAD;
        this.leg4A.zRot = 70F * DEG_TO_RAD;
        this.leg4B.zRot = 60F * DEG_TO_RAD;
        this.leg4C.zRot = 70F * DEG_TO_RAD;
        this.leg4A.xRot += f12;
        this.leg4B.xRot = this.leg4A.xRot;
        this.leg4C.xRot = this.leg4A.xRot;
        this.leg4A.zRot += f16;
        this.leg4B.zRot += f16;
        this.leg4C.zRot += f16;

        this.leg5A.xRot = -10F * DEG_TO_RAD;
        this.leg5A.zRot = -75F * DEG_TO_RAD;
        this.leg5B.zRot = -60F * DEG_TO_RAD;
        this.leg5C.zRot = -75F * DEG_TO_RAD;
        this.leg5A.xRot -= f9;
        this.leg5B.xRot = this.leg5A.xRot;
        this.leg5C.xRot = this.leg5A.xRot;
        this.leg5A.zRot -= f13;
        this.leg5B.zRot -= f13;
        this.leg5C.zRot -= f13;

        this.leg6A.xRot = -30F * DEG_TO_RAD;
        this.leg6A.zRot = -70F * DEG_TO_RAD;
        this.leg6B.zRot = -60F * DEG_TO_RAD;
        this.leg6C.zRot = -70F * DEG_TO_RAD;
        this.leg6A.xRot -= f10;
        this.leg6B.xRot = this.leg6A.xRot;
        this.leg6C.xRot = this.leg6A.xRot;
        this.leg6A.zRot -= f14;
        this.leg6B.zRot -= f14;
        this.leg6C.zRot -= f14;

        this.leg7A.xRot = -45F * DEG_TO_RAD;
        this.leg7A.zRot = -70F * DEG_TO_RAD;
        this.leg7B.zRot = -60F * DEG_TO_RAD;
        this.leg7C.zRot = -70F * DEG_TO_RAD;
        this.leg7A.xRot -= f11;
        this.leg7B.xRot = this.leg7A.xRot;
        this.leg7C.xRot = this.leg7A.xRot;
        this.leg7A.zRot -= f15;
        this.leg7B.zRot -= f15;
        this.leg7C.zRot -= f15;

        this.leg8A.xRot = -60F * DEG_TO_RAD;
        this.leg8A.zRot = -70F * DEG_TO_RAD;
        this.leg8B.zRot = -60F * DEG_TO_RAD;
        this.leg8C.zRot = -70F * DEG_TO_RAD;
        this.leg8A.xRot -= f12;
        this.leg8B.xRot = this.leg8A.xRot;
        this.leg8C.xRot = this.leg8A.xRot;
        this.leg8A.zRot -= f16;
        this.leg8B.zRot -= f16;
        this.leg8C.zRot -= f16;
    }
}
