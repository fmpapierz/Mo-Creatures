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
 * Roach model, converted faithfully from the legacy {@code MoCModelRoach} ({@code ModelBase}).
 * Geometry and texture offsets are preserved. In flight ({@code state.flying}) the elytra splay open and
 * the membranous wings beat; grounded, the shell stays closed with no wings — matching the legacy
 * {@code isFlying} shell/wing swap. The walking leg gait and antenna sway are kept.
 */
public class MoCModelRoach extends EntityModel<MoCEntityRenderState> {

    private static final float RADIAN_F = 57.29578F;

    private final ModelPart head;
    private final ModelPart lAnthenna;
    private final ModelPart rAnthenna;
    private final ModelPart thorax;
    private final ModelPart frontLegs;
    private final ModelPart midLegs;
    private final ModelPart rearLegs;
    private final ModelPart abdomen;
    private final ModelPart tailL;
    private final ModelPart tailR;
    private final ModelPart lShellClosed;
    private final ModelPart rShellClosed;
    private final ModelPart lShellOpen;
    private final ModelPart rShellOpen;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public MoCModelRoach(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.lAnthenna = this.head.getChild("l_anthenna");
        this.rAnthenna = this.head.getChild("r_anthenna");
        this.thorax = root.getChild("thorax");
        this.frontLegs = root.getChild("front_legs");
        this.midLegs = root.getChild("mid_legs");
        this.rearLegs = root.getChild("rear_legs");
        this.abdomen = root.getChild("abdomen");
        this.tailL = root.getChild("tail_l");
        this.tailR = root.getChild("tail_r");
        this.lShellClosed = root.getChild("l_shell_closed");
        this.rShellClosed = root.getChild("r_shell_closed");
        this.lShellOpen = root.getChild("l_shell_open");
        this.rShellOpen = root.getChild("r_shell_open");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 0F, -1F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0F, 23F, -2F, -2.171231F, 0F, 0F));

        PartDefinition lAnthenna = head.addOrReplaceChild("l_anthenna",
                CubeListBuilder.create().texOffs(3, 21).addBox(0F, 0F, 0F, 4.0F, 0.0F, 1.0F),
                PartPose.offsetAndRotation(0.5F, 0F, 0F, -90F / RADIAN_F, 0.4363323F, 0F));
        lAnthenna.addOrReplaceChild("l_anthenna_b",
                CubeListBuilder.create().texOffs(4, 21).addBox(0F, 0F, 1F, 3.0F, 0.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, 0F, -0.5F, 0F, 45F / RADIAN_F, 0F));

        PartDefinition rAnthenna = head.addOrReplaceChild("r_anthenna",
                CubeListBuilder.create().texOffs(3, 19).addBox(-4.5F, 0F, 0F, 4.0F, 0.0F, 1.0F),
                PartPose.offsetAndRotation(0F, 0F, 0F, -90F / RADIAN_F, -0.4363323F, 0F));
        rAnthenna.addOrReplaceChild("r_anthenna_b",
                CubeListBuilder.create().texOffs(4, 19).addBox(-4.0F, 0F, 1F, 3.0F, 0.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, 0F, 0.5F, 0F, -45F / RADIAN_F, 0F));

        root.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 3).addBox(-1F, 0F, -1F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0F, 22F, -1F));

        root.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 11).addBox(-2F, 0F, 0F, 4.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0F, 23F, -1.8F, -1.115358F, 0F, 0F));

        root.addOrReplaceChild("mid_legs",
                CubeListBuilder.create().texOffs(0, 13).addBox(-2.5F, 0F, 0F, 5.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0F, 23F, -1.2F, 1.264073F, 0F, 0F));

        root.addOrReplaceChild("rear_legs",
                CubeListBuilder.create().texOffs(0, 15).addBox(-2F, 0F, 0F, 4.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(0F, 23F, -0.4F, 1.368173F, 0F, 0F));

        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(0, 6).addBox(-1F, 0F, -1F, 2.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0F, 22F, 0F, 1.427659F, 0F, 0F));

        root.addOrReplaceChild("tail_l",
                CubeListBuilder.create().texOffs(2, 29).addBox(-0.5F, 0F, 0F, 1.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0F, 23F, 3.6F, 1.554066F, 0.6457718F, 0F));

        root.addOrReplaceChild("tail_r",
                CubeListBuilder.create().texOffs(0, 29).addBox(-0.5F, 0F, 0F, 1.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0F, 23F, 3.6F, 1.554066F, -0.6457718F, 0F));

        root.addOrReplaceChild("l_shell_closed",
                CubeListBuilder.create().texOffs(4, 23).addBox(0F, 0F, 0F, 2.0F, 0.0F, 6.0F),
                PartPose.offsetAndRotation(0F, 21.5F, -1.5F, -0.1487144F, -0.0872665F, 0.1919862F));

        root.addOrReplaceChild("r_shell_closed",
                CubeListBuilder.create().texOffs(0, 23).addBox(-2F, 0F, 0F, 2.0F, 0.0F, 6.0F),
                PartPose.offsetAndRotation(0F, 21.5F, -1.5F, -0.1487144F, 0.0872665F, -0.1919862F));

        // Open elytra + membranous wings, shown only when the roach is airborne (legacy LShellOpen/RShellOpen
        // + LeftWing/RightWing). Same texture regions as the closed shells, splayed open.
        root.addOrReplaceChild("l_shell_open",
                CubeListBuilder.create().texOffs(4, 23).addBox(0F, 0F, 0F, 2.0F, 0.0F, 6.0F),
                PartPose.offsetAndRotation(0F, 21.5F, -1.5F, 1.117011F, -0.0872665F, 1.047198F));
        root.addOrReplaceChild("r_shell_open",
                CubeListBuilder.create().texOffs(0, 23).addBox(-2F, 0F, 0F, 2.0F, 0.0F, 6.0F),
                PartPose.offsetAndRotation(0F, 21.5F, -1.5F, 1.117011F, 0.0872665F, -1.047198F));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(11, 21).mirror().addBox(0F, 1F, -1F, 6, 0, 2),
                PartPose.offsetAndRotation(0F, 21.5F, -1.5F, 0F, -1.047198F, -0.4363323F));
        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(11, 19).addBox(-6F, 1F, -1F, 6, 0, 2),
                PartPose.offsetAndRotation(0F, 21.5F, -1.5F, 0F, 1.047198F, 0.4363323F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float f = state.walkAnimationPos;
        float f1 = state.walkAnimationSpeed;
        float f4 = state.xRot;

        this.head.xRot = -2.171231F + (f4 / RADIAN_F);

        float antMov = 5F / RADIAN_F + (f1 * 1.5F);
        this.lAnthenna.zRot = -antMov;
        this.rAnthenna.zRot = antMov;

        float legMov = Mth.cos((f * 1.5F) + 3.141593F) * 0.6F * f1;
        float legMovB = Mth.cos(f * 1.5F) * 0.8F * f1;

        this.frontLegs.xRot = -1.115358F + legMov;
        this.midLegs.xRot = 1.264073F + legMovB;
        this.rearLegs.xRot = 1.368173F + legMov;

        // In flight the elytra open and the wings beat; grounded the shell is closed with no wings
        // (legacy isFlying = getIsFlying() || isOnAir()).
        boolean flying = state.flying;
        this.lShellClosed.visible = !flying;
        this.rShellClosed.visible = !flying;
        this.lShellOpen.visible = flying;
        this.rShellOpen.visible = flying;
        this.leftWing.visible = flying;
        this.rightWing.visible = flying;
        if (flying) {
            float flap = Mth.cos(state.ageInTicks * 2.0F) * 0.7F;
            this.leftWing.zRot = -0.4363323F - flap;
            this.rightWing.zRot = 0.4363323F + flap;
        }
    }
}
