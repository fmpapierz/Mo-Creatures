package drzhark.mocreatures.client.model;

import java.util.Set;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Firefly model, converted faithfully from the legacy {@code MoCModelFirefly} ({@code ModelBase}).
 * Geometry and texture offsets are preserved. In flight ({@code state.flying}) the elytra open, the
 * membranous wings flap and the front legs tuck up; grounded, the shell is closed with no wings and the
 * legs run the walking gait — matching the legacy {@code isFlying} branch.
 */
public class MoCModelFirefly extends EntityModel<MoCEntityRenderState> {

    // firefly.png paints only one tile of each zero-thickness box (shells/wings: DOWN; antenna/legs:
    // NORTH). The model renders culled, so each such box is split into two single-face boxes that
    // both sample the painted tile (the second texOffs re-aims the opposite face onto the same
    // rect — see MoCModelHorse's membranes).
    private static final Set<Direction> FACE_DOWN = Set.of(Direction.DOWN);
    private static final Set<Direction> FACE_UP = Set.of(Direction.UP);
    private static final Set<Direction> FACE_NORTH = Set.of(Direction.NORTH);
    private static final Set<Direction> FACE_SOUTH = Set.of(Direction.SOUTH);

    private final ModelPart head;
    private final ModelPart antenna;
    private final ModelPart thorax;
    private final ModelPart abdomen;
    private final ModelPart tail;
    private final ModelPart frontLegs;
    private final ModelPart midLegs;
    private final ModelPart rearLegs;
    private final ModelPart rightShellOpen;
    private final ModelPart leftShellOpen;
    private final ModelPart rightShell;
    private final ModelPart leftShell;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public MoCModelFirefly(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.antenna = root.getChild("antenna");
        this.thorax = root.getChild("thorax");
        this.abdomen = root.getChild("abdomen");
        this.tail = root.getChild("tail");
        this.frontLegs = root.getChild("front_legs");
        this.midLegs = root.getChild("mid_legs");
        this.rearLegs = root.getChild("rear_legs");
        this.rightShellOpen = root.getChild("right_shell_open");
        this.leftShellOpen = root.getChild("left_shell_open");
        this.rightShell = root.getChild("right_shell");
        this.leftShell = root.getChild("left_shell");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 22.5F, -2.0F, -2.171231F, 0.0F, 0.0F));
        root.addOrReplaceChild("antenna",
                CubeListBuilder.create().texOffs(0, 7).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 1.0F, 0.0F, FACE_NORTH)
                        .texOffs(-2, 7).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 1.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 22.5F, -3.0F, -1.665602F, 0.0F, 0.0F));
        root.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 21.0F, -1.0F));
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 1.427659F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(8, 17).addBox(-1.0F, 0.5F, -1.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 21.3F, 1.5F, 1.13023F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 7).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, FACE_NORTH)
                        .texOffs(-2, 7).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 23.0F, -1.8F, -0.8328009F, 0.0F, 0.0F));
        root.addOrReplaceChild("mid_legs",
                CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, FACE_NORTH)
                        .texOffs(-2, 9).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 23.0F, -1.2F, 1.070744F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_legs",
                CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_NORTH)
                        .texOffs(-2, 9).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 23.0F, -0.4F, 1.249201F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_shell_open",
                CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_DOWN)
                        .texOffs(-2, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_UP),
                PartPose.offsetAndRotation(-1.0F, 21.0F, -2.0F, 1.22F, 0.0F, -0.6457718F));
        root.addOrReplaceChild("left_shell_open",
                CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_DOWN)
                        .texOffs(-2, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_UP),
                PartPose.offsetAndRotation(1.0F, 21.0F, -2.0F, 1.22F, 0.0F, 0.6457718F));
        root.addOrReplaceChild("right_shell",
                CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_DOWN)
                        .texOffs(-2, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_UP),
                PartPose.offsetAndRotation(-1.0F, 21.0F, -2.0F, 0.0174533F, 0.0F, -0.6457718F));
        root.addOrReplaceChild("left_shell",
                CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_DOWN)
                        .texOffs(-2, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_UP),
                PartPose.offsetAndRotation(1.0F, 21.0F, -2.0F, 0.0174533F, 0.0F, 0.6457718F));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(15, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_DOWN)
                        .texOffs(13, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_UP),
                PartPose.offsetAndRotation(1.0F, 21.0F, -1.0F, 0.0F, 1.047198F, 0.0F));
        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(15, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_DOWN)
                        .texOffs(13, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F, FACE_UP),
                PartPose.offsetAndRotation(-1.0F, 21.0F, -1.0F, 0.0F, -1.047198F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float f = state.walkAnimationPos;
        float f1 = state.walkAnimationSpeed;
        boolean flying = state.flying;

        // Shell/wing swap: open elytra + flapping wings while airborne, closed shell (no wings) grounded.
        this.rightShellOpen.visible = flying;
        this.leftShellOpen.visible = flying;
        this.leftWing.visible = flying;
        this.rightWing.visible = flying;
        this.rightShell.visible = !flying;
        this.leftShell.visible = !flying;

        if (flying) {
            // Membranous wings beat quickly; the front legs lift and the leg drive speeds up (legacy isFlying).
            float flap = Mth.cos(state.ageInTicks * 1.8F) * 0.8F;
            this.leftWing.zRot = 1.047198F + flap;
            this.rightWing.zRot = -1.047198F - flap;

            float legMov = Mth.cos((f * 1.5F) + 3.141593F) * 2.0F * f1 * 1.5F;
            float legMovB = Mth.cos(f * 1.5F) * 2.0F * f1 * 1.5F;
            this.frontLegs.xRot = -0.8328009F + 1.4F + legMov;
            this.midLegs.xRot = 1.070744F + legMovB;
            this.rearLegs.xRot = 1.249201F + legMov;
        } else {
            // grounded walking gait (legacy non-flying branch)
            float legMov = Mth.cos((f * 1.5F) + 3.141593F) * 2.0F * f1;
            float legMovB = Mth.cos(f * 1.5F) * 2.0F * f1;

            this.frontLegs.xRot = -0.8328009F + legMov;
            this.midLegs.xRot = 1.070744F + legMovB;
            this.rearLegs.xRot = 1.249201F + legMov;
        }
    }
}
