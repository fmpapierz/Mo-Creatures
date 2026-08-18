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
 * Bee model, converted faithfully from the legacy {@code MoCModelBee} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; the wing buzz and leg gait are kept.
 */
public class MoCModelBee extends EntityModel<MoCEntityRenderState> {

    // The wing sheets paint only their DOWN tile; the model renders culled, so each zero-thickness
    // wing box is split into two single-face boxes that both sample the painted tile (the second
    // texOffs re-aims the opposite face onto the same rect — see MoCModelHorse's membranes).
    private static final Set<Direction> FACE_DOWN = Set.of(Direction.DOWN);
    private static final Set<Direction> FACE_UP = Set.of(Direction.UP);

    private final ModelPart head;
    private final ModelPart rAntenna;
    private final ModelPart lAntenna;
    private final ModelPart mouth;
    private final ModelPart thorax;
    private final ModelPart abdomen;
    private final ModelPart tail;
    private final ModelPart frontLegs;
    private final ModelPart rearLegs;
    private final ModelPart midLegs;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart foldedWings;

    public MoCModelBee(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.head = root.getChild("head");
        this.rAntenna = root.getChild("r_antenna");
        this.lAntenna = root.getChild("l_antenna");
        this.mouth = root.getChild("mouth");
        this.thorax = root.getChild("thorax");
        this.abdomen = root.getChild("abdomen");
        this.tail = root.getChild("tail");
        this.frontLegs = root.getChild("front_legs");
        this.rearLegs = root.getChild("rear_legs");
        this.midLegs = root.getChild("mid_legs");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.foldedWings = root.getChild("folded_wings");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 21.5F, -2.0F, -2.171231F, 0.0F, 0.0F));
        root.addOrReplaceChild("r_antenna",
                CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F),
                PartPose.offsetAndRotation(-0.5F, 20.2F, -2.3F, -1.041001F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("l_antenna",
                CubeListBuilder.create().texOffs(0, 12).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F),
                PartPose.offsetAndRotation(0.5F, 20.2F, -2.3F, -1.041001F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(0, 13).addBox(0.0F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 21.5F, -2.0F, -0.4461433F, 0.3569147F, 0.7853982F));
        root.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 5).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 20.5F, -1.0F));
        root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 21.5F, 0.0F, 1.249201F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 15).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 2.0F, 0.2379431F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(4, 14).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, -1.8F, 0.1487144F, 0.0F, 0.0F));
        root.addOrReplaceChild("rear_legs",
                CubeListBuilder.create().texOffs(8, 1).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 3.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.5F, -0.4F, 0.8922867F, 0.0F, 0.0F));
        root.addOrReplaceChild("mid_legs",
                CubeListBuilder.create().texOffs(4, 12).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 22.5F, -1.2F, 0.5948578F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, 0.5F, 2.0F, 0.0F, 4.0F, FACE_DOWN)
                        .texOffs(-2, 17).addBox(-1.0F, 0.0F, 0.5F, 2.0F, 0.0F, 4.0F, FACE_UP),
                PartPose.offsetAndRotation(0.0F, 20.4F, -1.0F, 0.0F, 1.047198F, 0.0F));
        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, 0.5F, 2.0F, 0.0F, 4.0F, FACE_DOWN)
                        .texOffs(-2, 17).addBox(-1.0F, 0.0F, 0.5F, 2.0F, 0.0F, 4.0F, FACE_UP),
                PartPose.offsetAndRotation(0.0F, 20.4F, -1.0F, 0.0F, -1.047198F, 0.0F));
        root.addOrReplaceChild("folded_wings",
                CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 4.0F, FACE_DOWN)
                        .texOffs(-2, 17).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 4.0F, FACE_UP),
                PartPose.offsetAndRotation(0.0F, 20.5F, -1.0F, 0.0001745F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        float ageInTicks = state.ageInTicks;

        // wing buzz (always, legacy)
        float wingRot = Mth.cos(ageInTicks * 3.0F) * 0.7F;
        this.rightWing.zRot = wingRot;
        this.leftWing.zRot = -wingRot;

        float legMov;
        float legMovB;
        if (state.flying) {
            // Airborne: the legs hang in a fixed dangling pose scaled by flight speed (legacy legMov=f1*1.5).
            legMov = limbAmount * 1.5F;
            legMovB = legMov;
        } else {
            // walking leg gait (legacy ground branch)
            legMov = Mth.cos((limbSwing * 1.5F) + 3.141593F) * 2.0F * limbAmount;
            legMovB = Mth.cos(limbSwing * 1.5F) * 2.0F * limbAmount;
        }

        this.frontLegs.xRot = 0.1487144F + legMov;
        this.midLegs.xRot = 0.5948578F + legMovB;
        this.rearLegs.xRot = 1.070744F + legMov;
    }
}
