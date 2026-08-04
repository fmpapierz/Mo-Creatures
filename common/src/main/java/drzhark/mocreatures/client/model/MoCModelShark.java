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
 * Shark model, converted faithfully from the legacy {@code MoCModelShark} ({@code ModelBase}).
 */
public class MoCModelShark extends EntityModel<MoCEntityRenderState> {

    private final ModelPart body;
    private final ModelPart uHead;
    private final ModelPart dHead;
    private final ModelPart rHead;
    private final ModelPart lHead;
    private final ModelPart pTail;
    private final ModelPart upperFin;
    private final ModelPart upperTailFin;
    private final ModelPart lowerTailFin;
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public MoCModelShark(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.uHead = root.getChild("u_head");
        this.dHead = root.getChild("d_head");
        this.rHead = root.getChild("r_head");
        this.lHead = root.getChild("l_head");
        this.pTail = root.getChild("p_tail");
        this.upperFin = root.getChild("upper_fin");
        this.upperTailFin = root.getChild("upper_tail_fin");
        this.lowerTailFin = root.getChild("lower_tail_fin");
        this.leftFin = root.getChild("left_fin");
        this.rightFin = root.getChild("right_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(6, 6).addBox(0.0F, 0.0F, 0.0F, 6.0F, 8.0F, 18.0F),
                PartPose.offset(-4.0F, 17.0F, -10.0F));
        root.addOrReplaceChild("u_head",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 5.0F, 2.0F, 8.0F),
                PartPose.offsetAndRotation(-3.5F, 21.0F, -16.5F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("d_head",
                CubeListBuilder.create().texOffs(44, 0).addBox(0.0F, 0.0F, 0.0F, 5.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-3.5F, 21.5F, -13.5F, -0.261799F, 0.0F, 0.0F));
        root.addOrReplaceChild("r_head",
                CubeListBuilder.create().texOffs(0, 3).addBox(0.0F, 0.0F, 0.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(-3.45F, 21.3F, -13.85F, 0.7853981F, 0.0F, 0.0F));
        root.addOrReplaceChild("l_head",
                CubeListBuilder.create().texOffs(0, 3).addBox(0.0F, 0.0F, 0.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.45F, 21.3F, -13.8F, 0.7853981F, 0.0F, 0.0F));
        root.addOrReplaceChild("p_tail",
                CubeListBuilder.create().texOffs(36, 8).addBox(0.0F, 0.0F, 0.0F, 4.0F, 6.0F, 10.0F),
                PartPose.offset(-3.0F, 18.0F, 8.0F));
        root.addOrReplaceChild("upper_fin",
                CubeListBuilder.create().texOffs(6, 12).addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 8.0F),
                PartPose.offsetAndRotation(-1.5F, 17.0F, -1.0F, 0.7853981F, 0.0F, 0.0F));
        root.addOrReplaceChild("upper_tail_fin",
                CubeListBuilder.create().texOffs(6, 12).addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 8.0F),
                PartPose.offsetAndRotation(-1.5F, 18.0F, 16.0F, 0.5235988F, 0.0F, 0.0F));
        root.addOrReplaceChild("lower_tail_fin",
                CubeListBuilder.create().texOffs(8, 14).addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 6.0F),
                PartPose.offsetAndRotation(-1.5F, 21.0F, 18.0F, -0.7853981F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(18, 0).addBox(0.0F, 0.0F, 0.0F, 8.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(2.0F, 24.0F, -5.0F, 0.0F, -0.5235988F, 0.5235988F));
        root.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(18, 0).addBox(0.0F, 0.0F, 0.0F, 8.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(-10.0F, 27.5F, -1.0F, 0.0F, 0.5235988F, -0.5235988F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        this.upperTailFin.yRot = Mth.cos(limbSwing * 0.6662F) * limbAmount;
        this.lowerTailFin.yRot = Mth.cos(limbSwing * 0.6662F) * limbAmount;
    }
}
