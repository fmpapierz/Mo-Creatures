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
 * Dragonfly model, converted faithfully from the legacy {@code MoCModelDragonfly} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; the wing-flap and leg gait are ported from the legacy
 * {@code setRotationAngles}.
 */
public class MoCModelDragonfly extends EntityModel<MoCEntityRenderState> {

    // The dragonfly sheets paint only one tile of each zero-thickness box (wings/antennae: DOWN;
    // legs: NORTH). The model renders culled, so each such box is split into two single-face boxes
    // that both sample the painted tile (the second texOffs re-aims the opposite face onto the same
    // rect — see MoCModelHorse's membranes).
    private static final Set<Direction> FACE_DOWN = Set.of(Direction.DOWN);
    private static final Set<Direction> FACE_UP = Set.of(Direction.UP);
    private static final Set<Direction> FACE_NORTH = Set.of(Direction.NORTH);
    private static final Set<Direction> FACE_SOUTH = Set.of(Direction.SOUTH);

    private final ModelPart head;
    private final ModelPart rAntenna;
    private final ModelPart lAntenna;
    private final ModelPart mouth;
    private final ModelPart thorax;
    private final ModelPart abdomen;
    private final ModelPart frontLegs;
    private final ModelPart midLegs;
    private final ModelPart rearLegs;
    private final ModelPart wingFrontRight;
    private final ModelPart wingFrontLeft;
    private final ModelPart wingRearRight;
    private final ModelPart wingRearLeft;

    public MoCModelDragonfly(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.rAntenna = root.getChild("r_antenna");
        this.lAntenna = root.getChild("l_antenna");
        this.mouth = root.getChild("mouth");
        this.thorax = root.getChild("thorax");
        this.abdomen = root.getChild("abdomen");
        this.frontLegs = root.getChild("front_legs");
        this.midLegs = root.getChild("mid_legs");
        this.rearLegs = root.getChild("rear_legs");
        this.wingFrontRight = root.getChild("wing_front_right");
        this.wingFrontLeft = root.getChild("wing_front_left");
        this.wingRearRight = root.getChild("wing_rear_right");
        this.wingRearLeft = root.getChild("wing_rear_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 21.0F, -2.0F, -2.171231F, 0.0F, 0.0F));
        root.addOrReplaceChild("r_antenna",
                CubeListBuilder.create().texOffs(0, 7).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, FACE_DOWN)
                        .texOffs(-1, 7).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, FACE_UP),
                PartPose.offsetAndRotation(-0.5F, 19.7F, -2.3F, -1.041001F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("l_antenna",
                CubeListBuilder.create().texOffs(4, 7).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, FACE_DOWN)
                        .texOffs(3, 7).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, FACE_UP),
                PartPose.offsetAndRotation(0.5F, 19.7F, -2.3F, -1.041001F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 21.1F, -2.3F, -2.171231F, 0.0F, 0.0F));
        root.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 20.0F, -1.0F));
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 20.5F, 0.0F, 1.427659F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_NORTH)
                        .texOffs(-2, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 21.5F, -1.8F, 0.1487144F, 0.0F, 0.0F));
        root.addOrReplaceChild("mid_legs",
                CubeListBuilder.create().texOffs(4, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_NORTH)
                        .texOffs(2, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 22.0F, -1.2F, 0.5948578F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_legs",
                CubeListBuilder.create().texOffs(8, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_NORTH)
                        .texOffs(6, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 22.0F, -0.4F, 1.070744F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing_front_right",
                CubeListBuilder.create().texOffs(0, 28).addBox(-7.0F, 0.0F, -1.0F, 7.0F, 0.0F, 2.0F, FACE_DOWN)
                        .texOffs(-7, 28).addBox(-7.0F, 0.0F, -1.0F, 7.0F, 0.0F, 2.0F, FACE_UP),
                PartPose.offsetAndRotation(-1.0F, 20.0F, -1.0F, 0.0F, -0.1396263F, 0.0872665F));
        root.addOrReplaceChild("wing_front_left",
                CubeListBuilder.create().texOffs(0, 30).addBox(0.0F, 0.0F, -1.0F, 7.0F, 0.0F, 2.0F, FACE_DOWN)
                        .texOffs(-7, 30).addBox(0.0F, 0.0F, -1.0F, 7.0F, 0.0F, 2.0F, FACE_UP),
                PartPose.offsetAndRotation(1.0F, 20.0F, -1.0F, 0.0F, 0.1396263F, -0.0872665F));
        root.addOrReplaceChild("wing_rear_right",
                CubeListBuilder.create().texOffs(0, 24).addBox(-7.0F, 0.0F, -1.0F, 7.0F, 0.0F, 2.0F, FACE_DOWN)
                        .texOffs(-7, 24).addBox(-7.0F, 0.0F, -1.0F, 7.0F, 0.0F, 2.0F, FACE_UP),
                PartPose.offsetAndRotation(-1.0F, 20.0F, -1.0F, 0.0F, 0.3490659F, -0.0872665F));
        root.addOrReplaceChild("wing_rear_left",
                CubeListBuilder.create().texOffs(0, 26).addBox(0.0F, 0.0F, -1.0F, 7.0F, 0.0F, 2.0F, FACE_DOWN)
                        .texOffs(-7, 26).addBox(0.0F, 0.0F, -1.0F, 7.0F, 0.0F, 2.0F, FACE_UP),
                PartPose.offsetAndRotation(1.0F, 20.0F, -1.0F, 0.0F, -0.3490659F, 0.0872665F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;

        // Wings beat only while airborne (legacy: flying -> cos flap, grounded -> wings still).
        float wingRot = state.flying ? Mth.cos(ageInTicks * 2.0F) * 0.5F : 0.0F;

        this.wingFrontRight.zRot = wingRot;
        this.wingRearLeft.zRot = wingRot;
        this.wingFrontLeft.zRot = -wingRot;
        this.wingRearRight.zRot = -wingRot;

        // In flight the legs tuck into a faster flight pose; grounded they run the walk cosine gait.
        float legScale = state.flying ? 1.5F : 1.0F;
        float legMov = Mth.cos((limbSwing * 1.5F) + 3.141593F) * 2.0F * limbAmount * legScale;
        float legMovB = Mth.cos(limbSwing * 1.5F) * 2.0F * limbAmount * legScale;

        this.frontLegs.xRot = 0.1487144F + legMov;
        this.midLegs.xRot = 0.5948578F + legMovB;
        this.rearLegs.xRot = 1.070744F + legMov;
    }
}
