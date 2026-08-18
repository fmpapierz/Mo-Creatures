package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Dolphin model, converted faithfully from the legacy {@code MoCModelDolphin} ({@code ModelBase}).
 * Geometry, texture offsets and the swimming tail-fin gait are preserved; only the scaffolding is modern.
 */
public class MoCModelDolphin extends EntityModel<MoCEntityRenderState> {

    private final ModelPart body;
    private final ModelPart uHead;
    private final ModelPart dHead;
    private final ModelPart pTail;
    private final ModelPart upperFin;
    private final ModelPart lTailFin;
    private final ModelPart rTailFin;
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public MoCModelDolphin(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.body = root.getChild("body");
        this.uHead = root.getChild("uhead");
        this.dHead = root.getChild("dhead");
        this.pTail = root.getChild("ptail");
        this.upperFin = root.getChild("upper_fin");
        this.lTailFin = root.getChild("ltail_fin");
        this.rTailFin = root.getChild("rtail_fin");
        this.leftFin = root.getChild("left_fin");
        this.rightFin = root.getChild("right_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(4, 6).addBox(0.0F, 0.0F, 0.0F, 6, 8, 18),
                PartPose.offset(-4.0F, 17.0F, -10.0F));
        root.addOrReplaceChild("uhead",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 5, 7, 8),
                PartPose.offset(-3.5F, 18.0F, -16.5F));
        root.addOrReplaceChild("dhead",
                CubeListBuilder.create().texOffs(50, 0).addBox(0.0F, 0.0F, 0.0F, 3, 3, 4),
                PartPose.offset(-2.5F, 21.5F, -20.5F));
        root.addOrReplaceChild("ptail",
                CubeListBuilder.create().texOffs(34, 9).addBox(0.0F, 0.0F, 0.0F, 5, 5, 10),
                PartPose.offset(-3.5F, 19.0F, 8.0F));
        root.addOrReplaceChild("upper_fin",
                CubeListBuilder.create().texOffs(4, 12).addBox(0.0F, 0.0F, 0.0F, 1, 4, 8),
                PartPose.offsetAndRotation(-1.5F, 18.0F, -4.0F, 0.7853981F, 0.0F, 0.0F));
        root.addOrReplaceChild("ltail_fin",
                CubeListBuilder.create().texOffs(34, 0).addBox(0.0F, 0.0F, 0.0F, 4, 1, 8, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(-2.0F, 21.5F, 18.0F, 0.0F, 0.7853981F, 0.0F));
        root.addOrReplaceChild("rtail_fin",
                CubeListBuilder.create().texOffs(34, 0).addBox(0.0F, 0.0F, 0.0F, 4, 1, 8, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(-3.0F, 21.5F, 15.0F, 0.0F, -0.7853981F, 0.0F));
        root.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(14, 0).addBox(0.0F, 0.0F, 0.0F, 8, 1, 4),
                PartPose.offsetAndRotation(2.0F, 24.0F, -7.0F, 0.0F, -0.5235988F, 0.5235988F));
        root.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(14, 0).addBox(0.0F, 0.0F, 0.0F, 8, 1, 4),
                PartPose.offsetAndRotation(-10.0F, 27.5F, -3.0F, 0.0F, 0.5235988F, -0.5235988F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float limbSwing = state.walkAnimationPos;
        float limbAmount = state.walkAnimationSpeed;
        this.rTailFin.xRot = Mth.cos(limbSwing * 0.6662F) * limbAmount;
        this.lTailFin.xRot = Mth.cos(limbSwing * 0.6662F) * limbAmount;
    }
}
