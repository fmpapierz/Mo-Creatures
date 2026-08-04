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
 * Deer model, converted faithfully from the legacy {@code MoCModelDeer} ({@code ModelBase}).
 * Geometry, texture offsets and the four-leg walk gait are preserved; only the scaffolding is modern.
 */
public class MoCModelDeer extends EntityModel<MoCEntityRenderState> {

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart tail;
    private final ModelPart earLeft;
    private final ModelPart earRight;
    private final ModelPart leftAntler;
    private final ModelPart rightAntler;

    public MoCModelDeer(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = root.getChild("neck");
        this.head = root.getChild("head");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        this.tail = root.getChild("tail");
        this.earLeft = root.getChild("ear_left");
        this.earRight = root.getChild("ear_right");
        this.leftAntler = root.getChild("left_antler");
        this.rightAntler = root.getChild("right_antler");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -6.0F, -9.5F, 3.0F, 3.0F, 6.0F),
                PartPose.offset(1.0F, 11.5F, -4.5F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 9).addBox(-2.0F, -2.0F, -6.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offsetAndRotation(1.0F, 11.5F, -4.5F, -0.7853981F, 0.0F, 0.0F));
        root.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.5F, -5.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, 11.5F, -4.5F, 0.0F, 0.0F, 0.7853981F));
        root.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(2.0F, -7.5F, -5.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, 11.5F, -4.5F, 0.0F, 0.0F, -0.7853981F));
        root.addOrReplaceChild("left_antler",
                CubeListBuilder.create().texOffs(54, 0).addBox(0.0F, -14.0F, -7.0F, 1.0F, 8.0F, 4.0F),
                PartPose.offsetAndRotation(1.0F, 11.5F, -4.5F, 0.0F, 0.0F, 0.2094395F));
        root.addOrReplaceChild("right_antler",
                CubeListBuilder.create().texOffs(54, 0).addBox(0.0F, -14.0F, -7.0F, 1.0F, 8.0F, 4.0F),
                PartPose.offsetAndRotation(1.0F, 11.5F, -4.5F, 0.0F, 0.0F, -0.2094395F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(24, 12).addBox(-2.0F, -3.0F, -6.0F, 6.0F, 6.0F, 14.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(9, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(3.0F, 16.0F, -4.0F));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(0, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(-1.0F, 16.0F, -4.0F));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(9, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(3.0F, 16.0F, 6.0F));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(0, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(-1.0F, 16.0F, 6.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(50, 20).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(1.0F, 11.0F, 7.0F, 0.7854F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;

        this.leg1.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
        this.leg2.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;
        this.leg3.xRot = Mth.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbAmount;
        this.leg4.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
    }
}
