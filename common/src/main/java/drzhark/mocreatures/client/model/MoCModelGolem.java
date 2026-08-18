package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Golem model, converted faithfully from the legacy {@code MoCModelGolem} ({@code ModelBase}).
 *
 * <p>The legacy model rendered each body cube from a dynamic per-block texture atlas slot; this port
 * draws every cube from the single {@code golemt.png} texture and shows/hides each one from the synched
 * cube presence mask. Geometry and rotation points are preserved exactly; degree-based rotations from
 * the legacy {@code setRotationG} helper are converted to radians here.</p>
 *
 * <p>Pose logic (keyed off render-state flags): the head/chest swap to their angry UV skins when the
 * golem's state passes 1; the four front chest cubes splay open while the golem inhales a vacuumed rock;
 * the arms/legs swing forward during the rock-throwing windup; and the chest wobbles gently while the
 * golem is summoning (state 1).</p>
 */
public class MoCModelGolem extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = 1.0F / 57.29578F;

    private final ModelPart head;
    private final ModelPart chest;
    /** Angry (glowing-eye) head/chest skins, shown when golemState > 1 (legacy headb/chestb). */
    private final ModelPart headAngry;
    private final ModelPart chestAngry;
    private final ModelPart lChest1;
    private final ModelPart lChest2;
    private final ModelPart rChest1;
    private final ModelPart rChest2;
    private final ModelPart back;
    private final ModelPart lBack1;
    private final ModelPart lBack2;
    private final ModelPart rBack1;
    private final ModelPart rBack2;
    private final ModelPart lShoulder;
    private final ModelPart lArm;
    private final ModelPart lHand;
    private final ModelPart rShoulder;
    private final ModelPart rArm;
    private final ModelPart rHand;
    private final ModelPart lThigh;
    private final ModelPart lKnee;
    private final ModelPart lFoot;
    private final ModelPart rThigh;
    private final ModelPart rKnee;
    private final ModelPart rFoot;
    private final ModelPart groin;
    private final ModelPart butt;

    public MoCModelGolem(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.chest = root.getChild("chest");
        this.headAngry = root.getChild("head_angry");
        this.chestAngry = root.getChild("chest_angry");
        this.lChest1 = root.getChild("l_chest1");
        this.lChest2 = root.getChild("l_chest2");
        this.rChest1 = root.getChild("r_chest1");
        this.rChest2 = root.getChild("r_chest2");
        this.back = root.getChild("back");
        this.lBack1 = root.getChild("l_back1");
        this.lBack2 = root.getChild("l_back2");
        this.rBack1 = root.getChild("r_back1");
        this.rBack2 = root.getChild("r_back2");
        this.lShoulder = root.getChild("l_shoulder");
        this.lArm = root.getChild("l_arm");
        this.lHand = root.getChild("l_hand");
        this.rShoulder = root.getChild("r_shoulder");
        this.rArm = root.getChild("r_arm");
        this.rHand = root.getChild("r_hand");
        this.lThigh = root.getChild("l_thigh");
        this.lKnee = root.getChild("l_knee");
        this.lFoot = root.getChild("l_foot");
        this.rThigh = root.getChild("r_thigh");
        this.rKnee = root.getChild("r_knee");
        this.rFoot = root.getChild("r_foot");
        this.groin = root.getChild("groin");
        this.butt = root.getChild("butt");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // head (legacy setRotation(head, 0, 0.7853982, 0))
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(96, 64).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));

        // chest (legacy setRotation(chest, 0, 0.7853982, 0))
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(96, 96).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -7.0F, 0.0F, 0.7853982F, 0.0F));

        // angry head / chest — alternate UV skins (legacy headb 96,80 & chestb 96,112) shown when gState > 1
        root.addOrReplaceChild("head_angry",
                CubeListBuilder.create().texOffs(96, 80).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("chest_angry",
                CubeListBuilder.create().texOffs(96, 112).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -7.0F, 0.0F, 0.7853982F, 0.0F));

        // body cubes (legacy blocks[0..22]); setRotationG values are degrees -> radians
        root.addOrReplaceChild("l_chest1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 3.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -97.0F * DEG_TO_RAD, -40.0F * DEG_TO_RAD, 0.0F));
        root.addOrReplaceChild("l_chest2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 3.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -55.0F * DEG_TO_RAD, -41.0F * DEG_TO_RAD, 0.0F));
        root.addOrReplaceChild("r_chest1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 3.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -97.0F * DEG_TO_RAD, 40.0F * DEG_TO_RAD, 0.0F));
        root.addOrReplaceChild("r_chest2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 3.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -55.0F * DEG_TO_RAD, 41.0F * DEG_TO_RAD, 0.0F));
        root.addOrReplaceChild("back",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -14.0F, -1.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 3.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("l_back1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 3.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 1.919862F, 0.6981317F, 0.0F));
        root.addOrReplaceChild("l_back2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 3.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 1.183003F, 0.6981317F, 0.0F));
        root.addOrReplaceChild("r_back1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 3.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 1.919862F, -0.6981317F, 0.0F));
        root.addOrReplaceChild("r_back2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 3.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 1.183003F, -0.6981317F, 0.0F));
        root.addOrReplaceChild("l_shoulder",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -2.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(8.0F, -3.0F, 0.0F, 0.0F, 0.0F, -0.6981317F));
        root.addOrReplaceChild("l_arm",
                CubeListBuilder.create().texOffs(0, 0).addBox(2.0F, 4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(8.0F, -3.0F, 0.0F, 0.0F, 0.0F, -0.2094395F));
        root.addOrReplaceChild("l_hand",
                CubeListBuilder.create().texOffs(0, 0).addBox(4.5F, 11.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(8.0F, -3.0F, 0.0F));
        root.addOrReplaceChild("r_shoulder",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -2.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(-8.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.6981317F));
        root.addOrReplaceChild("r_arm",
                CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(-8.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.2094395F));
        root.addOrReplaceChild("r_hand",
                CubeListBuilder.create().texOffs(0, 0).addBox(-12.5F, 11.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(-8.0F, -3.0F, 0.0F));
        root.addOrReplaceChild("l_thigh",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(5.0F, 4.0F, 0.0F, -0.3490659F, 0.0F, 0.0F));
        root.addOrReplaceChild("l_knee",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 6.0F, -7.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(5.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("l_foot",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 12.0F, -5.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(5.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("r_thigh",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(-5.0F, 4.0F, 0.0F, -0.3490659F, 0.0F, 0.0F));
        root.addOrReplaceChild("r_knee",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 6.0F, -7.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(-5.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("r_foot",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 12.0F, -5.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(-5.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("groin",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 3.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("butt",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 3.0F, -0.7435722F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * Maps the 23 legacy cube slot indices to this model's part fields, in slot order. Slots:
     * 0-3 front chest, 4 valuable back cube, 5-8 back, 9-11 left arm, 12-14 right arm,
     * 15-17 left leg, 18-20 right leg, 21 groin, 22 butt. The head and central chest are not in
     * this array — they stay always visible so a stripped golem still has a head.
     */
    public ModelPart[] cubeParts() {
        return new ModelPart[] {
                this.lChest1, this.lChest2, this.rChest1, this.rChest2,   // 0-3
                this.back,                                                 // 4
                this.lBack1, this.lBack2, this.rBack1, this.rBack2,        // 5-8
                this.lShoulder, this.lArm, this.lHand,                     // 9-11
                this.rShoulder, this.rArm, this.rHand,                     // 12-14
                this.lThigh, this.lKnee, this.lFoot,                       // 15-17
                this.rThigh, this.rKnee, this.rFoot,                       // 18-20
                this.groin, this.butt                                      // 21-22
        };
    }

    /**
     * Centre of each cube's 8x8x8 box in its part-local frame, in model pixels (box origin + 4 on each
     * axis), matching the {@code addBox} offsets in {@link #createBodyLayer}. Used by the per-block cube
     * renderer to place a real block model at the same spot the {@code golemt.png} cube occupied.
     */
    public static final float[][] CUBE_BOX_CENTERS = {
            {0.0F, 7.0F, 0.0F}, {0.0F, 7.0F, 0.0F}, {0.0F, 7.0F, 0.0F}, {0.0F, 7.0F, 0.0F}, // 0-3 chest
            {-3.0F, -10.0F, 3.0F},                                                          // 4 back (valuable)
            {0.0F, 7.0F, 0.0F}, {0.0F, 7.0F, 0.0F}, {0.0F, 7.0F, 0.0F}, {0.0F, 7.0F, 0.0F}, // 5-8 back
            {4.0F, 2.0F, 0.0F}, {6.0F, 8.0F, 0.0F}, {8.5F, 15.0F, 0.0F},                    // 9-11 left arm
            {-4.0F, 2.0F, 0.0F}, {-6.0F, 8.0F, 0.0F}, {-8.5F, 15.0F, 0.0F},                 // 12-14 right arm
            {0.5F, 4.0F, 0.0F}, {0.0F, 10.0F, -3.0F}, {0.5F, 16.0F, -1.0F},                 // 15-17 left leg
            {-0.5F, 4.0F, 0.0F}, {0.0F, 10.0F, -3.0F}, {-0.5F, 16.0F, -1.0F},               // 18-20 right leg
            {4.0F, 0.0F, -4.0F}, {0.0F, 0.0F, 0.0F}                                         // 21-22 groin, butt
    };

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        // The per-block cube layer (MoCGolemRenderer) draws each present cube as its real absorbed block,
        // so the golemt.png cube geometry itself is hidden here to avoid double-rendering. The cubes are
        // still animated below (openChest splay / throwing swing) because the block layer reads their
        // ModelPart transforms to position each block.
        ModelPart[] cubes = cubeParts();
        for (ModelPart cube : cubes) {
            cube.visible = false;
        }

        // Angry variant: the golem shows a glowing-eye head/chest once its state passes 1 (legacy
        // gState > 1). Exactly one of the normal / angry head-and-chest pair is ever visible.
        boolean angry = state.golemState > 1;
        this.head.visible = !angry;
        this.chest.visible = !angry;
        this.headAngry.visible = angry;
        this.chestAngry.visible = angry;

        boolean openChest = state.golemOpenChest;
        boolean summoning = state.golemState == 1;
        boolean throwing = state.golemThrowing;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float headYaw = state.yRot;
        float ageInTicks = state.ageInTicks;

        // legs swing in opposite phase (legacy RLeg/LLeg gait)
        float rLegXRot = Mth.cos((limbSwing * 0.6662F) + 3.141593F) * 1.2F * limbAmount;
        float lLegXRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbAmount;
        float rArmZRot = -(Mth.cos(ageInTicks * 0.09F) * 0.05F) + 0.05F;
        float lArmZRot = (Mth.cos(ageInTicks * 0.09F) * 0.05F) - 0.05F;

        float headY = (45.0F + headYaw) * DEG_TO_RAD;
        this.head.yRot = headY;
        this.headAngry.yRot = headY;

        // Summoning wobble: the chest sways gently while the golem rebuilds (legacy isSummoning branch).
        float chestY = summoning ? (45.0F * DEG_TO_RAD) + (Mth.cos(ageInTicks * 0.09F) * 0.15F)
                                 : 45.0F * DEG_TO_RAD;
        this.chest.yRot = chestY;
        this.chestAngry.yRot = chestY;

        // Open chest: the four front chest cubes splay outward and the chest slides back (legacy openChest).
        float chestZ = openChest ? -7.0F : -4.0F;
        this.chest.z = chestZ;
        this.chestAngry.z = chestZ;
        this.lChest1.yRot = (openChest ? -60.0F : -40.0F) * DEG_TO_RAD;
        this.lChest2.yRot = (openChest ? -55.0F : -41.0F) * DEG_TO_RAD;
        this.rChest1.yRot = (openChest ? 60.0F : 40.0F) * DEG_TO_RAD;
        this.rChest2.yRot = (openChest ? 55.0F : 41.0F) * DEG_TO_RAD;

        this.lThigh.xRot = (-20.0F * DEG_TO_RAD) + lLegXRot;
        this.lKnee.xRot = lLegXRot;
        this.lFoot.xRot = lLegXRot;
        this.rThigh.xRot = (-20.0F * DEG_TO_RAD) + rLegXRot;
        this.rKnee.xRot = rLegXRot;
        this.rFoot.xRot = rLegXRot;

        // Throwing windup: arms and legs swing forward together (legacy tcounter > 25 branch).
        if (throwing) {
            lLegXRot = -90.0F * DEG_TO_RAD;
            rLegXRot = -90.0F * DEG_TO_RAD;
            rArmZRot = 0.0F;
            lArmZRot = 0.0F;
        }

        this.rShoulder.zRot = (40.0F * DEG_TO_RAD) + rArmZRot;
        this.rShoulder.xRot = lLegXRot;
        this.rArm.zRot = (12.0F * DEG_TO_RAD) + rArmZRot;
        this.rArm.xRot = lLegXRot;
        this.rHand.zRot = rArmZRot;
        this.rHand.xRot = lLegXRot;

        this.lShoulder.zRot = (-40.0F * DEG_TO_RAD) + lArmZRot;
        this.lShoulder.xRot = rLegXRot;
        this.lArm.zRot = (-12.0F * DEG_TO_RAD) + lArmZRot;
        this.lArm.xRot = rLegXRot;
        this.lHand.zRot = lArmZRot;
        this.lHand.xRot = rLegXRot;
    }
}
