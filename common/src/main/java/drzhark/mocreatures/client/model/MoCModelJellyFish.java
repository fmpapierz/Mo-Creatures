package drzhark.mocreatures.client.model;

import drzhark.mocreatures.client.state.MoCEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * JellyFish model, converted faithfully from the legacy {@code MoCModelJellyFish} ({@code ModelBase}).
 * Geometry and texture offsets are preserved; the tentacles do a simple limb swing.
 */
public class MoCModelJellyFish extends EntityModel<MoCEntityRenderState> {

    private final ModelPart legSmall1;
    private final ModelPart legC1;
    private final ModelPart legC2;
    private final ModelPart legC3;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart leg5;
    private final ModelPart leg6;
    private final ModelPart leg7;
    private final ModelPart leg8;
    private final ModelPart leg9;

    public MoCModelJellyFish(ModelPart root) {
        // Base render type is plain translucent; the renderer (MoCJellyFishRenderer#getRenderType) swaps in
        // the emissive variant only at night when the jellyfish glows, so a daytime jelly is not self-lit.
        super(root, tex -> net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(tex));
        this.legSmall1 = root.getChild("leg_small1");
        this.legC1 = root.getChild("leg_c1");
        this.legC2 = root.getChild("leg_c2");
        this.legC3 = root.getChild("leg_c3");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        this.leg5 = root.getChild("leg5");
        this.leg6 = root.getChild("leg6");
        this.leg7 = root.getChild("leg7");
        this.leg8 = root.getChild("leg8");
        this.leg9 = root.getChild("leg9");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("top",
                CubeListBuilder.create().texOffs(0, 10).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("head_small",
                CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 12.5F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(36, 0).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, 13.8F, 0.0F));
        root.addOrReplaceChild("body_center",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 15.5F, 0.0F));
        root.addOrReplaceChild("body_bottom",
                CubeListBuilder.create().texOffs(20, 10).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 18.3F, 0.0F));
        root.addOrReplaceChild("side1",
                CubeListBuilder.create().texOffs(20, 10).addBox(-2.0F, 5.0F, 0.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 12.5F, 0.0F, -0.7679449F, 0.0F, 0.0F));
        root.addOrReplaceChild("side2",
                CubeListBuilder.create().texOffs(20, 10).addBox(-4.0F, 5.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 12.5F, 0.0F, 0.0F, 0.0F, -0.7679449F));
        root.addOrReplaceChild("side3",
                CubeListBuilder.create().texOffs(20, 10).addBox(0.0F, 5.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 12.5F, 0.0F, 0.0F, 0.0F, 0.7679449F));
        root.addOrReplaceChild("side4",
                CubeListBuilder.create().texOffs(20, 10).addBox(-2.0F, 5.0F, -4.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 12.5F, 0.0F, 0.7679449F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_small1",
                CubeListBuilder.create().texOffs(60, 2).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 18.5F, 0.0F));
        root.addOrReplaceChild("leg_c1",
                CubeListBuilder.create().texOffs(15, 10).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-0.5F, 15.5F, -0.5F, -0.2602503F, 0.0F, 0.1487144F));
        root.addOrReplaceChild("leg_c2",
                CubeListBuilder.create().texOffs(15, 10).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.5F, 15.5F, -0.5F, 0.1487144F, 1.747395F, 0.0F));
        root.addOrReplaceChild("leg_c3",
                CubeListBuilder.create().texOffs(15, 10).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-0.5F, 15.5F, 0.5F, 0.1115358F, 0.3717861F, 0.2230717F));
        root.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 20.0F, 2.5F));
        root.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 20.0F, -2.5F));
        root.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(2.5F, 20.0F, 0.0F));
        root.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(-2.5F, 20.0F, 0.0F));
        root.addOrReplaceChild("leg5",
                CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 20.0F, 2.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("leg6",
                CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 20.0F, -2.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("leg7",
                CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 20.0F, -2.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("leg8",
                CubeListBuilder.create().texOffs(60, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 18.5F, 0.0F));
        root.addOrReplaceChild("leg9",
                CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 20.0F, 2.0F, 0.0F, 0.7853982F, 0.0F));

        return LayerDefinition.create(mesh, 64, 16);
    }

    @Override
    public void setupAnim(MoCEntityRenderState state) {
        super.setupAnim(state);
        float f6 = state.walkAnimationSpeed * 2.0F;
        if (f6 > 1.0F) {
            f6 = 1.0F;
        }

        this.legSmall1.xRot = f6;
        this.legC1.xRot = f6;
        this.legC2.xRot = f6;
        this.legC3.xRot = f6;
        this.leg1.xRot = f6;
        this.leg2.xRot = f6;
        this.leg3.xRot = f6;
        this.leg4.xRot = f6;
        this.leg5.xRot = f6;
        this.leg6.xRot = f6;
        this.leg7.xRot = f6;
        this.leg8.xRot = f6;
        this.leg9.xRot = f6;
    }
}
