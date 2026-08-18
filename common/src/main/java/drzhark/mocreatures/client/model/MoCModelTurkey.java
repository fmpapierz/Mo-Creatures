package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.util.Set;

/**
 * Turkey model, converted faithfully from the legacy {@code MoCModelTurkey} ({@code ModelBase}).
 * Geometry, texture offsets and the walking gait are preserved; only the scaffolding is modern.
 */
public class MoCModelTurkey extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    // The tail fan and foot planes are split into two single-face boxes (painted face + a re-aimed opposite
    // face sampling the same painted tile) so the culled render type keeps them visible from both sides.
    private static final Set<Direction> NORTH_ONLY = Set.of(Direction.NORTH);
    private static final Set<Direction> SOUTH_ONLY = Set.of(Direction.SOUTH);
    private static final Set<Direction> DOWN_ONLY = Set.of(Direction.DOWN);
    private static final Set<Direction> UP_ONLY = Set.of(Direction.UP);

    private final ModelPart beak;
    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart chest;
    private final ModelPart rWing;
    private final ModelPart lWing;
    private final ModelPart uBody;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart rLeg;
    private final ModelPart rFoot;
    private final ModelPart lLeg;
    private final ModelPart lFoot;

    public MoCModelTurkey(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.beak = root.getChild("beak");
        this.head = root.getChild("head");
        this.neck = root.getChild("neck");
        this.chest = root.getChild("chest");
        this.rWing = root.getChild("r_wing");
        this.lWing = root.getChild("l_wing");
        this.uBody = root.getChild("u_body");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
        this.rLeg = root.getChild("r_leg");
        this.rFoot = root.getChild("r_foot");
        this.lLeg = root.getChild("l_leg");
        this.lFoot = root.getChild("l_foot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(17, 17).addBox(-0.5F, -1.866667F, -3.366667F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 9.7F, -5.1F, 0.7807508F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 27).addBox(-1.0F, -2.0F, -2.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 9.7F, -5.1F, 0.4833219F, 0.0F, 0.0F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 14.7F, -6.5F, -0.2246208F, 0.0F, 0.0F));
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 17).addBox(-3.0F, 0.0F, -4.0F, 6.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 12.5F, -4.0F, 0.5934119F, 0.0F, 0.0F));
        root.addOrReplaceChild("r_wing",
                CubeListBuilder.create().texOffs(32, 30).addBox(-1.0F, -2.0F, 0.0F, 1.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(-4.0F, 14.0F, -3.0F, -0.3346075F, 0.0F, 0.0F));
        root.addOrReplaceChild("l_wing",
                CubeListBuilder.create().texOffs(48, 30).addBox(0.0F, -2.0F, 0.0F, 1.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(4.0F, 14.0F, -3.0F, -0.3346075F, 0.0F, 0.0F));
        root.addOrReplaceChild("u_body",
                CubeListBuilder.create().texOffs(34, 0).addBox(-2.5F, -4.0F, 0.0F, 5.0F, 7.0F, 9.0F),
                PartPose.offset(0.0F, 15.0F, -3.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 9.0F),
                PartPose.offset(0.0F, 16.0F, -4.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(32, 17).addBox(-8.0F, -9.0F, 0.0F, 16.0F, 12.0F, 0.0F, NORTH_ONLY)
                        .texOffs(16, 17).addBox(-8.0F, -9.0F, 0.0F, 16.0F, 12.0F, 0.0F, SOUTH_ONLY),
                PartPose.offsetAndRotation(0.0F, 14.0F, 6.0F, -0.2974289F, 0.0F, 0.0F));
        root.addOrReplaceChild("r_leg",
                CubeListBuilder.create().texOffs(27, 17).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(-2.0F, 19.0F, 0.5F));
        root.addOrReplaceChild("r_foot",
                CubeListBuilder.create().texOffs(20, 23).addBox(-1.5F, 5.0F, -2.5F, 3.0F, 0.0F, 3.0F, DOWN_ONLY)
                        .texOffs(17, 23).addBox(-1.5F, 5.0F, -2.5F, 3.0F, 0.0F, 3.0F, UP_ONLY),
                PartPose.offset(-2.0F, 19.0F, 0.5F));
        root.addOrReplaceChild("l_leg",
                CubeListBuilder.create().texOffs(23, 17).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(2.0F, 19.0F, 0.5F));
        root.addOrReplaceChild("l_foot",
                CubeListBuilder.create().texOffs(20, 26).addBox(-1.5F, 5.0F, -2.5F, 3.0F, 0.0F, 3.0F, DOWN_ONLY)
                        .texOffs(17, 26).addBox(-1.5F, 5.0F, -2.5F, 3.0F, 0.0F, 3.0F, UP_ONLY),
                PartPose.offset(2.0F, 19.0F, 0.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float f = state.walkAnimationPos;
        float f1 = state.walkAnimationSpeed;
        float headYaw = state.yRot * DEG_TO_RAD;
        float headPitch = state.xRot * DEG_TO_RAD;

        float lLegXRot = Mth.cos(f * 0.6662F) * 1.4F * f1;
        float rLegXRot = Mth.cos((f * 0.6662F) + 3.141593F) * 1.4F * f1;
        float wingF = (Mth.cos(f * 0.6662F) * 1.4F * f1) / 4.0F;

        this.head.xRot = 0.4833219F + headPitch;
        this.head.yRot = headYaw;
        this.beak.xRot = 0.2974F + this.head.xRot;
        this.beak.yRot = this.head.yRot;

        this.lLeg.xRot = lLegXRot;
        this.lFoot.xRot = this.lLeg.xRot;
        this.rLeg.xRot = rLegXRot;
        this.rFoot.xRot = this.rLeg.xRot;

        this.lWing.yRot = wingF;
        this.rWing.yRot = -wingF;

        this.tail.xRot = -0.2974289F + wingF;
    }
}
