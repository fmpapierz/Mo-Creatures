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
 * Fox model, converted faithfully from the legacy {@code MoCModelFox} ({@code ModelBase}).
 * Geometry, texture offsets and the walking leg gait are preserved.
 */
public class MoCModelFox extends EntityModel<MoCEntityRenderState> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart ears;
    private final ModelPart tail;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;

    public MoCModelFox(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.snout = root.getChild("snout");
        this.ears = root.getChild("ears");
        this.tail = root.getChild("tail");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 6.0F, 6.0F, 12.0F),
                PartPose.offset(-4.0F, 10.0F, -6.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 4.0F),
                PartPose.offset(-1.0F, 11.0F, -6.0F));
        root.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(20, 20).addBox(-1.0F, 1.0F, -7.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(-1.0F, 11.0F, -6.0F));
        root.addOrReplaceChild("ears",
                CubeListBuilder.create().texOffs(50, 20).addBox(-3.0F, -6.0F, -2.0F, 6.0F, 4.0F, 1.0F),
                PartPose.offset(-1.0F, 11.0F, -6.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(32, 20).addBox(-5.0F, -5.0F, -2.0F, 3.0F, 3.0F, 8.0F),
                PartPose.offsetAndRotation(2.5F, 15.0F, 5.0F, -0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 3.0F, 8.0F, 3.0F),
                PartPose.offset(-2.0F, 16.0F, 5.0F));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 3.0F, 8.0F, 3.0F),
                PartPose.offset(1.0F, 16.0F, 5.0F));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 3.0F, 8.0F, 3.0F),
                PartPose.offset(-2.0F, 16.0F, -4.0F));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 3.0F, 8.0F, 3.0F),
                PartPose.offset(1.0F, 16.0F, -4.0F));

        // Back faces 1.12 drew with culling off (see MoCBackFaces): the head box has a gap in its underside, through
        // which its inner walls showed. Each twin is the named face of that part's box turned to face into the box,
        // with the same corners and texels.
        MoCBackFaces.add(root, "head", CubeListBuilder.create()
                .texOffs(-6, 20).mirror(false).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 0.0F, 4.0F, MoCBackFaces.UP) // back of the down face
                .texOffs(0, 20).mirror(true).addBox(-3.0F, -3.0F, -4.0F, 0.0F, 6.0F, 4.0F, MoCBackFaces.WEST) // back of the west face
                .texOffs(-2, 24).mirror(true).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 0.0F, MoCBackFaces.SOUTH) // back of the north face
                .texOffs(6, 20).mirror(true).addBox(3.0F, -3.0F, -4.0F, 0.0F, 6.0F, 4.0F, MoCBackFaces.EAST) // back of the east face
                // back of the south face, top band only: below it the body's front face lies flush on it (z-fight)
                .texOffs(14, 24).mirror(true).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 2.0F, 0.0F, MoCBackFaces.NORTH));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float headYaw = state.yRot * DEG_TO_RAD;
        float headPitch = state.xRot * DEG_TO_RAD;

        this.head.yRot = headYaw;
        this.head.xRot = headPitch;
        this.snout.yRot = headYaw;
        this.snout.xRot = headPitch;
        this.ears.yRot = headYaw;
        this.ears.xRot = headPitch;

        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        this.leg1.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
        this.leg2.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;
        this.leg3.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;
        this.leg4.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
    }
}
