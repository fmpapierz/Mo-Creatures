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
 * Butterfly model, converted faithfully from the legacy {@code MoCModelButterfly}
 * ({@code ModelBase}). Geometry and texture offsets are preserved; the wing-flap and leg gait
 * are mapped onto the modern render state.
 */
public class MoCModelButterfly extends EntityModel<MoCEntityRenderState> {

    // The bf*/moth* sheets paint only one tile of each zero-thickness box (wings/antennae: DOWN;
    // legs: NORTH). The model renders culled, so each such box is split into two single-face boxes
    // that both sample the painted tile (the second texOffs re-aims the opposite face onto the same
    // rect — see MoCModelHorse's membranes). The mouth (both tiles painted) stays a plain box.
    private static final Set<Direction> FACE_DOWN = Set.of(Direction.DOWN);
    private static final Set<Direction> FACE_UP = Set.of(Direction.UP);
    private static final Set<Direction> FACE_NORTH = Set.of(Direction.NORTH);
    private static final Set<Direction> FACE_SOUTH = Set.of(Direction.SOUTH);

    private final ModelPart abdomen;
    private final ModelPart frontLegs;
    private final ModelPart rightAntenna;
    private final ModelPart leftAntenna;
    private final ModelPart rearLegs;
    private final ModelPart midLegs;
    private final ModelPart head;
    private final ModelPart thorax;
    private final ModelPart mouth;
    private final ModelPart wingRight;
    private final ModelPart wingLeft;
    private final ModelPart wingLeftFront;
    private final ModelPart wingRightFront;
    private final ModelPart wingRightBack;
    private final ModelPart wingLeftBack;

    public MoCModelButterfly(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.abdomen = root.getChild("abdomen");
        this.frontLegs = root.getChild("front_legs");
        this.rightAntenna = root.getChild("right_antenna");
        this.leftAntenna = root.getChild("left_antenna");
        this.rearLegs = root.getChild("rear_legs");
        this.midLegs = root.getChild("mid_legs");
        this.head = root.getChild("head");
        this.thorax = root.getChild("thorax");
        this.mouth = root.getChild("mouth");
        this.wingRight = root.getChild("wing_right");
        this.wingLeft = root.getChild("wing_left");
        this.wingLeftFront = root.getChild("wing_left_front");
        this.wingRightFront = root.getChild("wing_right_front");
        this.wingRightBack = root.getChild("wing_right_back");
        this.wingLeftBack = root.getChild("wing_left_back");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 21.9F, -1.3F, -2.171231F, 0.0F, 0.0F));
        root.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(0, 8).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(-0.2F, 22.0F, -2.5F, 0.6548599F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_antenna",
                CubeListBuilder.create().texOffs(0, 7).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, FACE_DOWN)
                        .texOffs(-1, 7).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, FACE_UP),
                PartPose.offsetAndRotation(-0.5F, 21.7F, -2.3F, -1.041001F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("left_antenna",
                CubeListBuilder.create().texOffs(4, 7).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, FACE_DOWN)
                        .texOffs(3, 7).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, FACE_UP),
                PartPose.offsetAndRotation(0.5F, 21.7F, -2.3F, -1.041001F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 1.5F, -1.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 20.0F, -1.0F));
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(8, 1).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 21.5F, 0.0F, 1.427659F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_NORTH)
                        .texOffs(-2, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 21.5F, -1.8F, 0.1487144F, 0.0F, 0.0F));
        root.addOrReplaceChild("mid_legs",
                CubeListBuilder.create().texOffs(4, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_NORTH)
                        .texOffs(2, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 22.0F, -1.2F, 0.5948578F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_legs",
                CubeListBuilder.create().texOffs(0, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_NORTH)
                        .texOffs(-2, 8).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, FACE_SOUTH),
                PartPose.offsetAndRotation(0.0F, 22.5F, -0.4F, 1.070744F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing_left_front",
                CubeListBuilder.create().texOffs(4, 20).addBox(0.0F, 0.0F, -4.0F, 8.0F, 0.0F, 6.0F, FACE_DOWN)
                        .texOffs(-4, 20).addBox(0.0F, 0.0F, -4.0F, 8.0F, 0.0F, 6.0F, FACE_UP),
                PartPose.offset(0.3F, 21.4F, -1.0F));
        root.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(4, 26).addBox(0.0F, 0.0F, -1.0F, 8.0F, 0.0F, 6.0F, FACE_DOWN)
                        .texOffs(-4, 26).addBox(0.0F, 0.0F, -1.0F, 8.0F, 0.0F, 6.0F, FACE_UP),
                PartPose.offset(0.3F, 21.5F, -0.5F));
        root.addOrReplaceChild("wing_left_back",
                CubeListBuilder.create().texOffs(4, 0).addBox(0.0F, 0.0F, -1.0F, 5.0F, 0.0F, 8.0F, FACE_DOWN)
                        .texOffs(-1, 0).addBox(0.0F, 0.0F, -1.0F, 5.0F, 0.0F, 8.0F, FACE_UP),
                PartPose.offsetAndRotation(0.3F, 21.2F, -1.0F, 0.0F, 0.0F, 0.5934119F));
        root.addOrReplaceChild("wing_right_front",
                CubeListBuilder.create().texOffs(4, 8).addBox(-8.0F, 0.0F, -4.0F, 8.0F, 0.0F, 6.0F, FACE_DOWN)
                        .texOffs(-4, 8).addBox(-8.0F, 0.0F, -4.0F, 8.0F, 0.0F, 6.0F, FACE_UP),
                PartPose.offset(-0.3F, 21.4F, -1.0F));
        root.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(4, 14).addBox(-8.0F, 0.0F, -1.0F, 8.0F, 0.0F, 6.0F, FACE_DOWN)
                        .texOffs(-4, 14).addBox(-8.0F, 0.0F, -1.0F, 8.0F, 0.0F, 6.0F, FACE_UP),
                PartPose.offset(-0.3F, 21.5F, -0.5F));
        root.addOrReplaceChild("wing_right_back",
                CubeListBuilder.create().texOffs(14, 0).addBox(-5.0F, 0.0F, -1.0F, 5.0F, 0.0F, 8.0F, FACE_DOWN)
                        .texOffs(9, 0).addBox(-5.0F, 0.0F, -1.0F, 5.0F, 0.0F, 8.0F, FACE_UP),
                PartPose.offsetAndRotation(0.3F, 21.2F, -1.0F, 0.0F, 0.0F, -0.5934119F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);

        float f = state.walkAnimationPos;
        float f1 = state.walkAnimationSpeed;
        float f2 = state.ageInTicks;

        float baseAngle = 0.52359F;
        float wingRot;
        float legMov;
        float legMovB;

        if (state.flying) {
            // Flying: full, fast wing flap and the legs held out in the flight pose (legacy flying branch).
            wingRot = Mth.cos(f2 * 0.9F) * 0.9F;
            legMov = f1 * 1.5F;
            legMovB = legMov;
        } else {
            // Grounded: the legs run the walking cosine gait and the wings rest folded up, giving only an
            // occasional slow flap (legacy: wings still except when f2 % 100 falls in 40..60).
            legMov = Mth.cos((f * 1.5F) + 3.141593F) * 1.4F * f1;
            legMovB = Mth.cos(f * 1.5F) * 1.4F * f1;
            int phase = ((int) f2) % 100;
            wingRot = (phase >= 40 && phase <= 60) ? Mth.cos(f2 * 0.4F) * 0.4F : 0.0F;
        }

        this.wingLeft.zRot = -baseAngle + wingRot;
        this.wingRight.zRot = baseAngle - wingRot;
        this.wingLeftFront.zRot = -baseAngle + wingRot;
        this.wingLeftBack.zRot = 0.5934119F + -baseAngle + wingRot;
        this.wingRightFront.zRot = baseAngle - wingRot;
        this.wingRightBack.zRot = -0.5934119F + baseAngle - wingRot;

        this.frontLegs.xRot = 0.1487144F + legMov;
        this.midLegs.xRot = 0.5948578F + legMovB;
        this.rearLegs.xRot = 1.070744F + legMov;
    }
}
